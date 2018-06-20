package de.dfki.nlp.genes;


import banner.BannerProperties;
import banner.Sentence;
import banner.processing.PostProcessor;
import banner.tagging.CRFTagger;
import banner.tagging.TaggedToken;
import banner.tokenization.Tokenizer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class BannerNer {
    private CRFTagger tagger;
    private Tokenizer tokenizer;

    public static void main(String [] args)
    {
        BannerNer ner = new BannerNer();
        ner.extractFromText("In this abstract we describe our BRCA1 gene findings.");
    }


    public BannerNer(){
        try {
            BannerProperties  properties = BannerProperties.load(("/home/philippe/workspace/sia/src/main/resources/banner/banner.properties"));
            tokenizer = properties.getTokenizer();
            tagger = CRFTagger.load(new File("/home/philippe/workspace/sia/src/main/resources/banner/gene_model_v02.bin"), properties.getLemmatiser(), properties.getPosTagger());
            PostProcessor postProcessor = properties.getPostProcessor();

            /**
            // For each sentence to be labeled
            {
                Sentence sentence = new Sentence("");
                tokenizer.tokenize(sentence);
                tagger.tag(sentence);
//            if (postProcessor != null)
                //               postProcessor.postProcess(sentence2);
                System.out.println(sentence.getTrainingText(properties.getTagFormat()));
            }
             */
        }catch(Exception ex) {
            System.out.println(ex);
        }
    }



    public Stream<GeneMention> extractFromText(String text){

        Sentence sentence = new Sentence(text);
        tokenizer.tokenize(sentence);
        tagger.tag(sentence);



        List<GeneMention> mentions = new ArrayList<>();
        System.out.println(sentence.getTrainingText(TaggedToken.TagFormat.IOB)); //TODO do the parsing here

        return mentions.stream();

    }




}
