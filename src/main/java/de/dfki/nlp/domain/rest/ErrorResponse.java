package de.dfki.nlp.domain.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ErrorResponse {

    int status;
    boolean success = false;

    @JsonProperty("becalm_key")
    String key;

    String message;
    String errorCode;

}