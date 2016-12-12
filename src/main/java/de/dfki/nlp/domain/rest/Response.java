package de.dfki.nlp.domain.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Response {

    int status;
    boolean success = true;
    String becalm_key;

    Object data;

}
