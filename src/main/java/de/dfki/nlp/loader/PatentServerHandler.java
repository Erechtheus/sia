package de.dfki.nlp.loader;

import de.dfki.nlp.config.AnnotatorConfig;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.rest.ServerRequest;
import de.dfki.nlp.io.RetryHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class PatentServerHandler extends AbstractDocumentFetcher {

    private final AnnotatorConfig annotatorConfig;
    private final RetryHandler retryHandler;

    public PatentServerHandler(AnnotatorConfig annotatorConfig, RetryHandler retryHandler) {
        this.annotatorConfig = annotatorConfig;
        this.retryHandler = retryHandler;
    }

    @Override
    List<ParsedInputText> load(List<ServerRequest.Document> document) {

        try {

            List<String> ids = document.stream().map(ServerRequest.Document::getDocument_id).collect(Collectors.toList());

            ParsedInputText[] parsedInputText = retryHandler.retryablePost(annotatorConfig.patent.url, new MultiRequest(ids), ParsedInputText[].class);

            return Arrays.stream(parsedInputText).collect(Collectors.toList());

        } catch (RestClientException e) {
            log.error("Error retrieving abstracts {}", document, e);
        }

        return null;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class MultiRequest {
        List<String> patents;
    }

}
