package de.dfki.nlp;

import com.google.common.collect.Lists;
import de.dfki.nlp.config.MessagingConfig;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.PredictionResult;
import de.dfki.nlp.domain.rest.ServerRequest;
import de.dfki.nlp.loader.DocumentLoader;
import de.hu.berlin.wbi.objects.MutationMention;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.amqp.Amqp;
import org.springframework.integration.dsl.support.Function;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.web.client.ResponseErrorHandler;
import seth.SETH;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
@Slf4j
@EnableIntegration
public class SethTipsApplication {

    @Value("${apiKey}")
    String apiKey;

    public static void main(String[] args) {
        SpringApplication.run(SethTipsApplication.class, args);
    }

    @Autowired
    DocumentLoader documentLoader;

    // give each thread a new instance
    private static final ThreadLocal<SETH> SETH_THREAD_LOCAL = ThreadLocal.withInitial(() -> new SETH("resources/mutations.txt", true, true, false));

    @Value("${server.concurrentConsumer}")
    int concurrentConsumer;

    @Bean
    IntegrationFlow flow(ConnectionFactory connectionFactory, Queue input, Jackson2JsonMessageConverter messageConverter) {
        return IntegrationFlows
                .from(
                        Amqp.inboundAdapter(connectionFactory, input)
                                // set parallelism - anything larger than 1 gives parallelism
                                .concurrentConsumers(concurrentConsumer)
                                .messageConverter(messageConverter)
                                .headerMapper(DefaultAmqpHeaderMapper.inboundMapper())
                )
                .enrichHeaders(headerEnricherSpec -> headerEnricherSpec.headerExpression("communication_id", "payload.parameters.communication_id"))
                .split((Function<ServerRequest, List<ServerRequest.Document>>) serverRequest ->
                        // split documents
                        serverRequest.getParameters().getDocuments()
                )
                .transform(ServerRequest.Document.class, source -> documentLoader.load(source))
                .transform(ParsedInputText.class, payload -> {
                    if (payload.getExternalId() == null) return Collections.emptyList();

                    List<MutationMention> mutationsTitle = Collections.emptyList();
                    List<MutationMention> mutationsAbstract = Collections.emptyList();

                    log.info("Parsing");
                    if (payload.getTitle() != null) {
                        mutationsTitle = SETH_THREAD_LOCAL.get().findMutations(payload.getTitle());
                    }
                    if (payload.getAbstractText() != null) {
                        mutationsAbstract = SETH_THREAD_LOCAL.get().findMutations(payload.getAbstractText());
                    }
                    log.info("Done parsing");

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

                })
                .aggregate()
                // now merge the results by flattening
                .transform((GenericTransformer<List<List<Object>>, List<Object>>) source -> source.stream().flatMap(List::stream).collect(Collectors.toList()))
                .enrichHeaders(headerEnricherSpec -> headerEnricherSpec.headerExpression("Content-Type", "'application/json'"))
                .handleWithAdapter(adapters -> adapters
                        .http("http://www.becalm.eu/api/saveAnnotations/JSON?apikey={apikey}&communicationId={communicationId}")
                        .httpMethod(HttpMethod.POST)
                        .errorHandler(new ResponseErrorHandler() {
                            @Override
                            public boolean hasError(ClientHttpResponse response) throws IOException {
                                // TODO this is not really an error .. just pretend it is one, so we can view the result
                                return response.getRawStatusCode() != 20;
                            }

                            @Override
                            public void handleError(ClientHttpResponse response) throws IOException {
                               log.info(response.getStatusText());
                               log.info(IOUtils.toString(response.getBody()));
                            }
                        })
                        .uriVariable("apikey", "'" + apiKey + "'")
                        .uriVariable("communicationId", "headers['communication_id']"))
                .get();
    }


    @Bean
    @Profile("!cloud")
    CommandLineRunner commandLineRunner(MessagingConfig.ProcessingGateway processingGateway) {

        return args -> {
            ServerRequest message = new ServerRequest();
            ServerRequest.Documents parameters = new ServerRequest.Documents();
            parameters.setDocuments(Lists.newArrayList(
                    new ServerRequest.Document("CA2073855C", "Patent Server"),
                    new ServerRequest.Document("24218123", "PUBMED"),
                    new ServerRequest.Document("BC1403855C", "PMC")
                    )
            );

            parameters.setCommunication_id(-1);
            message.setParameters(parameters);

            // send one test message
            processingGateway.sendForProcessing(message);


        };

    }


}
