package de.dfki.nlp.rest;

import com.rabbitmq.client.ConnectionFactory;
import de.dfki.nlp.SiaMainApplication;
import de.dfki.nlp.config.MessagingConfig;
import de.dfki.nlp.domain.exceptions.Errors;
import de.dfki.nlp.domain.rest.Response;
import de.dfki.nlp.domain.rest.ServerRequest;
import de.dfki.nlp.domain.rest.ServerResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = SiaMainApplication.class)
public class RestEndpointTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    MessagingConfig.ProcessingGateway processingGateway;

    @MockBean
    ConnectionFactory connectionFactory;

    @Test
    public void getErrorAnnotations() throws Exception {

        ServerRequest serverRequest = new ServerRequest();
        serverRequest.setMethod(ServerRequest.Method.getAnnotations);

        ResponseEntity<ServerResponse> response = restTemplate.postForEntity("/call", serverRequest, ServerResponse.class);

        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getErrorCode()).isEqualTo(Errors.FORMAT_ERROR.errorCode);

    }

    @Test
    public void getErrorEmptyDocuments() throws Exception {

        ServerRequest serverRequest = new ServerRequest();
        serverRequest.setMethod(ServerRequest.Method.getAnnotations);

        // test empty documents
        serverRequest.setBecalm_key("boguskey");
        serverRequest.setName("BeCalm");
        serverRequest.setParameters(new ServerRequest.Documents());

        ResponseEntity<ServerResponse> response = restTemplate.postForEntity("/call", serverRequest, ServerResponse.class);

        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getErrorCode()).isEqualTo(Errors.NEED_PARAMETERS.errorCode);

    }

    @Test
    public void getGetState() throws Exception {

        ServerRequest serverRequest = new ServerRequest();
        serverRequest.setMethod(ServerRequest.Method.getState);

        ResponseEntity<ServerResponse> response = restTemplate.postForEntity("/call", serverRequest, ServerResponse.class);

        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getErrorCode()).isEqualTo(Errors.FORMAT_ERROR.errorCode);
        assertThat(response.getBody().getMessage()).isNotBlank();

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

        assertThat(response.getBody().getData()).isInstanceOf(Map.class);
        //noinspection unchecked
        assertThat((Map<String, ?>) response.getBody().getData())
                .hasSize(4)
                .containsKey("max_analyzable_documents")
                .containsKey("state")
                .containsKey("version")
                .containsKey("version_changes");
    }

}