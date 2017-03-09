package de.dfki.nlp.flow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import de.dfki.nlp.config.AnnotatorConfig;
import de.dfki.nlp.config.MessagingConfig.ProcessingGateway;
import de.dfki.nlp.domain.IdList;
import de.dfki.nlp.domain.PredictionResult;
import de.dfki.nlp.domain.exceptions.Errors;
import de.dfki.nlp.domain.rest.ServerRequest;
import de.dfki.nlp.domain.rest.ServerResponse;
import de.dfki.nlp.errors.FailedMessage;
import de.dfki.nlp.io.BufferingClientHttpResponseWrapper;
import de.dfki.nlp.loader.MultiDocumentFetcher;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.aop.Advice;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.dsl.Adapters;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.amqp.Amqp;
import org.springframework.integration.dsl.core.MessageHandlerSpec;
import org.springframework.integration.dsl.support.Function;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.amqp.rabbit.config.RetryInterceptorBuilder.stateless;

@Slf4j
@Component
@AllArgsConstructor
public class FlowHandler {

    private final MultiDocumentFetcher documentFetcher;

    private final AnnotatorConfig annotatorConfig;

    private final ObjectMapper objectMapper;


    @Bean
    IntegrationFlow errorSendingResults() {
        return f -> f
                .handle(MessagingException.class, (payload, headers) -> {

                    FailedMessage failedMessage = new FailedMessage();

                    Integer communicationId = (Integer) payload.getFailedMessage().getHeaders().getOrDefault("communication_id", -1);
                    failedMessage.setCommunicationId(communicationId);
                    log.error("Failure sending results [{}] {}", communicationId, payload.getMessage());

                    try {
                        failedMessage.setFailedMessagePayload(objectMapper.writeValueAsString(payload.getFailedMessage().getPayload()));
                    } catch (JsonProcessingException e) {
                        log.error("Could not serialize the error message {}", e.getMessage());
                    }

                    failedMessage.setServerErrorCause(payload.getMessage());

                    // try to replicate most of the message and the error
                    if (payload.getCause() instanceof HttpClientErrorException) {
                        HttpClientErrorException cause = (HttpClientErrorException) payload.getCause();

                        String serverPayload = cause.getResponseBodyAsString();
                        failedMessage.setServerErrorPayload(serverPayload);

                    }

                    try {
                        log.error("The complete failed message for retrying\n{}", objectMapper.writeValueAsString(failedMessage));
                    } catch (JsonProcessingException e) {
                        log.error("Double error ...  {}", e.getMessage());
                        log.error("The complete failed message for retrying\n{}", failedMessage);
                    }
                    return null;
                });
    }

    @Bean
    public RetryOperationsInterceptor retryOperationsInterceptor() {
        return stateless()
                .maxAttempts(2)
                .recoverer(new RejectAndDontRequeueRecoverer()).build();
    }


