package de.dfki.nlp.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


/**
 * Prediction
 * <p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "document_id",
        "section",
        "init",
        "end",
        "score",
        "annotated_text",
        "type",
        "database_id"
})
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class PredictionResult implements Serializable {

    /**
     * Document external ID
     * (Required)
     */
    @JsonProperty("document_id")
    private String documentId;
    /**
     * Section of the prediction (T or A)
     * (Required)
     */
    @JsonProperty("section")
    private PredictionResult.Section section;
    /**
     * Init of the prediction
     * (Required)
     */
    @JsonProperty("init")
    private Integer init;
    /**
     * End of the prediction
     * (Required)
     */
    @JsonProperty("end")
    private Integer end;
    /**
     * Score of the prediction
     */
    @JsonProperty("score")
    private Double score;
    /**
     * Annotated text of the prediction
     * (Required)
     */
    @JsonProperty("annotated_text")
    private String annotatedText;
    /**
     * Type of the prediction
     */
    @JsonProperty("type")
    private PredictionType type;
    /**
     * Database ID of the prediction
     */
    @JsonProperty("database_id")
    private String databaseId;

    @JsonIgnore
    private Map<String, Object> additionalProperties;


    public PredictionResult(String documentId, Section section, Integer init, Integer end, Double score, String annotatedText, PredictionType type) {
        this.documentId = documentId;
        this.section = section;
        this.init = init;
        this.end = end;
        this.score = score;
        this.annotatedText = annotatedText;
        this.type = type;
    }

    /**
     * Document external ID
     * (Required)
     *
     * @return The documentId
     */
    @JsonProperty("document_id")
    public String getDocumentId() {
        return documentId;
    }

    /**
     * Document external ID
     * (Required)
     *
     * @param documentId The document_id
     */
    @JsonProperty("document_id")
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    /**
     * Section of the prediction (T or A)
     * (Required)
     *
     * @return The section
     */
    @JsonProperty("section")
    public PredictionResult.Section getSection() {
        return section;
    }

    /**
     * Section of the prediction (T or A)
     * (Required)
     *
     * @param section The section
     */
    @JsonProperty("section")
    public void setSection(PredictionResult.Section section) {
        this.section = section;
    }

    /**
     * Init of the prediction
     * (Required)
     *
     * @return The init
     */
    @JsonProperty("init")
    public Integer getInit() {
        return init;
    }

    /**
     * Init of the prediction
     * (Required)
     *
     * @param init The init
     */
    @JsonProperty("init")
    public void setInit(Integer init) {
        this.init = init;
    }

    /**
     * End of the prediction
     * (Required)
     *
     * @return The end
     */
    @JsonProperty("end")
    public Integer getEnd() {
        return end;
    }

    /**
     * End of the prediction
     * (Required)
     *
     * @param end The end
     */
    @JsonProperty("end")
    public void setEnd(Integer end) {
        this.end = end;
    }

    /**
     * Score of the prediction
     *
     * @return The score
     */
    @JsonProperty("score")
    public Double getScore() {
        return score;
    }

    /**
     * Score of the prediction
     *
     * @param score The score
     */
    @JsonProperty("score")
    public void setScore(Double score) {
        this.score = score;
    }

    /**
     * Annotated text of the prediction
     * (Required)
     *
     * @return The annotatedText
     */
    @JsonProperty("annotated_text")
    public String getAnnotatedText() {
        return annotatedText;
    }

    /**
     * Annotated text of the prediction
     * (Required)
     *
     * @param annotatedText The annotated_text
     */
    @JsonProperty("annotated_text")
    public void setAnnotatedText(String annotatedText) {
        this.annotatedText = annotatedText;
    }

    /**
     * Type of the prediction
     *
     * @return The type
     */
    @JsonProperty("type")
    public PredictionType getType() {
        return type;
    }

    /**
     * Type of the prediction
     *
     * @param type The type
     */
    @JsonProperty("type")
    public void setType(PredictionType type) {
        this.type = type;
    }

    /**
     * Database ID of the prediction
     *
     * @return The databaseId
     */
    @JsonProperty("database_id")
    public String getDatabaseId() {
        return databaseId;
    }

    /**
     * Database ID of the prediction
     *
     * @param databaseId The database_id
     */
    @JsonProperty("database_id")
    public void setDatabaseId(String databaseId) {
        this.databaseId = databaseId;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public enum Section {

        T("T"),
        A("A");
        private final String value;
        private final static Map<String, PredictionResult.Section> CONSTANTS = new HashMap<>();

        static {
            for (PredictionResult.Section c : values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Section(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static PredictionResult.Section fromValue(String value) {
            PredictionResult.Section constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}