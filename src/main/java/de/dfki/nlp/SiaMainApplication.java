package de.dfki.nlp;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.retry.annotation.EnableRetry;

import com.google.common.collect.Lists;

import de.dfki.nlp.config.EnabledAnnotators;
import de.dfki.nlp.config.MessagingConfig;
import de.dfki.nlp.domain.rest.ServerRequest;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
@EnableIntegration
@EnableRetry
public class SiaMainApplication {

    public static void main(String[] args) {
        SpringApplication.run(SiaMainApplication.class, args);
    }

    @Autowired
    Environment environment;

    @Bean
    @Profile("!cloud")
    CommandLineRunner commandLineRunner(MessagingConfig.ProcessingGateway processGateway,
            EnabledAnnotators enabledAnnotators) {

        // this sends a test message when the cloud profile is not active
        return args -> {
            ServerRequest message = new ServerRequest();
            ServerRequest.Documents parameters = new ServerRequest.Documents();
            parameters.setDocuments(
                    Lists.newArrayList(new ServerRequest.Document("CA2073855C", "Patent Server"),
                            new ServerRequest.Document("24218123", "PUBMED"),
                            new ServerRequest.Document("BC1403855C", "PMC"),
                            new ServerRequest.Document("PMC20255", "PMC"),
                            new ServerRequest.Document("US20080038365", "Patent Server"),
                            new ServerRequest.Document("WO2010032704A1", "Patent Server"),
                            new ServerRequest.Document("WO2012005339A1", "Patent Server"),
                            new ServerRequest.Document("US20110195924", "Patent Server"),

                            new ServerRequest.Document("10022392", "PUBMED"),
                            new ServerRequest.Document("10022392", "ABSTRACT SERVER"),
                            new ServerRequest.Document("1422080", "ABSTRACT SERVER")));

            parameters.setExpired(Date.from(Instant.now().plusSeconds(60)));

            parameters.setCommunication_id(-1);
            parameters.setTypes(Lists.newArrayList(enabledAnnotators.enabledPredicationTypes()));
            // TODO re-enable
            // parameters.setExpired(Date.from(ZonedDateTime.now(ZoneId.of("Europe/Berlin")).plusMinutes(30).toInstant()));
            message.setParameters(parameters);
            message.setMethod(ServerRequest.Method.getAnnotations);
            // send one test message
            String ttlInMs = String.valueOf(TimeUnit.MILLISECONDS.convert(30, TimeUnit.DAYS));

            processGateway.sendForProcessing(message, ttlInMs, System.currentTimeMillis());

            log.info("Accepting requests on http://localhost:{}/", environment.getProperty("server.port", "8080"));
        };

    }

}
