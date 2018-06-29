package de.dfki.nlp.flow;

import static de.dfki.nlp.domain.PredictionType.CHEMICAL;
import static de.dfki.nlp.domain.PredictionType.DISEASE;
import static de.dfki.nlp.domain.PredictionType.GENE;
import static de.dfki.nlp.domain.PredictionType.MIRNA;
import static de.dfki.nlp.domain.PredictionType.MUTATION;
import static de.dfki.nlp.domain.PredictionType.ORGANISM;
import static org.springframework.amqp.rabbit.config.RetryInterceptorBuilder.stateless;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.aopalliance.aop.Advice;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.http.dsl.Http;
import org.springframework.integration.http.dsl.HttpMessageHandlerSpec;
import org.springframework.messaging.Message;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;

import de.dfki.nlp.config.AnnotatorConfig;
import de.dfki.nlp.config.EnabledAnnotators;
import de.dfki.nlp.config.MessagingConfig;
import de.dfki.nlp.config.MessagingConfig.ProcessingGateway;
import de.dfki.nlp.domain.AnnotationResponse;
import de.dfki.nlp.domain.IdList;
import de.dfki.nlp.domain.PredictionResult;
import de.dfki.nlp.domain.PredictionType;
import de.dfki.nlp.domain.exceptions.Errors;
import de.dfki.nlp.domain.rest.ServerRequest;
import de.dfki.nlp.domain.rest.ServerResponse;
import de.dfki.nlp.errors.FailedMessage;
import de.dfki.nlp.io.BufferingClientHttpResponseWrapper;
import de.dfki.nlp.loader.MultiDocumentFetcher;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
@Profile("backend")
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

                    payload.getCause().printStackTrace();

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
    @Profile("driver")
    IntegrationFlow resultHandler(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter messageConverter) {
        FileWritingMessageHandler fileWritingMessageHandler = new FileWritingMessageHandler(new File("annotated"));

        String filename = new SimpleDateFormat("'annotation-results_'yyyy-MM-dd_hh-mm-ss'.json'").format(new Date());

        log.info("Writing annotation output to {}", new File("annotated", filename).toString());

        fileWritingMessageHandler.setFileExistsMode(FileExistsMode.APPEND);
        fileWritingMessageHandler.setAppendNewLine(true);
        fileWritingMessageHandler.setFileNameGenerator(message -> filename);
        fileWritingMessageHandler.setExpectReply(false);

        return IntegrationFlows
                .from(Amqp.inboundAdapter(connectionFactory, MessagingConfig.queueOutput)
                        .messageConverter(messageConverter))
                // we might want to print only viable results, but for performance analysis, the complete results
                // are nice to have, as they have timestamps
                //.filter(AnnotationResponse.class, source -> source.getPredictionResults().size() > 0)
                .transform(Transformers.toJson())
                .handle(fileWritingMessageHandler)
                .get();
    }

    @Bean
    @Profile("!driver")
    IntegrationFlow resultHandlerDummy(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter messageConverter) {

        return IntegrationFlows
                .from(Amqp.inboundAdapter(connectionFactory, MessagingConfig.queueOutput)
                        .messageConverter(messageConverter))
                // do nothing
                .handle(message -> {})
                .get();
    }

    @SuppressWarnings("unchecked")
    private boolean headerContains(Message<?> message, PredictionType predictionType) {
        List<PredictionType> types = (List<PredictionType>) message.getHeaders().get("types");
        return types.contains(predictionType);
    }

    @Bean
    IntegrationFlow flow(ConnectionFactory connectionFactory,
                         @Qualifier("inputQueue") Queue input,
                         Jackson2JsonMessageConverter messageConverter,
                         Environment environment,
                         AnnotatorConfig annotatorConfig,
                         MultiDocumentFetcher documentFetcher,
                         EnabledAnnotators enabledAnnotators) {
        IntegrationFlowBuilder flow =
                IntegrationFlows
                        .from(
                                Amqp.inboundGateway(connectionFactory, input)
                                        // set concurrentConsumers - anything larger than 1 gives parallelism per annotator request
                                        // but not of the number of requests
                                        .configureContainer(conf -> {
                                            conf
                                                    .concurrentConsumers(annotatorConfig.getConcurrentConsumer())
                                                    // if this fails ... forward to the error queue
                                                    .defaultRequeueRejected(false);
                                        })
                                        .messageConverter(messageConverter)
                                        .headerMapper(DefaultAmqpHeaderMapper.inboundMapper())
                                        .replyTimeout(Long.MAX_VALUE)
                                        .defaultReplyTo(MessagingConfig.queueOutput)
                                        // retry the complete message
                                        //.retryTemplate(retryOperationsInterceptor())
                                        //.adviceChain(retryOperationsInterceptor())
                                        .errorChannel("errorSendingResults.input")
                        )
                        .enrichHeaders(headerEnricherSpec -> headerEnricherSpec.headerExpression("timeTakenFromQueue", "T(System).currentTimeMillis()"))
                        .enrichHeaders(headerEnricherSpec -> headerEnricherSpec.headerExpression("communication_id", "payload.parameters.communication_id"))
                        .enrichHeaders(headerEnricherSpec -> headerEnricherSpec.headerExpression("types", "payload.parameters.types"))

                        // if we have the entire data we don't need to download, otherwise we have to
                        .split(ServerRequest.class, serverRequest -> {
                                    // partition the input
                                    ImmutableListMultimap<String, ServerRequest.Document> index = Multimaps.index(serverRequest.getParameters()
                                            .getDocuments(), ServerRequest.Document::getSource);

                                    List<IdList> idLists = new ArrayList<>();

                                    // now split into X at most per source
                                    for (Map.Entry<String, Collection<ServerRequest.Document>> entry : index.asMap().entrySet()) {
                                        for (List<ServerRequest.Document> documentList : Iterables.partition(entry.getValue(), annotatorConfig.getRequestBulkSize())) {
                                            idLists.add(new IdList(entry.getKey(), documentList));
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
                        .scatterGather(r -> r.applySequence(true)
                                .defaultOutputToParentFlow()
                                .recipientMessageSelector("mirNer", message -> headerContains(message, MIRNA) && enabledAnnotators.mirNer)
                                .recipientMessageSelector("seth", message -> headerContains(message, MUTATION) && enabledAnnotators.seth)
                                .recipientMessageSelector("diseaseNer", message -> headerContains(message, DISEASE) && enabledAnnotators.diseaseNer)
                                .recipientMessageSelector("dnorm", message -> headerContains(message, DISEASE) && enabledAnnotators.dnorm)
                                .recipientMessageSelector("banner", message -> headerContains(message, GENE) && enabledAnnotators.banner)
                                .recipientMessageSelector("linnaeus", message -> headerContains(message, ORGANISM) && enabledAnnotators.linnaeus)
                                .recipientMessageSelector("chemspot", message -> headerContains(message, CHEMICAL) && enabledAnnotators.chemspot))
                        .<List<Set<PredictionResult>>, Set<PredictionResult>>transform(s -> s.stream().flatMap(Collection::stream).collect(Collectors.toSet()))
                        .aggregate() // this aggregates all document per source group
                        .aggregate() // this aggregates all documents
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
                    //.handle(sendToBecalmServer()));
                    .handle(sendToBecalmServer(), e -> e.advice(retryAdvice()))
                    .<Set<PredictionResult>>handle((m, headers) -> {
                        long now = System.currentTimeMillis();
                        long runtime = now - (long) headers.get(ProcessingGateway.HEADER_REQUEST_TIME);
                        long parseTime = now - (long) headers.get("timeTakenFromQueue");
                        return new AnnotationResponse(m, runtime, parseTime);
                    });

        } else {
            // for local deployments, just log
            flow
                    .<Set<PredictionResult>>handle((parsed, headers) -> {
                        //log.info(headers.toString());

                        long now = System.currentTimeMillis();
                        long runtime = now - (long) headers.get(ProcessingGateway.HEADER_REQUEST_TIME);
                        long parseTime = now - (long) headers.get("timeTakenFromQueue");
                        log.debug("Annotation request took [{}] {} ms (parsing {} ms)",
                                headers.get("communication_id"),
                                runtime, parseTime);

/*                        parsed
                                .stream()
                                .sorted((o1, o2) -> ComparisonChain
                                        .start()
                                        .compare(o1.getDocumentId(), o2.getDocumentId())
                                        .compare(o1.getSection().name(), o2.getSection().name())
                                        .compare(o1.getInit(), o2.getInit())
                                        .result())
                                .forEach(r -> log.info(r.toString()));*/

                        return new AnnotationResponse(parsed, runtime, parseTime);
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
    private HttpMessageHandlerSpec sendToBecalmServer() {

        RestTemplate restTemplate = new RestTemplate();
        // we need to use a custom interceptor to allow multiple reading of the respons body
        // once to check if we have an error, a second time to see the results (if there was no error)
        restTemplate.getInterceptors().add(new BufferingClientHttpResponseWrapper());
        restTemplate.setErrorHandler(errorHandler());

        return Http
                .outboundGateway(annotatorConfig.becalmSaveAnnotationLocation, restTemplate)
                // use the post method
                .httpMethod(HttpMethod.POST)
                // replace URI placeholders using variables
                .uriVariable("apikey", "'" + annotatorConfig.apiKey + "'")
                .uriVariable("communicationId", "headers['communication_id']")
                .expectedResponseType(ServerResponse.class);
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
