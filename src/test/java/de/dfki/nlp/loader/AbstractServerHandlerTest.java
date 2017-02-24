package de.dfki.nlp.loader;

import com.google.common.collect.Lists;
import de.dfki.nlp.config.AnnotatorConfig;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.rest.ServerRequest;
import de.dfki.nlp.io.RetryHandler;
import org.junit.Test;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


public class AbstractServerHandlerTest {

    @Test
    public void load() throws Exception {

        AnnotatorConfig annotatorConfig = new AnnotatorConfig();

        AnnotatorConfig.Def abstractserver = new AnnotatorConfig.Def();
        abstractserver.setUrl("http://193.147.85.10:8088/abstractserver/json/");
        annotatorConfig.setAbstractserver(abstractserver);

        AbstractServerHandler abstractServerHandler = new AbstractServerHandler(annotatorConfig, new RetryHandler(new RestTemplate()));


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

            List<ServerRequest.Document> documentList = idList.subList(0, i).stream().map(m -> new ServerRequest.Document(m, "a")).collect(Collectors.toList());

            // load multiple
            stopWatch.start("" + documentList.size());
            List<ParsedInputText> load = abstractServerHandler.load(documentList);
            stopWatch.stop();
            assertThat(load).hasSize(i);

        }

        System.out.printf(Locale.ENGLISH, "%s\t%s\t %n", "Task", "ms");
        Arrays.stream(stopWatch.getTaskInfo()).forEach(task -> {
            System.out.printf(Locale.ENGLISH, "%s\t%d\t %n", task.getTaskName(), task.getTimeMillis());
        });


    }

}