package de.dfki.nlp.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PredictionType {

    UNKNOWN, CHEMICAL, PROTEIN, DISEASE, ORGANISM, ANATOMIC_COMPOUND,
    CELL_LINE_AND_CELL_TYPE, MUTATION, GENE, SUBCELLULAR_STRUCTURE, TISSUE_AND_ORGAN,MIRNA;

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

        return null;
    }
}
