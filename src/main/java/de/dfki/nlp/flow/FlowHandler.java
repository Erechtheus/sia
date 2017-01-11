package de.dfki.nlp.flow;

import de.dfki.nlp.MirNer;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.PredictionResult;
import de.dfki.nlp.domain.PredictionTypes;
import de.dfki.nlp.domain.rest.ServerRequest;
import de.dfki.nlp.loader.DocumentFetcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.amqp.Amqp;
import org.springframework.integration.dsl.support.Function;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.stereotype.Component;
import seth.SETH;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class FlowHandler {

    final DocumentFetcher documentFetcher;

    @Value("${apiKey}")
    String apiKey;

    // give each thread a new instance
    private static final ThreadLocal<SETH> SETH_THREAD_LOCAL = ThreadLocal.withInitial(() -> new SETH("resources/mutations.txt", true, true, false));
    private static final MirNer mirNer  = new MirNer();
    @Value("${server.concurrentConsumer}")
    int concurrentConsumer;

    @Value("${server.concurrentHandler}")
    int concurrentHandler;

    @Autowired
    public FlowHandler(DocumentFetcher documentFetcher) {
        this.documentFetcher = documentFetcher;
    }

    @Bean
    IntegrationFlow flow(ConnectionFactory connectionFactory, Queue input, Jackson2JsonMessageConverter messageConverter) {
        return IntegrationFlows
                .from(
                        Amqp.inboundAdapter(connectionFactory, input)
                                // set concurrentConsumers - anything larger than 1 gives parallelism per request
                                .concurrentConsumers(concurrentConsumer)
                                .messageConverter(messageConverter)
                                .headerMapper(DefaultAmqpHeaderMapper.inboundMapper())
                )
                .enrichHeaders(headerEnricherSpec -> headerEnricherSpec.headerExpression("communication_id", "payload.parameters.communication_id"))
                .split((Function<ServerRequest, List<ServerRequest.Document>>) serverRequest ->
                        // split documents
                        serverRequest.getParameters().getDocuments()
                )
                // handle in parallel using an executor on a different channel
                .channel(c -> c.executor(Executors.newFixedThreadPool(concurrentHandler)))
                .transform(ServerRequest.Document.class, documentFetcher::load)
                .transform(ParsedInputText.class, this::performAnnotation)
                .aggregate()
                // now merge the results by flattening
                .transform((GenericTransformer<List<List<PredictionResult>>, List<PredictionResult>>) source -> source.stream().flatMap(List::stream).collect(Collectors.toList()))
                .enrichHeaders(headerEnricherSpec -> headerEnricherSpec.headerExpression("Content-Type", "'application/json'"))
                //.handle(m -> log.info(m.toString()))
                .handleWithAdapter(adapters -> adapters
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
                        .uriVariable("apikey", "'" + apiKey + "'")
                        .uriVariable("communicationId", "headers['communication_id']"))
                .get();
    }


    private List<PredictionResult> performAnnotation(ParsedInputText payload) {

        if (payload.getExternalId() == null) return Collections.emptyList();

        List<PredictionResult> results = new ArrayList<>();


        log.trace("Parsing");
        if (payload.getTitle() != null) {
            Stream<PredictionResult> mutations = SETH_THREAD_LOCAL.get().findMutations(payload.getTitle()).stream().map(l -> new PredictionResult(payload.getExternalId(), PredictionResult.Section.T, l.getStart(), l.getEnd(), 1.0, l.getText(), PredictionTypes.MUTATION, null, null));
            Stream<PredictionResult> mirnas = mirNer.extractFromText(payload.getTitle()).stream().map(l -> new PredictionResult(payload.getExternalId(), PredictionResult.Section.T, l.getStart(), l.getEnd(), 1.0, l.getText(), PredictionTypes.MIRNA, null, null));
            results.addAll(Stream.concat(mutations, mirnas).collect(Collectors.toList()));
        }
        if (payload.getAbstractText() != null) {
            Stream<PredictionResult> mutations = SETH_THREAD_LOCAL.get().findMutations(payload.getAbstractText()).stream().map(l -> new PredictionResult(payload.getExternalId(), PredictionResult.Section.A, l.getStart(), l.getEnd(), 1.0, l.getText(), PredictionTypes.MUTATION, null, null));
            Stream<PredictionResult> mirnas = mirNer.extractFromText(payload.getAbstractText()).stream().map(l -> new PredictionResult(payload.getExternalId(), PredictionResult.Section.A, l.getStart(), l.getEnd(), 1.0, l.getText(), PredictionTypes.MIRNA, null, null));
            results.addAll(Stream.concat(mutations, mirnas).collect(Collectors.toList()));
        }
        log.trace("Done parsing");

        // transform
        return results;

    }


}
