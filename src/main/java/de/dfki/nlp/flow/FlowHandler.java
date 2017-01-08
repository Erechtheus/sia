package de.dfki.nlp.flow;

import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.PredictionResult;
import de.dfki.nlp.domain.rest.ServerRequest;
import de.dfki.nlp.loader.DocumentFetcher;
import de.hu.berlin.wbi.objects.MutationMention;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
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
import org.springframework.integration.util.CallerBlocksPolicy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import seth.SETH;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
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

    @Value("${server.concurrentConsumer}")
    int concurrentConsumer;

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
                .channel(c -> c.executor(executor()))
                .transform(ServerRequest.Document.class, documentFetcher::load)
                .transform(ParsedInputText.class, this::performAnnotation)
                .aggregate()
                // now merge the results by flattening
                .transform((GenericTransformer<List<List<Object>>, List<Object>>) source -> source.stream().flatMap(List::stream).collect(Collectors.toList()))
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

            List<MutationMention> mutationsTitle = Collections.emptyList();
            List<MutationMention> mutationsAbstract = Collections.emptyList();

            log.trace("Parsing");
            if (payload.getTitle() != null) {
                mutationsTitle = SETH_THREAD_LOCAL.get().findMutations(payload.getTitle());
            }
            if (payload.getAbstractText() != null) {
                mutationsAbstract = SETH_THREAD_LOCAL.get().findMutations(payload.getAbstractText());
            }
            log.trace("Done parsing");

            // transform

            Stream<Pair<MutationMention, PredictionResult.Section>> title = mutationsTitle.stream().map(m -> Pair.of(m, PredictionResult.Section.T));
            Stream<Pair<MutationMention, PredictionResult.Section>> abstractT = mutationsAbstract.stream().map(m -> Pair.of(m, PredictionResult.Section.A));


            return Stream.concat(title, abstractT).map(pair -> {
                PredictionResult predictionResult = new PredictionResult();
                predictionResult.setDocumentId(payload.getExternalId());

                MutationMention mutationMentions = pair.getKey();

                predictionResult.setInit(mutationMentions.getStart());
                predictionResult.setEnd(mutationMentions.getEnd());
                predictionResult.setAnnotatedText(mutationMentions.getText());
                predictionResult.setSection(pair.getValue());
                predictionResult.setType(mutationMentions.getType().name());
                predictionResult.setDocumentId(payload.getExternalId());

                predictionResult.setScore(1d);

                return predictionResult;
            }).collect(Collectors.toList());

    }

    @Bean
    public Executor executor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setMaxPoolSize(concurrentConsumer);
        threadPoolTaskExecutor.setCorePoolSize(concurrentConsumer);
        threadPoolTaskExecutor.setQueueCapacity(concurrentConsumer * 40);
        threadPoolTaskExecutor.setRejectedExecutionHandler(new CallerBlocksPolicy(Integer.MAX_VALUE));
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }

}
