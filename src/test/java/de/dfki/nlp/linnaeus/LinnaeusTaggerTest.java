package de.dfki.nlp.linnaeus;

import org.junit.Ignore;
import org.junit.Test;
import uk.ac.man.entitytagger.Mention;

import java.util.List;

public class LinnaeusTaggerTest {

    @Test
    @Ignore("Out of memory on travis")
    public void test() {
        LinnaeusTagger linnaeusTagger = new LinnaeusTagger();

        String text = "The abilities of LHRH and a potent LHRH agonist ([D-Ser-(But),6, " +
                "des-Gly-NH210]LHRH ethylamide) inhibit FSH responses by rat " +
                "granulosa cells and Sertoli cells in vitro have been compared.";

        List<Mention> match = linnaeusTagger.match(text);

        System.out.println(match);
    }

}