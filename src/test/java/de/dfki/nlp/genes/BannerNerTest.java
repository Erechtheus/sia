package de.dfki.nlp.genes;

import banner.tagging.Mention;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BannerNerTest {

    @Test
    public void extractFromText() {

        BannerNer bannerNer = new BannerNer();
        String sentence = "In this abstract we describe our BRCA1 gene findings.";
        List<Mention> geneMentionStream = bannerNer.extractFromText(sentence);

        assertThat(geneMentionStream).hasSize(1);

        Mention mention = geneMentionStream.get(0);

        assertThat(mention.getStart()).isEqualTo(6);
        assertThat(mention.getEnd()).isEqualTo(8);
        assertThat(mention.getText()).isEqualTo("BRCA1 gene");
        assertThat(mention.getType().getText()).isEqualTo("GENE");

        geneMentionStream.forEach(System.out::println);

    }
}