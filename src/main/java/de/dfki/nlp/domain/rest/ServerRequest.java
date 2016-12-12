package de.dfki.nlp.domain.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class ServerRequest {

    String name;
    Method method;
    String becalm_key;
    // custom_parameters
    Documents parameters;

    @Data
    @ToString
    public static class Documents {
        List<Document> documents;
        List<String> types;
        String expired;
        int communication_id;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Document {
        String document_id;
        String source;
    }

    public enum Method {
        getAnnotations,
        getState
    }


}
