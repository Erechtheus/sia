package de.dfki.nlp.domain.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;

import java.util.Map;

@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerResponse {

    int status;
    boolean success = false;

    String becalmKey;

    String message;
    String errorCode;
    String errorName;

    Map<String, String> urlParams;

    public ServerResponse(int status, boolean success, String becalmKey, String message, String errorCode) {
        this.status = status;
        this.success = success;
        this.becalmKey = becalmKey;
        this.message = message;
        this.errorCode = errorCode;
    }
}