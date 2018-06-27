package de.dfki.nlp.annotator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.dfki.nlp.config.MessagingConfig;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.PredictionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.Transformer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
@Slf4j
@Profile("backend")
@ConditionalOnProperty(prefix = "sia.annotators", name = "chemspot")
public class ChemSpotAnnotator implements Annotator {

    @Autowired
    MessagingConfig.ChemSpotGateway chemSpotGateway;

    @Autowired
    ObjectMapper objectMapper;

    @Override
    @Transformer(inputChannel = "chemspot")
    public Set<PredictionResult> annotate(ParsedInputText payload) {

        String result = chemSpotGateway.sendForProcessing(payload);
        try {
            return objectMapper.readValue(result, new TypeReference<Set<PredictionResult>>() {
            });
        } catch (IOException e) {
            log.error("Can't read response from Chemspot Tagger", e);
            return null;
        }
    }
}
