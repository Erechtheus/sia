package de.dfki.nlp.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum PredictionType {

    UNKNOWN, CHEMICAL, PROTEIN, DISEASE, ORGANISM, ANATOMIC_COMPONENT,
    CELL_LINE_AND_CELL_TYPE, MUTATION, GENE, SUBCELLULAR_STRUCTURE, TISSUE_AND_ORGAN, MIRNA;

    /**
     * Handle case insensitive enums, just in case.
     *
     * @param key the String key
     * @return the found PredictionType
     */
    @JsonCreator
    public static PredictionType fromString(String key) {
        for(PredictionType type : PredictionType.values()) {
            if(type.name().equalsIgnoreCase(key)) {
                return type;
            }
        }

        log.error("Don't know how to handle Prediction type -> {}", key);

        // catch all
        return UNKNOWN;

    }
}
