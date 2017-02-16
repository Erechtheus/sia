package de.dfki.nlp.flow;

import de.dfki.nlp.annotator.DiseasesNERAnnotator;
import de.dfki.nlp.annotator.MirNERAnnotator;
import de.dfki.nlp.annotator.SethAnnotator;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.PredictionResult;
import de.dfki.nlp.domain.PredictionResult.Section;
import de.dfki.nlp.domain.PredictionType;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


public class AnnotatorTest {

    private static final String EXTERNAL_ID = "1-1-2";

    SethAnnotator sethAnnotator = new SethAnnotator();
    DiseasesNERAnnotator diseasesNERAnnotator = new DiseasesNERAnnotator();
    MirNERAnnotator mirNERAnnotator = new MirNERAnnotator();

    private ParsedInputText payload;

    @Before
    public void setUp() throws Exception {
        payload = new ParsedInputText(EXTERNAL_ID, "Necrotizing pancreatitis", "necrotizing pancreatitis Leu466Arg miR-199b*", "");
    }


    @Test
    public void performAnnotationWithDisease() throws Exception {

        Set<PredictionResult> predictionResults = diseasesNERAnnotator.annotate(payload);
        assertThat(predictionResults).hasSize(2).extracting("documentId").containsExactlyInAnyOrder(EXTERNAL_ID, EXTERNAL_ID);
        assertThat(predictionResults).extracting("documentId").containsExactlyInAnyOrder(EXTERNAL_ID, EXTERNAL_ID);
        assertThat(predictionResults).extracting("section").containsExactlyInAnyOrder(Section.A, Section.T);
        assertThat(predictionResults).extracting("annotatedText").contains("Necrotizing pancreatitis");

    }


    @Test
    public void performAnnotationWithSETH() throws Exception {

        Set<PredictionResult> predictionResults = sethAnnotator.annotate(payload);
        assertThat(predictionResults).hasSize(1).extracting("documentId").contains(EXTERNAL_ID);
        assertThat(predictionResults).extracting("section").containsExactlyInAnyOrder(Section.A);
        assertThat(predictionResults).extracting("annotatedText").contains("Leu466Arg");

    }

    @Test
    public void performAnnotationWithMirNer() throws Exception {

        Set<PredictionResult> predictionResults = mirNERAnnotator.annotate(payload);
        assertThat(predictionResults).hasSize(1).extracting("documentId").contains(EXTERNAL_ID);
        assertThat(predictionResults).extracting("section").containsExactlyInAnyOrder(Section.A);
        assertThat(predictionResults).extracting("annotatedText").contains("miR-199b*");

        assertThat(predictionResults).contains(new PredictionResult(EXTERNAL_ID, Section.A, 35, 44, 1.0, "miR-199b*", PredictionType.MIRNA));

    }


}