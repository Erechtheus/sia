package de.dfki.nlp.dnorm;


import org.junit.Test;

public class DNormIntegrationTest {

    @Test
    public void load() {

        DNorm dNorm = new DNorm();
        String text = "The abilities of LHRH and a potent LHRH agonist ([D-Ser-(But),6, " +
                "des-Gly-NH210]LHRH ethylamide) inhibit FSH responses by rat " +
                "granulosa cells and Sertoli cells in vitro have been compared.";

        dNorm.parse(text);

    }

}