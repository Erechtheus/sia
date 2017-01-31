package de.dfki.nlp.domain;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class PredictionResultTest {

    @Test
    public void testEquals() throws Exception {

        // test hashcode and equals
        PredictionResult predictionResult1 = new PredictionResult("1", PredictionResult.Section.A, 0,1,1.,"", PredictionType.MUTATION, null, null);
        PredictionResult predictionResult2 = new PredictionResult("1", PredictionResult.Section.A, 0,1,1.,"", PredictionType.MUTATION, null, null);

        assertThat(predictionResult2).isEqualTo(predictionResult1);

        // change something
        predictionResult2.setSection(PredictionResult.Section.T);
        assertThat(predictionResult2).isNotEqualTo(predictionResult1);

    }
}