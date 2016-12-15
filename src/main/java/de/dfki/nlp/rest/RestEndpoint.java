package de.dfki.nlp.rest;

import de.dfki.nlp.domain.rest.Response;
import de.dfki.nlp.domain.rest.ServerRequest;
import de.dfki.nlp.domain.rest.Stats;
import de.dfki.nlp.rest.exceptions.BaseException;
import de.dfki.nlp.rest.exceptions.PayloadException;
import de.dfki.nlp.rest.exceptions.UnsupportedMethodException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@RestController
public class RestEndpoint {

    public static String COMMUNICATION_ID = "communication_id";

    @Autowired
    JmsTemplate jmsTemplate;


    @Value("${serverKey}")
    String serverKey;

    @PostMapping("/call")
    public Response getAnnotations(@RequestBody @Valid ServerRequest serverRequest) throws BaseException {

        // TODO validate the object further

        switch (serverRequest.getMethod()) {
            case getAnnotations:

                if (serverRequest.getParameters() == null) {
                    throw new PayloadException("Request Parameter not set");
                }

                // send
                jmsTemplate.convertAndSend("input", serverRequest, message1 -> {
                    message1.setIntProperty(COMMUNICATION_ID, serverRequest.getParameters().getCommunication_id());
                    return message1;
                });

                return new Response(200, true, serverKey, null);
            case getState:
                return new Response(200, true, serverKey, new Stats(ZonedDateTime.now(ZoneId.of("Europe/Paris"))));
            default:
                throw new UnsupportedMethodException("Don't know how to handle " + serverRequest.getMethod());
        }

    }

}
