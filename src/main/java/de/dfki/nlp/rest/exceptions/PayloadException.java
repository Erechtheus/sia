package de.dfki.nlp.rest.exceptions;

public class PayloadException extends BaseException {

    public PayloadException(String message) {
        // FORMAT_ERROR
        super(message, "1");
    }
}
