package de.dfki.nlp.genes;


import banner.BannerProperties;
import banner.Sentence;
import banner.processing.PostProcessor;
import banner.tagging.CRFTagger;
import banner.tagging.Mention;
import banner.tokenization.Tokenizer;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;

@Slf4j
public class BannerNer {
    private CRFTagger tagger;
    private Tokenizer tokenizer;

    public BannerNer() {
        try {

            File directory = unpackResourceFiles();

            Properties bannerProperties = loadProperties("banner/banner.properties");

            // patch location
            bannerProperties.setProperty("lemmatiserDataDirectory", new File(directory, "banner/nlpdata/lemmatiser").toString());
            bannerProperties.setProperty("posTaggerDataDirectory", new File(directory, "banner/nlpdata/tagger").toString());

            BannerProperties properties = BannerProperties.load(bannerProperties);
            tokenizer = properties.getTokenizer();
            tagger = CRFTagger.load(new File(directory, "banner/gene_model_v02.bin"), properties.getLemmatiser(), properties.getPosTagger());
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
        } catch (Exception ex) {
            throw new IllegalStateException("Can't init BannerNER", ex);
        }
    }


    public List<Mention> extractFromText(String text) {

        Sentence sentence = new Sentence(text);
        tokenizer.tokenize(sentence);
        tagger.tag(sentence);

        // TODO translate token offsets into char offsets
        return sentence.getMentions();
    }


    public Properties loadProperties(String filename) {
        URL url = Resources.getResource(filename);
        final Properties properties = new Properties();

        final ByteSource byteSource = Resources.asByteSource(url);
        try (InputStream inputStream = byteSource.openBufferedStream()) {
            properties.load(inputStream);
        } catch (IOException e) {
            log.error("openBufferedStream failed!", e);
        }
        return properties;
    }

    private File unpackResourceFiles() throws IOException {
        File tempDir = Files.createTempDir();
        tempDir.deleteOnExit();

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "banner/" + "**");
        for (Resource resource : resources) {
            if (resource.exists() & resource.isReadable() && resource.contentLength() > 0) {
                URL url = resource.getURL();
                String urlString = url.toExternalForm();
                String targetName = urlString.substring(urlString.indexOf("banner"));
                File destination = new File(tempDir, targetName);
                FileUtils.copyURLToFile(url, destination);
            }
        }

        return tempDir;
    }

}
