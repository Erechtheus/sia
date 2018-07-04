package de.dfki.nlp.dnorm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({ "no_rabbit" })
public class DNormIntegrationTest {

    @Autowired
    DNorm dNorm;

    @Test
    public void load() {

        String text = "The abilities of LHRH and a potent LHRH agonist ([D-Ser-(But),6, "
                + "des-Gly-NH210]LHRH ethylamide) inhibit FSH responses by rat "
                + "granulosa cells and Sertoli cells in vitro have been compared.";

        dNorm.parse(text);

    }

}
