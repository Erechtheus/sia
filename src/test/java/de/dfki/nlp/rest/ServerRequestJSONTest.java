package de.dfki.nlp.rest;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import de.dfki.nlp.config.CustomObjectMapper;
import de.dfki.nlp.domain.rest.ServerRequest;


public class ServerRequestJSONTest {

    private final CustomObjectMapper customObjectMapper = new CustomObjectMapper();

    @Test
    public void testDeserializeBrokenRequest() throws Exception {

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

        assertThat(customObjectMapper.readValue(requestJson, ServerRequest.class)).hasFieldOrProperty("parameters");

    }

}
