package de.dfki.nlp.rest;

import de.dfki.nlp.domain.rest.ErrorResponse;
import de.dfki.nlp.domain.rest.Response;
import de.dfki.nlp.domain.rest.ServerRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    @Test
    public void getGetState() throws Exception {

        ServerRequest serverRequest = new ServerRequest();
        serverRequest.setMethod(ServerRequest.Method.getState);

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity("/call", serverRequest, ErrorResponse.class);

        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getErrorCode()).isEqualTo("1");

        // missing parameter
    }

    @Test
    public void getGetStateWithArray() throws Exception {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestJson = "{\n" +
                "    \"name\": \"BeCalm\",\n" +
                "    \"method\": \"getState\",\n" +
                "    \"becalm_key\": \"serverNotCreatedJet\",\n" +
                "    \"custom_parameters\": {\n" +
                "\n" +
                "    },\n" +
                "    \"parameters\": [\n" +
                "\n" +
                "    ]\n" +
                "}";

        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

        ResponseEntity<Response> response = restTemplate.postForEntity("/call", entity, Response.class);

        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getStatus()).isEqualTo(200);

        assertThat(response.getBody().getData()).hasFieldOrProperty("localtime");
    }

}