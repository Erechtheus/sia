package de.dfki.nlp.diseasener;

import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


public class DiseasesNerTest {


    @Test
    public void extractFromText() throws Exception {


        DiseasesNer diseasesNer = new DiseasesNer();


        List<DiseaseMention> diseaseMention = diseasesNer.extractFromText("Familial hypercholesterolemia kindred in Utah with novel C54S mutations of the LDL receptor gene.").collect(Collectors.toList());
        assertThat(diseaseMention).hasSize(1).contains(new DiseaseMention(0,28,"Familial hypercholesterolemia"));


        diseaseMention = diseasesNer.extractFromText("Intravenous contrast medium aggravates the impairment of pancreatic microcirculation in necrotizing pancreatitis in the rat.").collect(Collectors.toList());
        assertThat(diseaseMention).hasSize(1).contains(new DiseaseMention(88,111,"necrotizing pancreatitis"));

    }

}