package de.dfki.nlp.loader;

import com.google.common.collect.Lists;
import de.dfki.nlp.config.AnnotatorConfig;
import de.dfki.nlp.config.CustomObjectMapper;
import de.dfki.nlp.config.GeneralConfig;
import de.dfki.nlp.domain.IdList;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.io.RetryHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = {GeneralConfig.class, CustomObjectMapper.class})
@EnableConfigurationProperties(AnnotatorConfig.class)
public class PubMedDocumentFetcherTest {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    AnnotatorConfig annotatorConfig;


    @Test
    public void load() throws Exception {

        AnnotatorConfig annotatorConfig = new AnnotatorConfig();

        AnnotatorConfig.Def pubmed = new AnnotatorConfig.Def();
        pubmed.setUrl("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id={id}&retmode=xml");
        annotatorConfig.setPubmed(pubmed);

        PubMedDocumentFetcher pubMedDocumentFetcher = new PubMedDocumentFetcher(annotatorConfig, new RetryHandler(restTemplate));

        ArrayList<String> idList = Lists.newArrayList(
                "22835028", "22290653"
        );

        List<ParsedInputText> load = pubMedDocumentFetcher.load(IdList.withIds("pubmed", idList));

        assertThat(load).hasSize(2).extracting("externalId").containsExactlyElementsOf(idList);

        load.forEach(System.out::println);


    }

}