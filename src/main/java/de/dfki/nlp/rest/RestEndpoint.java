package de.dfki.nlp.rest;

import static de.dfki.nlp.domain.exceptions.Errors.NEED_PARAMETERS;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.MoreObjects;

import de.dfki.nlp.config.AnnotatorConfig;
import de.dfki.nlp.config.MessagingConfig;
import de.dfki.nlp.domain.exceptions.BaseException;
import de.dfki.nlp.domain.exceptions.PayloadException;
import de.dfki.nlp.domain.exceptions.UnsupportedMethodException;
import de.dfki.nlp.domain.rest.Response;
import de.dfki.nlp.domain.rest.ServerRequest;
import de.dfki.nlp.domain.rest.ServerState;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class RestEndpoint {

    private final AnnotatorConfig annotatorConfig;
    private final MessagingConfig.ProcessingGateway processGateway;

    public RestEndpoint(AnnotatorConfig annotatorConfig, MessagingConfig.ProcessingGateway processGateway) {
        this.annotatorConfig = annotatorConfig;
        this.processGateway = processGateway;
    }

    @PostMapping("/call")
    public Response getAnnotations(@RequestBody @Valid ServerRequest serverRequest) throws BaseException {

        switch (serverRequest.getMethod()) {
            case getAnnotations:

                if (serverRequest.getParameters() == null) {
                    throw new PayloadException("Request Parameter not set");
                }

                if (CollectionUtils.isEmpty(serverRequest.getParameters().getDocuments())) {
                    throw new BaseException("No documents specified", NEED_PARAMETERS.errorCode);
                }

                Set<String> collect = serverRequest.getParameters().getDocuments().stream().map(ServerRequest.Document::getSource).collect(Collectors.toSet());


                // calculate how long we have time to process in seconds - by default 120s
                Date expireAt = MoreObjects.firstNonNull(
                        serverRequest.getParameters().getExpired(),
                        DateUtils.addSeconds(new Date(), 120));
                Duration duration = Duration.between(Instant.now(), expireAt.toInstant());

                log.info("We have {} seconds to fulfill the request [{}]", duration.getSeconds(), serverRequest.getParameters().getCommunication_id());

                log.info("Request to analyze {} documents with types : {} from {} for id [{}] - with expiry date [{}]", serverRequest.getParameters().getDocuments().size(), serverRequest.getParameters().getTypes(), collect.toString(), serverRequest.getParameters().getCommunication_id(), expireAt);

                // calculate ttl - set it to 1 month ...
                String ttlInMs = String.valueOf(TimeUnit.MILLISECONDS.convert(30, TimeUnit.DAYS));
                // send
                processGateway.sendForProcessing(serverRequest, ttlInMs, System.currentTimeMillis());

                return new Response(200, true, annotatorConfig.apiKey, null);
            case getState:
                return new Response(200, true, annotatorConfig.apiKey, new ServerState(ServerState.State.Running, annotatorConfig.version, annotatorConfig.changes, annotatorConfig.maxDaily));
            default:
                throw new UnsupportedMethodException("Don't know how to handle " + serverRequest.getMethod());
        }

    }

}
