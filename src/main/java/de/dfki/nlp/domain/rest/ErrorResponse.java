package de.dfki.nlp.domain.rest;

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

    String key;

    String message;
    String errorCode;

}