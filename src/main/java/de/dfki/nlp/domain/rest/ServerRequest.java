package de.dfki.nlp.domain.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.dfki.nlp.domain.PredictionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import java.util.Date;
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
        List<PredictionType> types;
        Date expired;
        int communication_id;
    }

    @Data
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @RequiredArgsConstructor
    public static class Document {

        @NonNull
        String document_id;
        @NonNull
        String source;

        // we can also provide the text inline
        String abstractText;
        String title;
        String text;
    }

    public enum Method {
        getAnnotations,
        getState
    }


}
