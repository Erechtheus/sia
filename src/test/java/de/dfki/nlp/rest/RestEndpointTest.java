package de.dfki.nlp.rest;

import de.dfki.nlp.domain.rest.ErrorResponse;
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

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity("/call", serverRequest, ErrorResponse.class);

        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getErrorCode()).isEqualTo("1");

    }

}