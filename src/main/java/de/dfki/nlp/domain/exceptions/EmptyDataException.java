package de.dfki.nlp.domain.exceptions;

public class EmptyDataException extends BaseException {

    public EmptyDataException() {
        super(Errors.EMPTY_DATA);
    }
}
