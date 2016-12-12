package de.dfki.nlp.domain.rest;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Stats {

    ZonedDateTime localtime;
}
