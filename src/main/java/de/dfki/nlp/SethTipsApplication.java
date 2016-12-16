package de.dfki.nlp;

import com.google.common.collect.Lists;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.PredictionResult;
import de.dfki.nlp.domain.rest.ServerRequest;
import de.dfki.nlp.loader.DocumentLoader;
import de.hu.berlin.wbi.objects.MutationMention;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.jms.Jms;
import org.springframework.integration.dsl.support.Function;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.web.client.ResponseErrorHandler;
import seth.SETH;

import javax.jms.ConnectionFactory;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
@Slf4j
@EnableJms
public class SethTipsApplication implements CommandLineRunner {

    @Value("${apiKey}")
    String apiKey;

    public static void main(String[] args) {
        SpringApplication.run(SethTipsApplication.class, args);
    }

    @Autowired
    DocumentLoader documentLoader;

    // give each thread a new instance
    private static final ThreadLocal<SETH> SETH_THREAD_LOCAL = ThreadLocal.withInitial(() -> new SETH("resources/mutations.txt", true, true, false));

    @Bean
    IntegrationFlow flow(ConnectionFactory connectionFactory) {
        return IntegrationFlows
                .from(Jms.messageDrivenChannelAdapter(connectionFactory)
                        .configureListenerContainer(container ->
                                container.sessionTransacted(true))
                        .jmsMessageConverter(jacksonJmsMessageConverter()).destination("input"))
                // set parallelism - anything larger than 1 gives parallelism
                .channel(c -> c.executor(Executors.newFixedThreadPool(2)))
                .split((Function<ServerRequest, List<ServerRequest.Document>>) serverRequest ->
                        // split documents
                        serverRequest.getParameters().getDocuments()
                )
                .transform(ServerRequest.Document.class, source -> documentLoader.load(source))
                .transform(ParsedInputText.class, payload -> {
                    if (payload.getExternalId() == null) return Collections.emptyList();

                    // TODO handle SETH for title and abstract

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
                                System.out.println(response.getStatusText());
                                System.out.println(IOUtils.toString(response.getBody()));
                            }
                        })
                        .uriVariable("apikey", "'" + apiKey + "'")
                        .uriVariable("communicationId", "headers['communication_id']"))
                .get();
    }

    ;

    @Bean // Serialize message content to json using TextMessage
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    @Autowired
    JmsTemplate jmsTemplate;

    @Override
    public void run(String... args) throws Exception {

        ServerRequest message = new ServerRequest();
        ServerRequest.Documents parameters = new ServerRequest.Documents();
        parameters.setDocuments(Lists.newArrayList(
                new ServerRequest.Document("CA2073855C", "Patent Server"),
                new ServerRequest.Document("24218123", "PUBMED"),
                new ServerRequest.Document("BC1403855C", "PMC")
                )
        );
        message.setParameters(parameters);

        jmsTemplate.convertAndSend("input", message, m -> {
            m.setIntProperty("communication_id", 101);
            return m;
        });


    }


}
