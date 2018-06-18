package de.dfki.nlp.io;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.concurrent.TimeUnit;

/**
 * Simple Wrapper around RestTemplate, which retries to download documents, in case of failures.
 */
@Component
@Slf4j
public class RetryHandler {

    private final RestTemplate restTemplate;

    public RetryHandler(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        restTemplate.getInterceptors().add(new PerfRequestSyncInterceptor());
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

    @Retryable(value = ResourceAccessException.class, maxAttempts = 10, backoff = @Backoff(value = 1000, multiplier = 2, maxDelay = 60000))
    public <T> T retryablePost(String urlpattern, Object request, Class<T> responseType, Object... uriVariables) {

        try {

            return restTemplate.postForObject(urlpattern, request, responseType, uriVariables);

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() != HttpStatus.NOT_FOUND) {
                // retry
                throw new ResourceAccessException("Client Error " + ex.getMessage());
            } else {
                log.error("Error '{}' downloading documents {} from {}", ex.getMessage(), request, urlpattern);
            }
        }

        // return empty
        return null;

    }

    // handle 404's ...
    @Recover
    public <T> T connectionException(RestClientException e, String urlpattern, Object request, Class<T> responseType, Object... uriVariables) {
        log.error("Failed to download after 10 tries {} ... continuing", e.getMessage());
        try {
            if (responseType.isArray()) {
                return (T) Array.newInstance(responseType.getComponentType(), 0);
            } else {
                return responseType.newInstance();
            }
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    /**
     * Simple class that logs the time taken for a request.
     */
    @Slf4j
    public static class PerfRequestSyncInterceptor implements ClientHttpRequestInterceptor {

        @Override
        public ClientHttpResponse intercept(HttpRequest hr, byte[] bytes, ClientHttpRequestExecution chre) throws IOException {
            Stopwatch stopwatch = Stopwatch.createStarted();
            ClientHttpResponse response = chre.execute(hr, bytes);
            stopwatch.stop();

            log.info("corpus adapter performance: method={}, response_time={}, response_code={}, uri={}",
                    hr.getMethod(), stopwatch.elapsed(TimeUnit.MILLISECONDS), response.getStatusCode().value(), StringUtils.abbreviate(hr.getURI().toString(), 70));

            return response;
        }
    }

}