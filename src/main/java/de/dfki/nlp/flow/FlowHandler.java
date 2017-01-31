package de.dfki.nlp.flow;

import com.google.common.collect.ComparisonChain;
import de.dfki.nlp.config.AnnotatorConfig;
import de.dfki.nlp.domain.PredictionResult;
import de.dfki.nlp.domain.rest.ServerRequest;
import de.dfki.nlp.loader.DocumentFetcher;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
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
                                        // set concurrentConsumers - anything larger than 1 gives parallelism per request
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
                    .handleWithAdapter(httpHandler());
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
     * Handles the BECALM post requests
     *
     * @return the HTTP adapter
     */
    private Function<Adapters, MessageHandlerSpec<?, HttpRequestExecutingMessageHandler>> httpHandler() {
        return adapters -> adapters
                .http("http://www.becalm.eu/api/saveAnnotations/JSON?apikey={apikey}&communicationId={communicationId}")
                .httpMethod(HttpMethod.POST)
/*                        .errorHandler(new ResponseErrorHandler() {
                    @Override
                    public boolean hasError(ClientHttpResponse response) throws IOException {
                        // TODO this is not really an error .. just pretend it is one, so we can view the result
                        return response.getRawStatusCode() != 20;
                    }

                    @Override
                    public void handleError(ClientHttpResponse response) throws IOException {
                        log.debug(response.getStatusText());
                        log.debug(IOUtils.toString(response.getBody()));
                    }
                })*/
                .uriVariable("apikey", "'" + annotatorConfig.apiKey + "'")
                .uriVariable("communicationId", "headers['communication_id']");
    }



}
