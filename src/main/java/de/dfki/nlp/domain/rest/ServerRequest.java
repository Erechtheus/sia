package de.dfki.nlp.domain.rest;

import lombok.Data;

import java.util.List;

@Data
public class ServerRequest {

    String name;
    Method method;
    String becalm_key;
    // custom_parameters
    Documents parameters;

    @Data
    private static class Documents {
        List<Document> documents;
        List<String> types;
        String expired;
        int communication_id;
    }

    @Data
    private static class Document {
        String document_id;
        String source;
    }

    public enum Method {
        getAnnotations,
        getState
    }


}
