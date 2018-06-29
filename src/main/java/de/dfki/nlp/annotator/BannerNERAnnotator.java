package de.dfki.nlp.annotator;

import static de.dfki.nlp.domain.PredictionResult.Section.T;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.Transformer;
import org.springframework.stereotype.Component;

import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.PredictionResult;
import de.dfki.nlp.domain.PredictionType;
import de.dfki.nlp.genes.BannerNer;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Profile("backend")
@ConditionalOnProperty(prefix = "sia.annotators", name = "banner")
public class BannerNERAnnotator implements Annotator {

    private static final BannerNer bannerNERAnnotator = new BannerNer();

    @Override
    @Transformer(inputChannel = "banner")
    public Set<PredictionResult> annotate(ParsedInputText payload) {

        if (payload.getExternalId() == null) return Collections.emptySet();

        Set<PredictionResult> results = new HashSet<>();

        log.trace("Parsing {}", payload.getExternalId());


        // iterate over the text sections
        for (PredictionResult.Section section : PredictionResult.Section.values()) {

            String analyzetext = section == T ? payload.getTitle() : payload.getAbstractText();
            if (analyzetext == null) continue;

            results.addAll(detectBanner(analyzetext, section, payload.getExternalId()).collect(Collectors.toList()));

        }

        log.trace("Done parsing {}", payload.getExternalId());

        return results;

    }

    // Banner is not thread safe
    private synchronized Stream<PredictionResult> detectBanner(String text, PredictionResult.Section section, String externalID) {
        return bannerNERAnnotator.extractFromText(text).stream().map(m -> new PredictionResult(externalID, section, m.getStartChar(), m.getEndChar(), 1.0, m.getText(), PredictionType.GENE));
    }
}
