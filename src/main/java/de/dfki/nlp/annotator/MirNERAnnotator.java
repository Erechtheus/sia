package de.dfki.nlp.annotator;

import de.dfki.nlp.MirNer;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.PredictionResult;
import de.dfki.nlp.domain.PredictionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.Transformer;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.dfki.nlp.domain.PredictionResult.Section.T;

@Component
@Slf4j
@Profile("backend")
public class MirNERAnnotator implements Annotator {

    private static final MirNer mirNer = new MirNer();


    @Override
    @Transformer(inputChannel = "mirner", outputChannel = "parsed")
    public Set<PredictionResult> annotate(ParsedInputText payload) {
        if (payload.getExternalId() == null) return Collections.emptySet();

        Set<PredictionResult> results = new HashSet<>();

        log.trace("Parsing {}", payload.getExternalId());

        // iterate over the text sections
        for (PredictionResult.Section section : PredictionResult.Section.values()) {

            String analyzetext = section == T ? payload.getTitle() : payload.getAbstractText();
            if (analyzetext == null) continue;

            results.addAll(detectmirNer(analyzetext, section, payload.getExternalId()).collect(Collectors.toList()));

        }

        log.trace("Done parsing {}", payload.getExternalId());

        return results;

    }

    private Stream<PredictionResult> detectmirNer(String text, PredictionResult.Section section, String externalID) {
        return mirNer.extractFromText(text).stream().map(l -> new PredictionResult(externalID, section, l.getStart(), l.getEnd(), 1.0, l.getText(), PredictionType.MIRNA));
    }
}
