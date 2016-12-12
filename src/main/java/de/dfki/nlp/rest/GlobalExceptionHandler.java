package de.dfki.nlp.rest;

import de.dfki.nlp.domain.rest.ErrorResponse;
import de.dfki.nlp.rest.exceptions.UnsupportedMethodException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@ControllerAdvice
@RestController
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    public ErrorResponse handleException(Exception e) {
        log.error("Error",e);
        return new ErrorResponse(400, false, "", e.getMessage(), "1");
    }

    @ExceptionHandler(value = UnsupportedMethodException.class)
    public ErrorResponse handleunsupported(Exception e) {
        log.error("Error",e);
        return new ErrorResponse(400, false, "", "Message not supported", "1");
    }


}  