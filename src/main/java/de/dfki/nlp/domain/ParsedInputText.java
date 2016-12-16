package de.dfki.nlp.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
public class ParsedInputText {

    String externalId;
    String title;
    String abstractText;

}
