package de.dfki.nlp.dnorm;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Component;

import com.google.common.io.Resources;

import banner.eval.BANNER;
import banner.postprocessing.PostProcessor;
import banner.tagging.CRFTagger;
import banner.tokenization.Tokenizer;
import banner.types.Mention;
import banner.types.Sentence;
import banner.types.SentenceWithOffset;
import banner.util.RankedList;
import banner.util.SentenceBreaker;
import de.dfki.nlp.domain.PredictionResult;
import dnorm.core.DiseaseNameAnalyzer;
import dnorm.core.Lexicon;
import dnorm.core.MEDICLexiconLoader;
import dnorm.core.SynonymTrainer;
import dnorm.types.FullRankSynonymMatrix;
import dnorm.types.SynonymMatrix;
import dragon.nlp.tool.Tagger;
import dragon.nlp.tool.lemmatiser.EngLemmatiser;
import dragon.util.EnvVariable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DNorm {

    private static CRFTagger tagger;

    private static XMLConfiguration config;

    private final SentenceBreaker breaker;

    private final DNormConfig dNormConfig;

    private SynonymTrainer syn;

    private Tokenizer tokenizer;

    public DNorm(DNormConfig dNormConfig) {
        this.dNormConfig = dNormConfig;

        try {
            this.downloadAndUnzipFiles();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        // Do the setup
        DiseaseNameAnalyzer analyzer = DiseaseNameAnalyzer.getDiseaseNameAnalyzer(true, true, false,
                true);
        MEDICLexiconLoader loader = new MEDICLexiconLoader();
        Lexicon lex = new Lexicon(analyzer);
        loader.loadLexicon(lex, dNormConfig.lexiconFilename);
        lex.prepare();
        SynonymMatrix matrix = FullRankSynonymMatrix.load(dNormConfig.matrixFilename);
        syn = new SynonymTrainer(lex, matrix, 1000);

        try {
            prepareBANNER(Resources.getResource("dnorm/banner_NCBIDisease_TEST.xml"));
        } catch (Exception e) {
            throw new RuntimeException("Can't init DNorm", e);
        }

        breaker = new SentenceBreaker();

    }

    public List<PredictionResult> parse(String input) {
        List<Sentence> inputSentences = getNextSentenceList(input);
        List<Sentence> outputSentences = processSentences_BANNER(inputSentences);
        return output(outputSentences);
    }

    private List<Sentence> getNextSentenceList(String input) {
        List<Sentence> currentSentences = new ArrayList<Sentence>();
        breaker.setText(input);
        int start = 0;
        List<String> sentenceTexts = breaker.getSentences();
        for (int i = 0; i < sentenceTexts.size(); i++) {
            String sentenceId = Integer.toString(i);
            if (sentenceId.length() < 2)
                sentenceId = "0" + sentenceId;

            String sentenceText = sentenceTexts.get(i);
            Sentence s = new SentenceWithOffset(sentenceId, "1", sentenceText, start);
            currentSentences.add(s);
            start += sentenceText.length();
        }
        return currentSentences;
    }

    private static List<PredictionResult> output(List<Sentence> outputSentences) {
        List<PredictionResult> predictionResults = new ArrayList<>();
        for (Sentence outputSentence : outputSentences) {
            int offset = ((SentenceWithOffset) outputSentence).getOffset();
            for (Mention mention : outputSentence.getMentions()) {

                PredictionResult predictionResult = new PredictionResult();

                predictionResult.setAnnotatedText(mention.getText());
                predictionResult.setDatabaseId(mention.getConceptId());
                predictionResult.setInit(mention.getStartChar() + offset);
                predictionResult.setEnd(mention.getEndChar() + offset);

                predictionResults.add(predictionResult);

            }
        }

        return predictionResults;
    }

    private List<Sentence> processSentences_BANNER(List<Sentence> inputSentences) {
        // TODO Refactor this into separate NER and normalization methods
        List<Sentence> outputSentences = new ArrayList<Sentence>(inputSentences.size());
        for (Sentence inputSentence : inputSentences) {
            int offset = ((SentenceWithOffset) inputSentence).getOffset();
            Sentence bannerSentence = new SentenceWithOffset(inputSentence.getSentenceId(),
                    inputSentence.getDocumentId(), inputSentence.getText(), offset);
            Tokenizer tokenizer = BANNER.getTokenizer(config);
            PostProcessor postProcessor = BANNER.getPostProcessor(config);
            Sentence outputSentence = BANNER.process(tagger, tokenizer, postProcessor,
                    bannerSentence);

            for (Mention mention : outputSentence.getMentions(Mention.MentionType.Found)) {
                String lookupText = mention.getText();
                // Do lookup & store results
                RankedList<SynonymTrainer.LookupResult> results = syn.lookup(lookupText);
                if (results.size() > 0) {
                    String conceptId = results.getObject(0).getConceptId();
                    mention.setConceptId(conceptId);
                }
            }
            outputSentences.add(outputSentence);
        }
        return outputSentences;
    }

    private static void prepareBANNER(URL configurationFile)
            throws IOException, ConfigurationException {
        long start = System.currentTimeMillis();
        config = new XMLConfiguration(configurationFile);

        // patch locations
        EnvVariable.setDragonHome(".");
        EnvVariable.setCharSet("US-ASCII");
        EngLemmatiser lemmatiser = BANNER.getLemmatiser(config);
        Tagger posTagger = BANNER.getPosTagger(config);
        HierarchicalConfiguration localConfig = config.configurationAt(
                BANNER.class.getPackage().getName());
        String modelFilename = localConfig.getString("modelFilename");
        log.info("Model: " + modelFilename);
        tagger = CRFTagger.load(new File(modelFilename), lemmatiser, posTagger);
        log.info("Completed input: " + (System.currentTimeMillis() - start));
    }

    private void downloadAndUnzipFiles() throws IOException, ArchiveException {
        File targetFile = new File(dNormConfig.dataDirectory, dNormConfig.downloadFileName);

        if (!dNormConfig.dataDirectory.exists() || !targetFile.exists()
                || !dNormConfig.unpackedDirectory.exists()) {
            unpackArchive(dNormConfig.downloadUrl, targetFile, dNormConfig.dataDirectory);
        } else {
            log.info("Reusing dnorm-data from {}", dNormConfig.dataDirectory.getAbsolutePath());
        }
    }

    /**
     * Unpack an archive from a URL
     *
     * @param url
     * @param targetDir
     * @return the file to the url
     * @throws IOException
     */
    private File unpackArchive(String url, File downloadFile, File targetDir)
            throws IOException, ArchiveException {
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        if (!downloadFile.exists()) {
            log.info("Downloading data from {}", url);
            try (CloseableHttpClient httpclient = HttpClients.custom().setSSLHostnameVerifier(
                    new NoopHostnameVerifier()).build()) {
                HttpGet httpget = new HttpGet(url);
                try (CloseableHttpResponse response = httpclient.execute(httpget)) {

                    InputStream in = response.getEntity().getContent();
                    OutputStream out = new FileOutputStream(downloadFile);
                    IOUtils.copy(in, out);
                    // make sure we get the actual file
                    log.info("Downloading done, unpacking");
                }
            }
        }

        return unpackArchive(downloadFile, targetDir);

    }

    /**
     * Unpack a compressed file
     *
     * @param theFile
     * @param targetDir
     * @return the file
     * @throws IOException
     */
    private File unpackArchive(File theFile, File targetDir) throws IOException, ArchiveException {
        if (!theFile.exists()) {
            throw new IOException(theFile.getAbsolutePath() + " does not exist");
        }
        if (!buildDirectory(targetDir)) {
            throw new IOException("Could not create directory: " + targetDir);
        }

        try (ArchiveInputStream input = new ArchiveStreamFactory().createArchiveInputStream(
                new BufferedInputStream(new FileInputStream(theFile)))) {
            ArchiveEntry entry = null;
            while ((entry = input.getNextEntry()) != null) {
                if (!input.canReadEntryData(entry)) {
                    // log something?
                    continue;
                }
                File f = new File(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    if (!f.isDirectory() && !f.mkdirs()) {
                        throw new IOException("failed to create directory " + f);
                    }
                } else {
                    File parent = f.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("failed to create directory " + parent);
                    }
                    try (OutputStream o = java.nio.file.Files.newOutputStream(f.toPath())) {
                        IOUtils.copy(input, o);
                    }
                }
            }
        }

        log.info("unpaked data into {}", targetDir);

        return theFile;
    }

    private boolean buildDirectory(File file) {
        return file.exists() || file.mkdirs();
    }

}
