package de.dfki.nlp;

import static de.dfki.nlp.config.MessagingConfig.ProcessingGateway;
import static de.dfki.nlp.config.MessagingConfig.queueName;
import static de.dfki.nlp.config.MessagingConfig.queueOutput;
import static org.springframework.amqp.rabbit.core.RabbitAdmin.QUEUE_MESSAGE_COUNT;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.oro.io.GlobFilenameFilter;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import de.dfki.nlp.config.EnabledAnnotators;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.pubmed.PubmedArticle;
import de.dfki.nlp.domain.pubmed.PubmedArticleSet;
import de.dfki.nlp.domain.rest.ServerRequest;
import de.dfki.nlp.loader.PubMedDocumentFetcher;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
@EnableIntegration
@EnableRetry
@EnableScheduling
public class SiaPubmedAnnotator {

    public static void main(String[] args) {

        new SpringApplicationBuilder(SiaPubmedAnnotator.class).web(WebApplicationType.NONE).run(
                args);
    }

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    private AtomicBoolean shutdown = new AtomicBoolean(false);

    @Scheduled(fixedRate = 5000)
    public void reportCurrentTime() throws InterruptedException {

        // check length
        Integer countIn = (Integer) amqpAdmin.getQueueProperties(queueName).get(
                QUEUE_MESSAGE_COUNT);
        Integer countOut = (Integer) amqpAdmin.getQueueProperties(queueOutput).get(
                QUEUE_MESSAGE_COUNT);

        if ((countIn == 0) && (countOut == 0) && doneLoading.get() && !shutdown.get()) {
            shutdown.set(true);
            log.info("Done annotating documents");
            // don't close immediately ..
            TimeUnit.SECONDS.sleep(5);
            applicationContext.close();
        }

        log.info("Message counts input queue: {} output queue: {}", countIn, countOut);
    }

    private AtomicBoolean doneLoading = new AtomicBoolean(false);

    @Bean
    CommandLineRunner commandLineRunner(ProcessingGateway processGateway,
            EnabledAnnotators enabledAnnotators) {

        return args -> {

            Jaxb2RootElementHttpMessageConverter converter = new Jaxb2RootElementHttpMessageConverter();
            converter.setSupportDtd(true);

            ServerRequest message = new ServerRequest();
            ServerRequest.Documents parameters = new ServerRequest.Documents();
            parameters.setCommunication_id(-1);
            parameters.setTypes(Lists.newArrayList(enabledAnnotators.enabledPredicationTypes()));
            message.setParameters(parameters);
            message.setMethod(ServerRequest.Method.getAnnotations);
            parameters.setExpired(Date.from(Instant.now().plusSeconds(60 * 1000)));

            // load pubmed files
            File directory = new File("tools/pubmedcache");
            String[] list = directory.list(new GlobFilenameFilter("*.gz"));

            for (String filename : list) {

                File inputFile = new File(directory, filename);

                log.info("Loading articles from {}", inputFile.toString());

                try (CompressorInputStream is = new CompressorStreamFactory().createCompressorInputStream(
                        new BufferedInputStream(new FileInputStream(inputFile), 1024 * 64))) {

                    PubmedArticleSet read = (PubmedArticleSet) converter.read(
                            PubmedArticleSet.class, new HttpInputMessage() {
                                @Override
                                public InputStream getBody() {
                                    return is;
                                }

                                @Override
                                public HttpHeaders getHeaders() {
                                    return null;
                                }
                            });

                    Iterables.limit(read.getPubmedArticleOrPubmedBookArticle(), 100).forEach(a -> {
                        ParsedInputText parsedInputText = PubMedDocumentFetcher.convert(
                                (PubmedArticle) a);

                        String ttlInMs = String.valueOf(
                                TimeUnit.MILLISECONDS.convert(30, TimeUnit.DAYS));
                        ServerRequest.Document inline = new ServerRequest.Document(
                                parsedInputText.getExternalId(), "INLINE");

                        inline.setAbstractText(
                                StringUtils.trimToNull(parsedInputText.getAbstractText()));
                        inline.setTitle(StringUtils.trimToNull(parsedInputText.getTitle()));
                        inline.setText(StringUtils.trimToNull(parsedInputText.getText()));

                        message.getParameters().setDocuments(Lists.newArrayList(inline));

                        // send away
                        processGateway.sendForProcessing(message, ttlInMs,
                                System.currentTimeMillis());

                    });

                }
            }

            doneLoading.set(true);

        };
    }

}
