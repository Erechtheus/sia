package de.dfki.nlp.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ParsedInputText implements Serializable {

    String externalId;
    String title;
    String abstractText;

    String text;

}
