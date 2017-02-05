package de.dfki.nlp.loader;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Simple Wrapper around RestTemplate, which retries to download documents, in case of failures.
 */
@Component
public class RetryHandler {

    private final RestTemplate restTemplate;

    RetryHandler(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retryable(value = ResourceAccessException.class, maxAttempts = 10, backoff = @Backoff(multiplier = 2, maxDelay = 30000))
    public <T> T retryableGet(String urlpattern, Class<T> responseType, Object... uriVariables) {
        return restTemplate.getForObject(urlpattern, responseType, uriVariables);
    }

}