package de.dfki.nlp.config;

import de.dfki.nlp.domain.PredictionType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

@ConfigurationProperties(prefix = "sia.annotators")
@Configuration
@Data
public class EnabledAnnotators {

    public boolean banner;
    public boolean diseaseNer;
    public boolean dnorm;
    public boolean linnaeus;
    public boolean mirNer;
    public boolean seth;
    public boolean chemspot;

    public Set<PredictionType> enabledPredicationTypes() {
        Set<PredictionType> types = new HashSet<>();

        if (banner) types.add(PredictionType.GENE);
        if (diseaseNer) types.add(PredictionType.DISEASE);
        if (dnorm) types.add(PredictionType.DISEASE);
        if (linnaeus) types.add(PredictionType.ORGANISM);
        if (mirNer) types.add(PredictionType.MIRNA);
        if (seth) types.add(PredictionType.MUTATION);
        if (chemspot) types.add(PredictionType.CHEMICAL);

        return types;
    }

    @PostConstruct
    void isValid() {
        // check if at least on of the methods is enabled
        if (enabledPredicationTypes().size() == 0) {
            throw new IllegalArgumentException("Please enable at least one annotator");
        }
    }

}
