package de.dfki.nlp.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import de.dfki.nlp.config.AnnotatorConfig;
import de.dfki.nlp.config.GeneralConfig;
import de.dfki.nlp.domain.IdList;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.rest.ServerRequest;
import de.dfki.nlp.io.RetryHandler;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = {GeneralConfig.class,
        MultiDocumentFetcher.class,
        ObjectMapper.class,
        RetryHandler.class,
        PubMedDocumentFetcher.class,
        AbstractServerFetcher.class,
        PMCDocumentFetcher.class,
        PatentServerFetcher.class
})
@EnableConfigurationProperties(AnnotatorConfig.class)
public class DocumentFetcherTest {

    @Autowired
    private MultiDocumentFetcher documentFetcher;

    @Test
    public void testNotImplementedLoader() throws Exception {

        ParsedInputText load = documentFetcher.load(new ServerRequest.Document("BC1403855C", "Patent Server"));

        assertThat(load.getExternalId()).isNull();
        assertThat(load.getTitle()).isNull();
        assertThat(load.getAbstractText()).isNull();

    }

    @Test
    @Ignore("The patent server has been shutdown")
    public void testPatent() throws Exception {

        ParsedInputText load = documentFetcher.load(new ServerRequest.Document("CA2073855C", "Patent Server"));

        assertThat(load.getExternalId()).isEqualTo("CA2073855C");
        assertThat(load.getTitle()).isEqualTo("Glycoalkaloids for controlling cellular autophagy");
        assertThat(load.getAbstractText()).isEqualTo("The invention is directed to the control of cellular autophagy, cellular agglutination and the immobilization of motile cells.Such control is useful in, for example, the treatment of cancer, contraception, termination of pregnancy, removal of pathogenic organisms and removal of any abnormal cellular growth (malignant or otherwise); as a diagnostic and analytical tool whereby cell structure can be studied and testing could be undertaken for the presence (and subsequent analysis) of pathogenic and non-pathogenic organisms; and in the manufacture of biochemicals whereby certain cells must be destroyed or otherwise contained.From surface analysis of normal and abnormal cells, specific receptors on abnormal cells which are either not present on normal cells or are only present insignificantly reduced numbers can be identified. Alkaloids and other pharmaceutically acceptable compounds are preferentially recognised by the abnormal cells, and which bind thereto and subsequently destroy.");

    }

    @Test
    public void testPubMed() throws Exception {
        // now test pubmed

        ParsedInputText load = documentFetcher.load(new ServerRequest.Document("BC1403854C", "pubmed"));

        assertThat(load.getExternalId()).isEqualTo("BC1403854C");
        assertThat(load.getTitle()).isEqualTo("Twelve-year clinical report on multiple endodontic implant stabilizers.");
        assertThat(load.getAbstractText()).isNull();

    }

    @Test
    public void testPMC() throws Exception {
        // now test pmc

        ParsedInputText pmc = documentFetcher.load(new ServerRequest.Document("20255", "PMC"));

        assertThat(pmc.getExternalId()).isEqualTo("20255");
        assertThat(pmc.getTitle()).isEqualTo("Mycobacterium bovis bacille Calmette–Guérin strains secreting listeriolysin of Listeria monocytogenes");
        assertThat(pmc.getAbstractText()).startsWith("Recombinant (r) Mycobacterium bovis strains were constructed that secrete biologically active listeriolysin (Hly) fusion protein of Listeria monocytogenes");

        ParsedInputText noAbstract = documentFetcher.load(new ServerRequest.Document("BC1403855C", "PMC"));

        assertThat(noAbstract.getExternalId()).isEqualTo("BC1403855C");
        assertThat(noAbstract.getTitle()).isEqualTo("A complex concurrent schedule of reinforcement1");
        assertThat(noAbstract.getAbstractText()).isNull();

        ParsedInputText noDocument = documentFetcher.load(new ServerRequest.Document("12211244", "PMC"));

        assertThat(noDocument.getExternalId()).isNull();
        assertThat(noDocument.getTitle()).isNull();
        assertThat(noDocument.getAbstractText()).isNull();

        // now check multiple
        List<String> multipleId = Lists.newArrayList("BC1403855C", "20255", "20255");
        List<ParsedInputText> multiple = documentFetcher.load(IdList.withIds("PMC", multipleId));

        assertThat(multiple).hasSize(3).extracting("externalId").containsExactlyElementsOf(multipleId);

    }
}