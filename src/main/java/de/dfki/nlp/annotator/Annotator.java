package de.dfki.nlp.annotator;

import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.PredictionResult;

import java.util.Set;

public interface Annotator {

    Set<PredictionResult> annotate(ParsedInputText payload);

}
