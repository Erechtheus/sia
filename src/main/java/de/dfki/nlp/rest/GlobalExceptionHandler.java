package de.dfki.nlp.rest;

import com.google.common.base.Joiner;
import de.dfki.nlp.config.AnnotatorConfig;
import de.dfki.nlp.domain.exceptions.BaseException;
import de.dfki.nlp.domain.rest.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@ControllerAdvice
@RestController
@Slf4j
public class GlobalExceptionHandler {


    private final AnnotatorConfig annotatorConfig;

    public GlobalExceptionHandler(AnnotatorConfig annotatorConfig) {
        this.annotatorConfig = annotatorConfig;
    }

    @ExceptionHandler(value = Exception.class)
    public ErrorResponse handleException(Exception e) {
        log.error("Error", e);

        String errorCode = "1";
        if (e instanceof HttpRequestMethodNotSupportedException) {
            errorCode = "2";
        }

        return new ErrorResponse(400, false, annotatorConfig.apiKey, e.getMessage(), errorCode);
    }

    @ExceptionHandler(value = BaseException.class)
    public ErrorResponse handleCustom(BaseException e) {
        log.error("Error", e);
        return new ErrorResponse(400, false, annotatorConfig.apiKey, e.getMessage(), e.getErrorCode());
    }

    @ExceptionHandler(value = HttpMessageConversionException.class)
    public ErrorResponse handleHTTPError(HttpMessageConversionException e) {
        log.error("Error", e);
        return new ErrorResponse(400, false, annotatorConfig.apiKey, e.getMessage(), "1");
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ErrorResponse handleValidationError(MethodArgumentNotValidException e) {
        log.error("Error", e);

        String message = e.getBindingResult().getAllErrors().stream()
                .map(validationError -> {
                    if (validationError instanceof FieldError) {
                        FieldError fieldError = (FieldError) validationError;
                        return Joiner.on(" ").join(fieldError.getField(), fieldError.getDefaultMessage());
                    }
                    return "Error for " + validationError.getObjectName();
                }).collect(Collectors.joining(". "));
        return new ErrorResponse(400, false, annotatorConfig.apiKey, message, "1");
    }

}  