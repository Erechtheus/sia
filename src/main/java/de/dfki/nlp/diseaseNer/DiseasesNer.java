package de.dfki.nlp.diseaseNer;

import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;

import java.io.*;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Created by philippe on 1/31/17.
 */
public class DiseasesNer {

    private final Trie trie;

    public DiseasesNer(){
        this("drugs.dict");
    }

    public DiseasesNer(String file){
        super();

        Set<String> allNames = new TreeSet<>();
        try{
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream is = classloader.getResourceAsStream(file);
            //BufferedReader br = new BufferedReader(new FileReader(new File(file)));
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            while(br.ready()){
                String line = br.readLine();

                line = line.replaceAll("\"","");
                line = line.replaceAll("\\(","");
                line = line.replaceAll("\\)","");
                line = line.replaceAll("#","");

                if(line.length() > 5 && !line.matches("^[A-Z0-9\\s]*$")){
                    allNames.add(line.toLowerCase());
                }
            }
            br.close();

        }catch(Exception ex){
            ex.printStackTrace();
        }

        Trie.TrieBuilder builder = Trie.builder()
                .caseInsensitive()
                .onlyWholeWords()
                .removeOverlaps();

        for(String s : allNames){
            //System.out.println("'" +s +"'");
            builder.addKeyword(s);
        }
        this.trie = builder.build();

    }

    public Collection<DiseaseMention> extractFromText(String text){

        Collection<Emit> emits = trie.parseText(text);
        return emits.stream().map(a -> new DiseaseMention(a.getStart(), a.getEnd(), a.getKeyword())).collect(Collectors.toSet());
    }
}