    @Bean
    IntegrationFlow flow(ConnectionFactory connectionFactory, Queue input, Jackson2JsonMessageConverter messageConverter, Environment environment) {
        IntegrationFlowBuilder flow =
                IntegrationFlows
                        .from(
                                Amqp.inboundAdapter(connectionFactory, input)
                                        // set concurrentConsumers - anything larger than 1 gives parallelism per annotator request
                                        // but not the number of requests
                                        .concurrentConsumers(annotatorConfig.getConcurrentConsumer())
                                        .messageConverter(messageConverter)
                                        .headerMapper(DefaultAmqpHeaderMapper.inboundMapper())
                                        // retry the complete message
                                        // if this fails ... forward to the error queue
                                        .defaultRequeueRejected(false)
                                        .adviceChain(retryOperationsInterceptor())
                                        .errorChannel("errorSendingResults.input")
                        )
                        .enrichHeaders(headerEnricherSpec -> headerEnricherSpec.headerExpression("communication_id", "payload.parameters.communication_id"))
                        .enrichHeaders(headerEnricherSpec -> headerEnricherSpec.headerExpression("types", "payload.parameters.types"))
                        .split(ServerRequest.class, serverRequest -> {
                                    // partition the input
                                    ImmutableListMultimap<String, ServerRequest.Document> index = Multimaps.index(serverRequest.getParameters()
                                            .getDocuments(), ServerRequest.Document::getSource);

                                    List<IdList> idLists = new ArrayList<>();

                                    // now split into X at most per source
                                    for (Map.Entry<String, Collection<ServerRequest.Document>> entry : index.asMap().entrySet()) {
                                        for (List<ServerRequest.Document> documentList : Iterables.partition(entry.getValue(), annotatorConfig.getRequestBulkSize())) {
                                            idLists.add(new IdList(entry.getKey(), documentList.stream().map(ServerRequest.Document::getDocument_id).collect(Collectors.toList())));
                                        }
                                    }

                                    return idLists;
                                }
                        )
                        // handle in parallel using an executor on a different channel
                        //   .channel(c -> c.executor("Downloader", Executors.newFixedThreadPool(annotatorConfig.getConcurrentHandler())))
                        .transform(IdList.class, documentFetcher::load)
                        .split()
                        .channel("annotate")
                        .routeToRecipients(r ->
                                r.applySequence(true)
                                        .defaultOutputToParentFlow()
                                        .recipient("mirner", "headers['types'].contains(T(de.dfki.nlp.domain.PredictionType).MIRNA)")
                                        .recipient("seth", "headers['types'].contains(T(de.dfki.nlp.domain.PredictionType).MUTATION)")
                                        .recipient("diseases", "headers['types'].contains(T(de.dfki.nlp.domain.PredictionType).DISEASE)")
                        )
                        .channel("parsed")
                        .aggregate() // this aggregates annotations per document (from router)
                        .<List<Set<PredictionResult>>, Set<PredictionResult>>transform(s -> s.stream().flatMap(Collection::stream).collect(Collectors.toSet()))
                        .channel("aggregate")
                        .aggregate() // this aggregates all document per source group
                        .aggregate() // this aggregates all documents
                        // now merge the results by flattening
                        .channel("jointogether")
                        .<List<List<Set<PredictionResult>>>, Set<PredictionResult>>transform(source ->
                                source.stream().flatMap(Collection::stream).flatMap(Collection::stream).collect(Collectors.toSet()));


        if (environment.acceptsProfiles("cloud")) {
            // when cloud profile is active, send results via http
            flow
                    .enrichHeaders(headerEnricherSpec -> headerEnricherSpec.headerExpression("Content-Type", "'application/json'"))
                    .log(LoggingHandler.Level.INFO, objectMessage ->
                            String.format("Sending Results [%s] after %d ms %s", objectMessage.getHeaders().get("communication_id"), (System.currentTimeMillis() - (long) objectMessage.getHeaders().get(ProcessingGateway.HEADER_REQUEST_TIME)), objectMessage.getHeaders().toString()))
                    //"respone", "headers['communication_id']")
                    // use retry advice - which tries to resend in case of failure
                    .handleWithAdapter(sendToBecalmServer(), e -> e.advice(retryAdvice()));

        } else {
            // for local deployment, just log
            flow
                    .<Set<PredictionResult>>handle((parsed, headers) -> {
                        log.info(headers.toString());

                        log.info("Annotation request took [{}] {} ms", headers.get("communication_id"), System.currentTimeMillis() - (long) headers.get(ProcessingGateway.HEADER_REQUEST_TIME));

                        parsed
                                .stream()
                                .sorted((o1, o2) -> ComparisonChain
                                        .start()
                                        .compare(o1.getDocumentId(), o2.getDocumentId())
                                        .compare(o1.getSection().name(), o2.getSection().name())
                                        .compare(o1.getInit(), o2.getInit())
                                        .result())
                                .forEach(r -> log.info(r.toString()));

                        return null;
                    });
        }

        return flow.get();

    }


