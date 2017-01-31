package de.dfki.nlp.diseaseNer;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DiseaseMention {

    private int start;
    private int end;
    private String text;


}
