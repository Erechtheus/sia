package de.dfki.nlp.loader;

import com.google.common.collect.Lists;
import de.dfki.nlp.config.AnnotatorConfig;
import de.dfki.nlp.config.CustomObjectMapper;
import de.dfki.nlp.config.GeneralConfig;
import de.dfki.nlp.domain.IdList;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.rest.ServerRequest;
import de.dfki.nlp.io.RetryHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = {GeneralConfig.class, CustomObjectMapper.class})
@EnableConfigurationProperties(AnnotatorConfig.class)
public class AbstractServerFetcherTest {

    @Autowired
    AnnotatorConfig annotatorConfig;

    @Autowired
    RestTemplate restTemplate;

    private AbstractServerFetcher abstractServerHandler;

    @Before
    public void setUp() throws Exception {

        abstractServerHandler = new AbstractServerFetcher(annotatorConfig, new RetryHandler(restTemplate));

    }

    @Test
    public void load() throws Exception {



        ArrayList<String> idList = Lists.newArrayList(
                "24218123", "10022392", "22296369", "21947881", "22277050",
                "23583468", "23933271", "20023238", "21861709", "23721855",
                "17924846", "15547674", "24418399", "20668462", "24078092",
                "26429324", "26770981", "19766904", "18279705");

        ServerRequest.Document document = new ServerRequest.Document("10022392", "abstractText");
        ParsedInputText abstractText = abstractServerHandler.load(document);
        assertThat(abstractText).extracting("externalId", "title").containsExactly("10022392", "Prenatal diagnosis of thyroid hormone resistance.");


        StopWatch stopWatch = new StopWatch();
        for (int i = 1; i <= idList.size(); i++) {

            IdList idListQuery = new IdList("abstract", idList.subList(0, i));

            // load multiple
            stopWatch.start("" + idListQuery.getIds().size());
            List<ParsedInputText> load = abstractServerHandler.load(idListQuery);
            stopWatch.stop();
            assertThat(load).hasSize(i);

        }

        System.out.printf(Locale.ENGLISH, "%s\t%s\t %n", "Task", "ms");
        Arrays.stream(stopWatch.getTaskInfo()).forEach(task -> {
            System.out.printf(Locale.ENGLISH, "%s\t%d\t %n", task.getTaskName(), task.getTimeMillis());
        });


    }

    @Test
    public void loadSingle() throws Exception {


        ParsedInputText abstractText = abstractServerHandler.load(new ServerRequest.Document("1422080", "a"));
        assertThat(abstractText).isEqualTo(new ParsedInputText());

        List<ParsedInputText> inputTexts = abstractServerHandler.load(new IdList("a", Lists.newArrayList("1422080")));
        assertThat(inputTexts).hasSize(0);

    }
}