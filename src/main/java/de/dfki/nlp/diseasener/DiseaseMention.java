package de.dfki.nlp.diseasener;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode
public class DiseaseMention {

    private int start;
    private int end;
    private String text;


}
