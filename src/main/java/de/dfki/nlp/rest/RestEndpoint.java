package de.dfki.nlp.rest;

import de.dfki.nlp.config.MessagingConfig;
import de.dfki.nlp.domain.exceptions.BaseException;
import de.dfki.nlp.domain.exceptions.PayloadException;
import de.dfki.nlp.domain.exceptions.UnsupportedMethodException;
import de.dfki.nlp.domain.rest.Response;
import de.dfki.nlp.domain.rest.ServerRequest;
import de.dfki.nlp.domain.rest.ServerState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
public class RestEndpoint {

    @Value("${apiKey}")
    String apiKey;

    @Value("${server.version}")
    String version;

    @Value("${server.changes}")
    String changes;

    @Value("${server.max:500}")
    String max;

    @Autowired
    MessagingConfig.ProcessingGateway processGateway;

    @PostMapping("/call")
    public Response getAnnotations(@RequestBody @Valid ServerRequest serverRequest) throws BaseException {

        switch (serverRequest.getMethod()) {
            case getAnnotations:

                if (serverRequest.getParameters() == null) {
                    throw new PayloadException("Request Parameter not set");
                }

                // send
                processGateway.sendForProcessing(serverRequest);

                return new Response(200, true, apiKey, null);
            case getState:
                return new Response(200, true, apiKey, new ServerState(ServerState.State.Running, version, changes, max));
            default:
                throw new UnsupportedMethodException("Don't know how to handle " + serverRequest.getMethod());
        }

    }

}
