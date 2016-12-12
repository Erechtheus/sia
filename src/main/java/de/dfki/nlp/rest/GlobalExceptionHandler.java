package de.dfki.nlp.rest;

import de.dfki.nlp.domain.rest.ErrorResponse;
import de.dfki.nlp.rest.exceptions.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.HttpRequestMethodNotSupportedException;
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

        String errorCode = "1";
        if(e instanceof HttpRequestMethodNotSupportedException) {
            errorCode = "2";
        }

        return new ErrorResponse(400, false, "", e.getMessage(), errorCode);
    }

    @ExceptionHandler(value = BaseException.class)
    public ErrorResponse handleCustom(BaseException e) {
        log.error("Error",e);
        return new ErrorResponse(400, false, "", e.getMessage(), e.getErrorCode());
    }


}  