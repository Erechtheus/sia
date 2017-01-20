package de.dfki.nlp;

import com.google.common.collect.Lists;
import de.dfki.nlp.config.MessagingConfig;
import de.dfki.nlp.domain.rest.ServerRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.config.EnableIntegration;

@SpringBootApplication
@Slf4j
@EnableIntegration
public class SethTipsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SethTipsApplication.class, args);
    }


    @Bean
    @Profile("!cloud")
    CommandLineRunner commandLineRunner(MessagingConfig.ProcessingGateway processingGateway) {

        // this sends a test message when the cloud profile is not active

        return args -> {
            ServerRequest message = new ServerRequest();
            ServerRequest.Documents parameters = new ServerRequest.Documents();
            parameters.setDocuments(Lists.newArrayList(
                    new ServerRequest.Document("CA2073855C", "Patent Server"),
                    new ServerRequest.Document("24218123", "PUBMED"),
                    new ServerRequest.Document("BC1403855C", "PMC"),
                    new ServerRequest.Document("US20080038365", "Patent Server"),
                    new ServerRequest.Document("WO2010032704A1", "Patent Server"),
                    new ServerRequest.Document("WO2012005339A1", "Patent Server"),
                    new ServerRequest.Document("US20110195924", "Patent Server")
                    )
            );

            parameters.setCommunication_id(-1);
            // TODO re-enable
            //parameters.setExpired(Date.from(ZonedDateTime.now(ZoneId.of("Europe/Berlin")).plusMinutes(30).toInstant()));
            message.setParameters(parameters);

            // send one test message
            processingGateway.sendForProcessing(message);

        };

    }


}
