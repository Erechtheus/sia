package de.dfki.nlp.config;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Central configuration class.
 */
@ConfigurationProperties(prefix = "server")
@Configuration
@Data
public class AnnotatorConfig {

    /**
     * Number of concurrent consumers for the input queue.
     *
     * <p>This defines how many concurrent requests can be processed per jvm.</p>
     */
    @Min(1)
    @Max(100)
    public int concurrentConsumer;

    /**
     * Number of concurrent annotator handlers.
     *
     * <p>Requests contain <code>n</code> documents, this property defines how many documents can be processed in parallel.</p>
     */
    @Min(1)
    @Max(100)
    public int concurrentHandler;


    /**
     * How many documents should be retrieved at once.
     */
    @Min(1)
    @Max(100)
    public int requestBulkSize = 10;

    /**
     * Maximum number of accepted documents per day.
     */
    @NotEmpty
    public String maxDaily;

    /**
     * Version of the current service, e.g. 1.0.
     */
    @NotEmpty
    public String version;

    /**
     * Changes in this version, e.g. small description.
     */
    @NotEmpty
    public String changes;

    /**
     * Location of the pubmed server.
     */
    @Valid
    @NotNull
    public Def pubmed;

    @Valid
    @NotNull
    public Def pmc;

    @Valid
    @NotNull
    public Def patent;

    @Valid
    @NotNull
    public Def abstractserver;

    @NotEmpty
    public String becalmSaveAnnotationLocation;

    /**
     * In case of an error sending the response to the becalm server, how often should we retry.
     */
    @Min(1)
    @Max(100)
    public Integer becalmSaveAnnotationRetries;

    @NotEmpty
    public String apiKey;

    @NotEmpty
    public String serverKey;

    @Data
    public static class Def {

        /**
         * Location of document to download, use <b>{id}</b> as id placeholder.
         *
         * <p>
         * E.g.: <code>http://example.com/download/{id}</code>
         * </p>
         */
        @NotEmpty
        public String url;
    }

}
