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
public class PMCDocumentFetcherTest {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    AnnotatorConfig annotatorConfig;


    @Test
    public void load() throws Exception {

        ArrayList<String> idList = Lists.newArrayList(
                "BC1403855C", "BC1403855C", "PMC1403923", "26837", "PMC20255"
        );

        PMCDocumentFetcher pmcDocumentFetcher = new PMCDocumentFetcher(annotatorConfig, new RetryHandler(restTemplate));

        List<ParsedInputText> load = pmcDocumentFetcher.load(IdList.withIds("pmc", idList));

        assertThat(load).hasSize(idList.size()).extracting("externalId").containsExactlyElementsOf(idList);

        load.forEach(System.out::println);


    }

}