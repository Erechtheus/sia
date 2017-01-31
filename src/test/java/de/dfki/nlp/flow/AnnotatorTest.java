package de.dfki.nlp.flow;

import com.google.common.collect.Lists;
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

    private Annotator annotator = new Annotator();
    private ParsedInputText payload;

    @Before
    public void setUp() throws Exception {
        payload = new ParsedInputText(EXTERNAL_ID, "Necrotizing pancreatitis", "necrotizing pancreatitis Leu466Arg miR-199b*", "");
    }


    @Test
    public void performAnnotationWithNoTypes() throws Exception {

        Set<PredictionResult> predictionResults = annotator.performAnnotation(payload, Lists.newArrayList());
        assertThat(predictionResults).hasSize(0);

    }

    @Test
    public void performAnnotationWithDisease() throws Exception {

        Set<PredictionResult> predictionResults = annotator.performAnnotation(payload, Lists.newArrayList(PredictionType.DISEASE));
        assertThat(predictionResults).hasSize(2).extracting("documentId").containsExactlyInAnyOrder(EXTERNAL_ID, EXTERNAL_ID);
        assertThat(predictionResults).extracting("documentId").containsExactlyInAnyOrder(EXTERNAL_ID, EXTERNAL_ID);
        assertThat(predictionResults).extracting("section").containsExactlyInAnyOrder(Section.A, Section.T);
        assertThat(predictionResults).extracting("annotatedText").contains("Necrotizing pancreatitis");

    }


    @Test
    public void performAnnotationWithSETH() throws Exception {

        Set<PredictionResult> predictionResults = annotator.performAnnotation(payload, Lists.newArrayList(PredictionType.MUTATION));
        assertThat(predictionResults).hasSize(1).extracting("documentId").contains(EXTERNAL_ID);
        assertThat(predictionResults).extracting("section").containsExactlyInAnyOrder(Section.A);
        assertThat(predictionResults).extracting("annotatedText").contains("Leu466Arg");

    }

    @Test
    public void performAnnotationWithMirNer() throws Exception {

        Set<PredictionResult> predictionResults = annotator.performAnnotation(payload, Lists.newArrayList(PredictionType.MIRNA));
        assertThat(predictionResults).hasSize(1).extracting("documentId").contains(EXTERNAL_ID);
        assertThat(predictionResults).extracting("section").containsExactlyInAnyOrder(Section.A);
        assertThat(predictionResults).extracting("annotatedText").contains("miR-199b*");

        assertThat(predictionResults).contains(new PredictionResult(EXTERNAL_ID, Section.A, 35,44,1.0,"miR-199b*", PredictionType.MIRNA));

    }


}