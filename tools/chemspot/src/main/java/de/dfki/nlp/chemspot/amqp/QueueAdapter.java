package de.dfki.nlp.chemspot.amqp;

import static de.dfki.nlp.domain.PredictionResult.Section.T;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.dfki.nlp.chemspot.ChemSpotRunner;
import de.dfki.nlp.chemspot.QueueAdapterChemSpot;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.PredictionResult;
import de.dfki.nlp.domain.PredictionType;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class QueueAdapter {

    @Autowired
    private ChemSpotRunner chemSpotRunner;

    @Autowired
    private ObjectMapper objectMapper;

    @RabbitListener(queuesToDeclare = @Queue(name = QueueAdapterChemSpot.queueName, durable = "false"))
    public String processOrder(ParsedInputText payload) throws JsonProcessingException {

        String docId = payload.getExternalId();
        if (docId == null)
            return objectMapper.writeValueAsString(Collections.emptySet());

        Set<PredictionResult> results = new HashSet<>();

        log.trace("Parsing {}", docId);

        // iterate over the text sections
        for (PredictionResult.Section section : PredictionResult.Section.values()) {

            String analyzetext = section == T ? payload.getTitle() : payload.getAbstractText();
            if (analyzetext == null)
                continue;

            results.addAll(
                    detectChemSpot(analyzetext, section, docId).collect(Collectors.toList()));

        }

        log.trace("Done parsing {}", docId);

        return objectMapper.writeValueAsString(results);

    }

    private Stream<PredictionResult> detectChemSpot(String analyzetext,
            PredictionResult.Section section, String externalID) {
        return chemSpotRunner.parse(analyzetext).stream().map(m -> {

            PredictionResult predictionResult = new PredictionResult();
            predictionResult.setSection(section);
            predictionResult.setDocumentId(externalID);
            predictionResult.setInit(m.getStart());
            predictionResult.setEnd(m.getEnd());

            predictionResult.setAnnotatedText(m.getText());

            predictionResult.setDatabaseId(m.getCHID());

            /**
             * SYSTEMATIC, IDENTIFIER, FORMULA, TRIVIAL, ABBREVIATION, FAMILY, MULTIPLE,
             * UNKNOWN;
             */

            predictionResult.setType(PredictionType.CHEMICAL);
            predictionResult.setScore(1.0);

            return predictionResult;
        });
    }

}
