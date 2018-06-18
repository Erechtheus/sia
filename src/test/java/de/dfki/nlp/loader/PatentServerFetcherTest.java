package de.dfki.nlp.loader;

import com.google.common.collect.Lists;
import de.dfki.nlp.config.AnnotatorConfig;
import de.dfki.nlp.config.CustomObjectMapper;
import de.dfki.nlp.config.GeneralConfig;
import de.dfki.nlp.domain.IdList;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.io.RetryHandler;
import org.junit.Ignore;
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
public class PatentServerFetcherTest {

    @Autowired
    AnnotatorConfig annotatorConfig;

    @Autowired
    RestTemplate restTemplate;

    @Test
    @Ignore("The patent server has been shutdown")
    public void load() throws Exception {


        PatentServerFetcher patentServerFetcher = new PatentServerFetcher(annotatorConfig, new RetryHandler(restTemplate));

        ArrayList<String> idList = Lists.newArrayList(
                "US20080038365", "WO2010032704A1", "WO2012005339A1"
        );

        List<ParsedInputText> load = patentServerFetcher.load(new IdList("patent", idList));

        assertThat(load).hasSize(3).extracting("externalId").containsOnlyElementsOf(idList);

        assertThat(load).extracting("text").containsNull();
        assertThat(load).extracting("title").containsExactlyInAnyOrder(
                "Prophylactic or therapeutic agent for diabetes",
                "Pharmaceutical preparation comprising micro-rna-143 derivative",
                "TRPC6 involved in glomerulonephritis"
                );
    }

}