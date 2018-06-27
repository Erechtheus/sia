package de.dfki.nlp.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Set;

@Data
@NoArgsConstructor
@ToString
@AllArgsConstructor
public class AnnotationResponse {

    Set<PredictionResult> predictionResults;
    long runtimeInMs;
    long parseTimeInMs;
}
