package de.dfki.nlp;

import com.google.common.collect.Lists;
import de.dfki.nlp.domain.PredictionResult;
import de.dfki.nlp.domain.rest.ServerRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.jms.Jms;
import org.springframework.integration.dsl.support.Function;
import org.springframework.integration.dsl.support.GenericHandler;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import javax.jms.ConnectionFactory;
import java.util.List;
import java.util.concurrent.Executors;

@SpringBootApplication
@Slf4j
@EnableJms
public class SethTipsApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(SethTipsApplication.class, args);
    }

    @Bean
    IntegrationFlow flow(ConnectionFactory connectionFactory) {
        return IntegrationFlows
                .from(Jms.messageDrivenChannelAdapter(connectionFactory)
                        .configureListenerContainer(container ->
                        container.sessionTransacted(true))
                        .jmsMessageConverter(jacksonJmsMessageConverter()).destination("input"))
                // set parallelism - anything larger than 1 gives parallelism
                .channel(c -> c.executor(Executors.newFixedThreadPool(1)))
                .split((Function<ServerRequest, List<ServerRequest.Document>>) serverRequest ->
                    // split documents
                    serverRequest.getParameters().getDocuments()
                )
                .handle((GenericHandler<ServerRequest.Document>) (payload, headers) -> {
                    log.info("Download the text {}", payload.toString());
                    return payload;
                }).handle(ServerRequest.Document.class, (payload, headers) -> {
                    // TODO handle SETH
                    PredictionResult predictionResult = new PredictionResult();
                    predictionResult.setDocumentId(payload.getDocument_id());
                    return predictionResult;
                })
                .aggregate()
                // TODO here we would post the result
                .channel(new MessageChannel() {
                    @Override
                    public boolean send(Message<?> message) {
                        List<PredictionResult> payload = (List<PredictionResult>) message.getPayload();
                        log.info("Sending response direct with {} results \n For communication_id {}", payload.size(), message.getHeaders().get("communication_id"));
                        return true;
                    }

                    @Override
                    public boolean send(Message<?> message, long timeout) {
                        log.info("Sending response with timeout");
                        return true;
                    }
                })
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
                new ServerRequest.Document("BC1403854C", "PUBMED"),
                new ServerRequest.Document("BC1403854C", "PUBMED")));
        message.setParameters(parameters);

        jmsTemplate.convertAndSend("input", message, message1 -> {
            message1.setIntProperty("communication_id", 101);
            return message1;
        });


    }




}
