package de.dfki.nlp.flow;

import com.google.common.collect.ComparisonChain;
import de.dfki.nlp.config.AnnotatorConfig;
import de.dfki.nlp.domain.PredictionResult;
import de.dfki.nlp.domain.rest.ServerRequest;
import de.dfki.nlp.loader.DocumentFetcher;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
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
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

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
                        .channel(c -> c.executor(Executors.newFixedThreadPool(annotatorConfig.getConcurrentHandler())))
                        .transform(ServerRequest.Document.class, documentFetcher::load)
                        .transform(new Annotator())
                        .aggregate()
                        // now merge the results by flattening
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
        retryTemplate.setBackOffPolicy(new ExponentialBackOffPolicy());

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
                .uriVariable("communicationId", "headers['communication_id']");
    }

}
