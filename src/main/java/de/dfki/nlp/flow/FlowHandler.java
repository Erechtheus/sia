package de.dfki.nlp.flow;

import de.dfki.nlp.MirNer;
import de.dfki.nlp.config.AnnotatorConfig;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.PredictionResult;
import de.dfki.nlp.domain.PredictionTypes;
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
import seth.SETH;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import de.dfki.nlp.diseaseNer.DiseasesNer;
@Slf4j
@Component
public class FlowHandler {

    private final DocumentFetcher documentFetcher;

    // give each thread a new instance - this might not be needed
    //private static final ThreadLocal<SETH> SETH_THREAD_LOCAL = ThreadLocal.withInitial(() -> new SETH("resources/mutations.txt", true, true, false));
    private static final SETH SETH_DETECTOR = new SETH("resources/mutations.txt", true, true, false);
    private static final MirNer mirNer = new MirNer();
    private static final DiseasesNer diseaseNer = new DiseasesNer();

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
                        .split(ServerRequest.class, serverRequest ->
                                // split documents
                                serverRequest.getParameters().getDocuments()
                        )
                        // handle in parallel using an executor on a different channel
                        .channel(c -> c.executor(Executors.newFixedThreadPool(annotatorConfig.getConcurrentHandler())))
                        .transform(ServerRequest.Document.class, documentFetcher::load)
                        .transform(ParsedInputText.class, this::performAnnotation)
                        .aggregate()
                        // now merge the results by flattening
                        .<List<List<PredictionResult>>, List<PredictionResult>>transform(source ->
                                source.stream().flatMap(List::stream).collect(Collectors.toList()));


        if (environment.acceptsProfiles("cloud")) {
            // when cloud profile is active, send results via http
            flow
                    .enrichHeaders(headerEnricherSpec -> headerEnricherSpec.headerExpression("Content-Type", "'application/json'"))
                    .handleWithAdapter(httpHandler());
        } else {
            // for local deployment, just log
            flow
                    .<List<PredictionResult>>handle((parsed, headers) -> {
                        log.info(headers.toString());
                        for (PredictionResult predictionResult : parsed) {
                            log.info(predictionResult.toString());
                        }
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


    private List<PredictionResult> performAnnotation(ParsedInputText payload) {

        if (payload.getExternalId() == null) return Collections.emptyList();

        List<PredictionResult> results = new ArrayList<>();

        log.trace("Parsing");
        if (payload.getTitle() != null) {
            Stream<PredictionResult> mutations = SETH_DETECTOR.findMutations(payload.getTitle()).stream().map(l -> new PredictionResult(payload.getExternalId(), PredictionResult.Section.T, l.getStart(), l.getEnd(), 1.0, l.getText(), PredictionTypes.MUTATION, null, null));
            Stream<PredictionResult> mirnas = mirNer.extractFromText(payload.getTitle()).stream().map(l -> new PredictionResult(payload.getExternalId(), PredictionResult.Section.T, l.getStart(), l.getEnd(), 1.0, l.getText(), PredictionTypes.MIRNA, null, null));
            Stream<PredictionResult> diseases = diseaseNer.extractFromText(payload.getTitle()).stream().map(l -> new PredictionResult(payload.getExternalId(), PredictionResult.Section.T, l.getStart(), l.getEnd(), 1.0, l.getText(), PredictionTypes.DISEASE, null, null));

            results.addAll(Stream.concat(Stream.concat(mutations, mirnas),diseases).collect(Collectors.toList()));
        }
        if (payload.getAbstractText() != null) {
            Stream<PredictionResult> mutations = SETH_DETECTOR.findMutations(payload.getAbstractText()).stream().map(l -> new PredictionResult(payload.getExternalId(), PredictionResult.Section.A, l.getStart(), l.getEnd(), 1.0, l.getText(), PredictionTypes.MUTATION, null, null));
            Stream<PredictionResult> mirnas = mirNer.extractFromText(payload.getAbstractText()).stream().map(l -> new PredictionResult(payload.getExternalId(), PredictionResult.Section.A, l.getStart(), l.getEnd(), 1.0, l.getText(), PredictionTypes.MIRNA, null, null));
            Stream<PredictionResult> diseases = diseaseNer.extractFromText(payload.getAbstractText()).stream().map(l -> new PredictionResult(payload.getExternalId(), PredictionResult.Section.A, l.getStart(), l.getEnd(), 1.0, l.getText(), PredictionTypes.DISEASE, null, null));

            results.addAll(Stream.concat(Stream.concat(mutations, mirnas),diseases).collect(Collectors.toList()));
        }
        log.trace("Done parsing");

        // transform
        return results;

    }


}
