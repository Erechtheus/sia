package de.dfki.nlp.diseaseNer;

import com.google.common.base.Charsets;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Created by philippe on 1/31/17.
 */
public class DiseasesNer {

    private final Trie trie;

    public DiseasesNer() {
        this("drugs.dict");
    }

    public DiseasesNer(String file) {

        Set<String> allNames;
        try {
            allNames = Resources.readLines(Resources.getResource(file), Charsets.UTF_8, new LineProcessor<Set<String>>() {

                Set<String> allNames = new TreeSet<>();

                @Override
                public boolean processLine(String line) throws IOException {

                    line = line.replaceAll("\"", "");
                    line = line.replaceAll("\\(", "");
                    line = line.replaceAll("\\)", "");
                    line = line.replaceAll("#", "");

                    if (line.length() > 5 && !line.matches("^[A-Z0-9\\s]*$")) {
                        allNames.add(line.toLowerCase());
                    }

                    return false;
                }

                @Override
                public Set<String> getResult() {
                    return allNames;
                }
            });

        } catch (IOException e) {
            throw new IllegalArgumentException("Can't initialize DiseasesNer", e);
        }

        Trie.TrieBuilder builder = Trie.builder()
                .caseInsensitive()
                .onlyWholeWords()
                .removeOverlaps();

        allNames.forEach(builder::addKeyword);

        this.trie = builder.build();

    }

    public Stream<DiseaseMention> extractFromText(String text){

        Collection<Emit> emits = trie.parseText(text);
        return emits.stream().map(a -> new DiseaseMention(a.getStart(), a.getEnd(), a.getKeyword()));
    }
}
