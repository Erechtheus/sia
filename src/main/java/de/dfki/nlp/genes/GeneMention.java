package de.dfki.nlp.genes;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode
public class GeneMention {

    private int start;
    private int end;
    private String text;


}

