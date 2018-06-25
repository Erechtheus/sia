package de.dfki.nlp.annotator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.dfki.nlp.config.MessagingConfig;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.PredictionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.Transformer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
@Slf4j
@Profile("backend")
public class DNormAnnotator implements Annotator {

    @Autowired
    MessagingConfig.DNormGateway dNormGateway;

    @Autowired
    ObjectMapper objectMapper;

    @Override
    @Transformer(inputChannel = "dnorm", outputChannel = "parsed")
    public Set<PredictionResult> annotate(ParsedInputText payload) {

        String result = dNormGateway.sendForProcessing(payload);
        try {
            return objectMapper.readValue(result, new TypeReference<Set<PredictionResult>>() {
            });
        } catch (IOException e) {
            log.error("Can't read response from Dnorm Tagger", e);
            return null;
        }
    }
}
