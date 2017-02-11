package de.dfki.nlp.domain.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {ObjectMapper.class})
public class ServerResponseTest {

    @Autowired
    private JacksonTester<ServerResponse> json;

    @Test
    public void testDeserialize() throws Exception {
        String content = "{\"status\":400,\"success\":false,\"message\":\"All documents have already been analysed for this request.Please, revise the number of  responses that your annotation server sends to the BeCalm metaserver. Only one response it is allow ed per request\",\"errorName\":\"REQUEST_CLOSED\",\"errorCode\":\"13\",\"becalmKey\":\"c71715154f4a7e5c77930a7994c013527f1218cb\",\"urlParams\":{\"apikey\":\"0a0dd3959e54f085dec2a3036feeeacb6aa01d41\",\"communicationId\": \"303074\"}}";
        assertThat(this.json.parseObject(content).getMessage()).startsWith("All do");
        assertThat(this.json.parseObject(content).getStatus()).isEqualTo(400);
        assertThat(this.json.parseObject(content).isSuccess()).isFalse();

        assertThat(this.json.parseObject(content).getBecalmKey()).isEqualTo("c71715154f4a7e5c77930a7994c013527f1218cb");

        assertThat(this.json.parseObject(content).getUrlParams())
                .hasSize(2)
                .containsEntry("communicationId", "303074")
                .containsEntry("apikey", "0a0dd3959e54f085dec2a3036feeeacb6aa01d41");


    }
}