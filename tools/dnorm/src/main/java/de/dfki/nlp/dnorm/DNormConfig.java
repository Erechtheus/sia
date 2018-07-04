package de.dfki.nlp.dnorm;

import java.io.File;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.sun.istack.internal.NotNull;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "dnorm")
@Data
public class DNormConfig {

    @NotNull
    String downloadUrl;

    @NotNull
    String downloadFileName;

    @NotNull
    File dataDirectory;

    @NotNull
    File unpackedDirectory;

    @NotNull
    String lexiconFilename;

    @NotNull
    File matrixFilename;

}
