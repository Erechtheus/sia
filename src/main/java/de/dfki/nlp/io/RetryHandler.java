package de.dfki.nlp.io;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Simple Wrapper around RestTemplate, which retries to download documents, in case of failures.
 */
@Component
@Slf4j
public class RetryHandler {

    private final RestTemplate restTemplate;

    RetryHandler(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retryable(value = ResourceAccessException.class, maxAttempts = 10, backoff = @Backoff(value = 1000, multiplier = 2, maxDelay = 60000))
    public <T> T retryableGet(String urlpattern, Class<T> responseType, Object... uriVariables) {

        try {
            return restTemplate.getForObject(urlpattern, responseType, uriVariables);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() != HttpStatus.NOT_FOUND) {
                // retry
                throw new ResourceAccessException("Client Error " + ex.getMessage());
            } else {
                log.error("Error '{}' downloading document id {} from {}", ex.getMessage(), uriVariables, urlpattern);
            }
        }

        return BeanUtils.instantiate(responseType);

    }

    // handle 404

    @Recover
    public <T> T recover(ResourceAccessException e, String urlpattern, Class<T> responseType, Object... uriVariables) throws IllegalAccessException, InstantiationException {
        log.error("Failed to download after 10 tries {} ... continuing", e.getMessage());
        return responseType.newInstance();
    }

}