    /**
     * This bean is a retry advice to handle failures using retry
     * finally it fails.
     * It tries 20 (configurable) times using an exponential backoff strategy:
     * - initial wait 100ms - default multiplier 2 - maximal wait between calls 30s
     * <p>
     * Thus it tries in case of a failure ...
     * </p>
     * <pre>
     * Sleeping for 100
     * Sleeping for 200
     * ......
     * Sleeping for 30000
     * </pre>
     *
     * @return bean
     */
    @Bean
    public Advice retryAdvice() {
        RequestHandlerRetryAdvice advice = new RequestHandlerRetryAdvice();
        RetryTemplate retryTemplate = new RetryTemplate();
        // use exponential backoff to wait between calls
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);
        backOffPolicy.setMultiplier(2);
        backOffPolicy.setMaxInterval(60000);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // try at most 20 times
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(annotatorConfig.becalmSaveAnnotationRetries));
        advice.setRetryTemplate(retryTemplate);
        return advice;
    }


    /**
     * Handles the BECALM post requests.
     *
     * @return the configured HTTP adapter
     */
    private Function<Adapters, MessageHandlerSpec<?, HttpRequestExecutingMessageHandler>> sendToBecalmServer() {
        return adapters -> {
            RestTemplate restTemplate = new RestTemplate();
            // we need to use a custom interceptor to allow multiple reading of the respons body
            // once to check if we have an error, a second time to see the results (if there was no error)
            restTemplate.getInterceptors().add(new BufferingClientHttpResponseWrapper());
            restTemplate.setErrorHandler(errorHandler());

            return adapters
                    // where to send the results to
                    .http(annotatorConfig.becalmSaveAnnotationLocation, restTemplate)
                    // use the post method
                    .httpMethod(HttpMethod.POST)
                    // replace URI placeholders using variables
                    .uriVariable("apikey", "'" + annotatorConfig.apiKey + "'")
                    .uriVariable("communicationId", "headers['communication_id']")
                    .expectedResponseType(ServerResponse.class);
        };
    }


    @Bean
    ResponseErrorHandler errorHandler() {
        return new ResponseErrorHandler() {

            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                // todo check success
                return true;
            }

            @Override
            public void handleError(ClientHttpResponse response) throws IOException {

                // we have some sort of error
                // try to inspect the result
                HttpStatus statusCode = getHttpStatusCode(response);
                byte[] responseBody = getResponseBody(response);
                Charset charset = getCharset(response);

                try {


                    HttpHeaders headers = response.getHeaders();
                    MediaType contentType = headers.getContentType();

                    if (!contentType.includes(MediaType.APPLICATION_JSON)) {
                        String serverResponse = new String(responseBody, MoreObjects.firstNonNull(charset, Charsets.UTF_8));
                        log.error("Server did not respond with JSON, but contentType {} - {}", contentType, StringUtils.replaceAll(serverResponse, "[\\r\\n]+", " "));
                        throw new HttpClientErrorException(statusCode, response.getStatusText(), response.getHeaders(), serverResponse.getBytes(), charset);
                    }


                    ServerResponse serverResponse = objectMapper.readValue(responseBody, ServerResponse.class);

                    // now check the serverResponse
                    if (serverResponse.isSuccess()) {
                        log.info("Success sending results: {}", serverResponse);
                        return;
                    }

                    switch (Errors.lookup(serverResponse.getErrorCode())) {

                        case REQUEST_CLOSED: // no error - we have sent it once .. continue
                            log.info("Posting results - assuming we don't have an error as server responded with {}", serverResponse.toString());
                            break;
                        default:
                            log.error("Error posting results to server: {} ", serverResponse);
                            throw new HttpClientErrorException(statusCode, response.getStatusText(), response.getHeaders(), serverResponse.toString().getBytes(), charset);
                    }


                } catch (IOException e) {
                    log.error("Error parsing Server Response: {} {}", new String(responseBody), e.getMessage());
                    throw new HttpClientErrorException(statusCode, response.getStatusText(),
                            response.getHeaders(), responseBody, charset);
                }

            }

            private HttpStatus getHttpStatusCode(ClientHttpResponse response) throws IOException {
                HttpStatus statusCode;
                try {
                    statusCode = response.getStatusCode();
                } catch (IllegalArgumentException ex) {
                    throw new UnknownHttpStatusCodeException(response.getRawStatusCode(),
                            response.getStatusText(), response.getHeaders(), getResponseBody(response), getCharset(response));
                }
                return statusCode;
            }


            private byte[] getResponseBody(ClientHttpResponse response) {
                try {
                    InputStream responseBody = response.getBody();
                    if (responseBody != null) {
                        return FileCopyUtils.copyToByteArray(responseBody);
                    }
                } catch (IOException ex) {
                    // ignore
                }
                return new byte[0];
            }

            private Charset getCharset(ClientHttpResponse response) {
                HttpHeaders headers = response.getHeaders();
                MediaType contentType = headers.getContentType();
                return contentType != null ? contentType.getCharset() : null;
            }

        };
    }

}
