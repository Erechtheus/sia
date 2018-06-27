package de.dfki.nlp.annotator;

import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.PredictionResult;
import de.dfki.nlp.domain.PredictionType;
import de.dfki.nlp.linnaeus.LinnaeusTagger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.Transformer;
import org.springframework.stereotype.Component;
import uk.ac.man.entitytagger.Mention;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.dfki.nlp.domain.PredictionResult.Section.T;

@Component
@Slf4j
@Profile("backend")
@ConditionalOnProperty(prefix = "sia.annotators", name = "linnaeus")
public class LinnaeusTaggerAnnotator implements Annotator {

    private static final LinnaeusTagger linnaeusTagger = new LinnaeusTagger();

    @Override
    @Transformer(inputChannel = "linnaeus")
    public Set<PredictionResult> annotate(ParsedInputText payload) {
        if (payload.getExternalId() == null) return Collections.emptySet();

        Set<PredictionResult> results = new HashSet<>();

        log.trace("Parsing {}", payload.getExternalId());

        // iterate over the text sections
        for (PredictionResult.Section section : PredictionResult.Section.values()) {

            String analyzetext = section == T ? payload.getTitle() : payload.getAbstractText();
            if (analyzetext == null) continue;

            results.addAll(detectLinnaeusTagger(analyzetext, section, payload.getExternalId()).collect(Collectors.toList()));

        }


        log.trace("Done parsing {}", payload.getExternalId());

        return results;
    }

    private Stream<PredictionResult> detectLinnaeusTagger(String analyzetext, PredictionResult.Section section, String externalId) {
        return linnaeusTagger.match(analyzetext).stream().map(l -> transform(externalId, section, l));
    }

    private PredictionResult transform(String externalId, PredictionResult.Section section, Mention mention) {
        PredictionResult predictionResult = new PredictionResult(externalId, section, mention.getStart(), mention.getEnd(), 1.0, mention.getText(), PredictionType.ORGANISM);
        Double[] probabilities = mention.getProbabilities();
        if (probabilities  != null && probabilities.length > 0) {
            predictionResult.setScore(probabilities[0]);
        }
        predictionResult.setDatabaseId(mention.getMostProbableID());
        return predictionResult;
    }
}
