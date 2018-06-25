package de.dfki.nlp.genes;


import banner.BannerProperties;
import banner.Sentence;
import banner.tagging.CRFTagger;
import banner.tagging.Mention;
import banner.tokenization.Tokenizer;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
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

            Properties bannerProperties = loadProperties("banner/banner.properties");

            Pair<File, BannerProperties> init = loadBanner(bannerProperties);
            tokenizer = init.getRight().getTokenizer();

            tagger = CRFTagger.load(new File(init.getLeft(), "banner/gene_model_v02.bin"), init.getRight().getLemmatiser(), init.getRight().getPosTagger(), null);

        } catch (Exception ex) {
            throw new IllegalStateException("Can't init BannerNER", ex);
        }
    }

    public static Pair<File, BannerProperties> loadBanner(Properties bannerProperties) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        File directory = unpackResourceFiles();

        // patch location
        bannerProperties.setProperty("lemmatiserDataDirectory", new File(directory, "banner/nlpdata/lemmatiser").toString());
        bannerProperties.setProperty("posTaggerDataDirectory", new File(directory, "banner/nlpdata/tagger").toString());

        return Pair.of(directory, BannerProperties.load(bannerProperties));

    }


    public List<Mention> extractFromText(String text) {

        Sentence sentence = new Sentence(text);
        tokenizer.tokenize(sentence);
        tagger.tag(sentence);

        // TODO translate token offsets into char offsets
        return sentence.getMentions();
    }


    public static Properties loadProperties(String filename) {
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

    private static File unpackResourceFiles() throws IOException {
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
