package de.dfki.nlp.flow;

import de.dfki.nlp.MirNer;
import de.dfki.nlp.diseasener.DiseasesNer;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.PredictionResult;
import de.dfki.nlp.domain.PredictionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.handler.annotation.Header;
import seth.SETH;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.dfki.nlp.domain.PredictionResult.Section.T;
import static de.dfki.nlp.domain.PredictionType.MUTATION;

/**
 * Class that handles all necessary annotations.
 */
@Slf4j
public class Annotator {

    // give each thread a new instance - this might not be needed
    //private static final ThreadLocal<SETH> SETH_THREAD_LOCAL = ThreadLocal.withInitial(() -> new SETH("resources/mutations.txt", true, true, false));
    private static final SETH SETH_DETECTOR = new SETH("resources/mutations.txt", true, true, false);
    private static final MirNer mirNer = new MirNer();
    private static final DiseasesNer diseaseNer = new DiseasesNer();


    @Transformer
    public Set<PredictionResult> performAnnotation(ParsedInputText payload, @Header("types") List<PredictionType> types) {

        if (payload.getExternalId() == null) return Collections.emptySet();

        Set<PredictionResult> results = new HashSet<>();

        log.trace("Parsing {}", payload.getExternalId());

        // iterate over the text sections
        for (PredictionResult.Section section : PredictionResult.Section.values()) {

            String analyzetext = section == T ? payload.getTitle() : payload.getAbstractText();
            if (analyzetext == null) continue;

            for (PredictionType predictionType : types) {
                switch (predictionType) {
                    case MIRNA:
                        results.addAll(detectmirNer(analyzetext, section, payload.getExternalId()).collect(Collectors.toSet()));
                        break;
                    case MUTATION:
                        results.addAll(detectSETH(analyzetext, section, payload.getExternalId()).collect(Collectors.toSet()));
                        break;
                    case DISEASE:
                        results.addAll(detectDisease(analyzetext, section, payload.getExternalId()).collect(Collectors.toSet()));
                        break;
                }
            }
        }

        log.trace("Done parsing {}", payload.getExternalId());

        return results;

    }

    private Stream<PredictionResult> detectSETH(String text, PredictionResult.Section section, String externalID) {
        return SETH_DETECTOR.findMutations(text).stream().map(l -> new PredictionResult(externalID, section, l.getStart(), l.getEnd(), 1.0, l.getText(), MUTATION));
    }

    private Stream<PredictionResult> detectmirNer(String text, PredictionResult.Section section, String externalID) {
        return mirNer.extractFromText(text).stream().map(l -> new PredictionResult(externalID, section, l.getStart(), l.getEnd(), 1.0, l.getText(), PredictionType.MIRNA));
    }

    private Stream<PredictionResult> detectDisease(String text, PredictionResult.Section section, String externalID) {
        return diseaseNer.extractFromText(text).map(l -> new PredictionResult(externalID, section, l.getStart(), l.getEnd(), 1.0, l.getText(), PredictionType.DISEASE));
    }


}
