package de.dfki.nlp.flow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ComparisonChain;
import de.dfki.nlp.config.AnnotatorConfig;
import de.dfki.nlp.domain.PredictionResult;
import de.dfki.nlp.domain.exceptions.Errors;
import de.dfki.nlp.domain.rest.ErrorResponse;
import de.dfki.nlp.domain.rest.ServerRequest;
import de.dfki.nlp.errors.FailedMessage;
import de.dfki.nlp.loader.DocumentFetcher;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
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
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.UnknownHttpStatusCodeException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FlowHandler {

    private final DocumentFetcher documentFetcher;

    private final AnnotatorConfig annotatorConfig;

    public FlowHandler(DocumentFetcher documentFetcher, AnnotatorConfig annotatorConfig) {
        this.documentFetcher = documentFetcher;
        this.annotatorConfig = annotatorConfig;
    }

    @Bean
    IntegrationFlow errorFlow(ObjectMapper objectMapper) {
        return IntegrationFlows
                .from("errorChannel")
                .handle(MessageHandlingException.class, (payload, headers) -> {
                    log.error("Failure sending results {}", payload.getMessage());

                    FailedMessage failedMessage = new FailedMessage();


                    Integer communicationId = (Integer) payload.getFailedMessage().getHeaders().getOrDefault("communication_id", -1);

                    failedMessage.setCommunicationId(communicationId);
                    try {
                        failedMessage.setFailedMessagePayload(objectMapper.writeValueAsString(payload.getFailedMessage().getPayload()));
                    } catch (JsonProcessingException e) {
                        log.error("Could not serialize the error message {}", e.getMessage());
                    }

                    failedMessage.setServerErrorCause(payload.getCause().getMessage());

                    // try to replicate most of the message and the error
                    if(payload.getCause() instanceof HttpClientErrorException) {
                        HttpClientErrorException cause = (HttpClientErrorException) payload.getCause();

                        String serverPayload = cause.getResponseBodyAsString();
                        failedMessage.setServerErrorPayload(serverPayload);

                    }

                    log.error("Failed Message for retry\n{}", failedMessage);
                    return null;
                })
                .get();
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
                        )
                        .enrichHeaders(headerEnricherSpec -> headerEnricherSpec.headerExpression("communication_id", "payload.parameters.communication_id"))
                        .enrichHeaders(headerEnricherSpec -> headerEnricherSpec.headerExpression("types", "payload.parameters.types"))
                        .split(ServerRequest.class, serverRequest ->
                                // split documents
                                serverRequest.getParameters().getDocuments()
                        )
                        // handle in parallel using an executor on a different channel
                        .channel(c -> c.executor("Downloader", Executors.newFixedThreadPool(annotatorConfig.getConcurrentHandler())))
                        .transform(ServerRequest.Document.class, documentFetcher::load)
                        .channel("annotate")
                        .transform(new Annotator())
                        .channel("aggregate")
                        .aggregate()
                        // now merge the results by flattening
                        .channel("jointogether")
                        .<List<Set<PredictionResult>>, Set<PredictionResult>>transform(source ->
                                source.stream().flatMap(Collection::stream).collect(Collectors.toSet()));


        if (environment.acceptsProfiles("cloud")) {
            // when cloud profile is active, send results via http
            flow
                    .enrichHeaders(headerEnricherSpec -> headerEnricherSpec.headerExpression("Content-Type", "'application/json'"))
                    // use retry advice - which tries to resend in case of failure
                    .handleWithAdapter(httpHandler(), e -> e.advice(retryAdvice()));

        } else {
            // for local deployment, just log
            flow
                    .<Set<PredictionResult>>handle((parsed, headers) -> {
                        log.info(headers.toString());

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
    private Function<Adapters, MessageHandlerSpec<?, HttpRequestExecutingMessageHandler>> httpHandler() {
        return adapters -> adapters
                // where to send the results to
                .http(annotatorConfig.becalmSaveAnnotationLocation)
                // use the post method
                .httpMethod(HttpMethod.POST)
                // replace URI placeholders using variables
                .uriVariable("apikey", "'" + annotatorConfig.apiKey + "'")
                .uriVariable("communicationId", "headers['communication_id']")
                .errorHandler(errorHandler());
    }


    @Bean
    ResponseErrorHandler errorHandler() {
        return new DefaultResponseErrorHandler() {

            ObjectMapper objectMapper = new ObjectMapper();

            @Override
            public void handleError(ClientHttpResponse response) throws IOException {

                // we have some sort of error
                // try to inspect the result
                HttpStatus statusCode = getHttpStatusCode(response);
                byte[] responseBody = getResponseBody(response);

                try {

                    ErrorResponse serverResponse = objectMapper.readValue(responseBody, ErrorResponse.class);

                    // now check the serverResponse
                    if (serverResponse.isSuccess()) {
                        log.info("Success sending results {}", serverResponse);
                        return;
                    }

                    switch (Errors.lookup(serverResponse.getErrorCode())) {

                        case REQUEST_CLOSED: // no error - we have sent it once .. continue
                            break;
                        default:
                            log.error("Error from server {}", serverResponse);
                            throw new HttpClientErrorException(statusCode, response.getStatusText(), response.getHeaders(), serverResponse.toString().getBytes(), getCharset(response));
                    }


                } catch (IOException e) {
                    log.error("Error parsing", e);
                    throw new HttpClientErrorException(statusCode, response.getStatusText(),
                            response.getHeaders(), responseBody, getCharset(response));
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
