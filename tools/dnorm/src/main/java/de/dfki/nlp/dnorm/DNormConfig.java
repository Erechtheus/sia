package de.dfki.nlp.dnorm;

import java.io.File;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "dnorm")
@Data
@Validated
public class DNormConfig {

    @NotBlank
    @URL
    private String downloadUrl;

    @NotBlank
    private String downloadFileName;

    @NotNull
    private File dataDirectory;

    @NotNull
    private File unpackedDirectory;

    @NotBlank
    private String lexiconFilename;

    @NotNull
    private File matrixFilename;

}
