package de.dfki.nlp.domain.exceptions;

public class UnsupportedMethodException extends BaseException {

    public UnsupportedMethodException(String message) {
        // INCORRECT_TRANSFER_REQUEST_METHOD
        super(message, "2");
    }
}
