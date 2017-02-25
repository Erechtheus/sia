package de.dfki.nlp.loader;

import com.google.common.collect.Lists;
import de.dfki.nlp.config.AnnotatorConfig;
import de.dfki.nlp.config.CustomObjectMapper;
import de.dfki.nlp.config.GeneralConfig;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.rest.ServerRequest;
import de.dfki.nlp.io.RetryHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = {GeneralConfig.class, CustomObjectMapper.class})
public class PatentServerHandlerTest {

    @Autowired
    RestTemplate restTemplate;

    @Test
    public void load() throws Exception {


        AnnotatorConfig annotatorConfig = new AnnotatorConfig();

        AnnotatorConfig.Def patentserverURL = new AnnotatorConfig.Def();
        patentserverURL.setUrl("http://193.147.85.10:8087/patentserver/json");
        annotatorConfig.setPatent(patentserverURL);

        PatentServerHandler patentServerHandler = new PatentServerHandler(annotatorConfig, new RetryHandler(restTemplate));

        ArrayList<String> idList = Lists.newArrayList(
                "US20080038365", "WO2010032704A1", "WO2012005339A1"
        );

        List<ServerRequest.Document> documentList = idList.stream().map(m -> new ServerRequest.Document(m, "a")).collect(Collectors.toList());

        List<ParsedInputText> load = patentServerHandler.load(documentList);

        assertThat(load).hasSize(3).extracting("externalId").containsOnlyElementsOf(idList);

        assertThat(load).extracting("text").containsNull();
        assertThat(load).extracting("title").containsExactlyInAnyOrder(
                "Prophylactic or therapeutic agent for diabetes",
                "Pharmaceutical preparation comprising micro-rna-143 derivative",
                "TRPC6 involved in glomerulonephritis"
                );
    }

}