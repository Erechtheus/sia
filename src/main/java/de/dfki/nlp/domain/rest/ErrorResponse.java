package de.dfki.nlp.domain.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    int status;
    boolean success = false;

    String key;

    String message;
    String errorCode;

}