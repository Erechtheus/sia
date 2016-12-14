package de.dfki.nlp.domain.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerRequest {

    @NotEmpty
    String name;
    Method method;

    @NotEmpty
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
