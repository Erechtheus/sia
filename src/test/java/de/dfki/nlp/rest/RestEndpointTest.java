package de.dfki.nlp.rest;

import de.dfki.nlp.domain.rest.Response;
import de.dfki.nlp.domain.rest.ServerRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RestEndpointTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void getAnnotations() throws Exception {

        ServerRequest serverRequest = new ServerRequest();
        serverRequest.setMethod(ServerRequest.Method.getAnnotations);

        ResponseEntity<Response> response = restTemplate.postForEntity("/call", serverRequest, Response.class);

        assertThat(response.getBody().isSuccess()).isTrue();

    }

}