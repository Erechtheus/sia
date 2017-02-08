package de.dfki.nlp.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ParsedInputText {

    String externalId;
    String title;
    String abstractText;

    String text;

}
