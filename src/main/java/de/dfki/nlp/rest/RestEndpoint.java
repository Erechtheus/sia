package de.dfki.nlp.rest;

import de.dfki.nlp.domain.rest.Response;
import de.dfki.nlp.domain.rest.ServerRequest;
import de.dfki.nlp.rest.exceptions.UnsupportedMethodException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
public class RestEndpoint {

    @PostMapping("/call")
    public Response getAnnotations(@RequestBody @Valid ServerRequest serverRequest) throws UnsupportedMethodException {

        switch (serverRequest.getMethod()) {
            case getAnnotations:
                return new Response(200, true, "", null);
            case getState:
                return new Response(200, true, "", null);
            default:
                throw new UnsupportedMethodException();
        }

    }

}
