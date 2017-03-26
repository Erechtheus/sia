package de.dfki.nlp.annotator;

import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.PredictionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.Transformer;
import org.springframework.stereotype.Component;
import seth.SETH;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.dfki.nlp.domain.PredictionResult.Section.T;
import static de.dfki.nlp.domain.PredictionType.MUTATION;

@Slf4j
@Component
@Profile("backend")
public class SethAnnotator implements Annotator {

    private static final SETH SETH_DETECTOR = new SETH("resources/mutations.txt", true, true, false);

    @Transformer(inputChannel = "seth", outputChannel = "parsed")
    public Set<PredictionResult> annotate(ParsedInputText payload) {

        if (payload.getExternalId() == null) return Collections.emptySet();

        log.trace("Parsing {}", payload.getExternalId());

        Set<PredictionResult> results = new HashSet<>();

        // iterate over the text sections
        for (PredictionResult.Section section : PredictionResult.Section.values()) {

            String analyzetext = section == T ? payload.getTitle() : payload.getAbstractText();
            if (analyzetext == null) continue;

            results.addAll(detectSETH(analyzetext, section, payload.getExternalId()).collect(Collectors.toList()));

        }

        log.trace("Done parsing {}", payload.getExternalId());

        return results;


    }

    private Stream<PredictionResult> detectSETH(String text, PredictionResult.Section section, String externalID) {
        return SETH_DETECTOR.findMutations(text).stream().map(l -> new PredictionResult(externalID, section, l.getStart(), l.getEnd(), 1.0, l.getText(), MUTATION));
    }

}
