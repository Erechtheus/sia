package de.dfki.nlp.rest.exceptions;

import lombok.Data;

@Data
public abstract class BaseException extends Exception {

    String errorCode;

    public BaseException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
