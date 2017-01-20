package de.dfki.nlp.config;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Central configuration class.
 */
@ConfigurationProperties(prefix = "server")
@Component
@Data
public class AnnotatorConfig {

    /** Number of concurrent consumers for the input queue. */
    @Min(1)
    @Max(100)
    public int concurrentConsumer;

    @Min(1)
    @Max(100)
    public int concurrentHandler;

    @NotEmpty
    public String version;

    @NotEmpty
    public String changes;

    @Valid
    @NotNull
    public Def pubmed;

    @Valid
    @NotNull
    public Def pmc;

    @Valid
    @NotNull
    public Def patent;

    @Data
    public static class Def {

        @NotEmpty
        public String url;
    }

}
