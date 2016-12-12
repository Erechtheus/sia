package de.dfki.nlp.rest;

import de.dfki.nlp.domain.rest.Response;
import de.dfki.nlp.domain.rest.ServerRequest;
import de.dfki.nlp.domain.rest.Stats;
import de.dfki.nlp.rest.exceptions.BaseException;
import de.dfki.nlp.rest.exceptions.PayloadException;
import de.dfki.nlp.rest.exceptions.UnsupportedMethodException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.ZonedDateTime;

@RestController
public class RestEndpoint {

    @Autowired
    JmsTemplate jmsTemplate;

    @PostMapping("/call")
    public Response getAnnotations(@RequestBody @Valid ServerRequest serverRequest) throws BaseException {

        // TODO validate the object further

        switch (serverRequest.getMethod()) {
            case getAnnotations:

                if(serverRequest.getParameters() == null) {
                    throw new PayloadException("Request Parameter not set");
                }

                // send
                jmsTemplate.convertAndSend("input", serverRequest, message1 -> {
                    message1.setIntProperty("communication_id", serverRequest.getParameters().getCommunication_id());
                    return message1;
                });

                return new Response(200, true, "", null);
            case getState:
                return new Response(200, true, "", new Stats(ZonedDateTime.now()));
            default:
                throw new UnsupportedMethodException("Don't know how to handle " + serverRequest.getMethod());
        }

    }

}
