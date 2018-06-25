package de.dfki.nlp.dnorm;

import banner.eval.BANNER;
import banner.postprocessing.PostProcessor;
import banner.tagging.CRFTagger;
import banner.tokenization.Tokenizer;
import banner.types.Mention;
import banner.types.Sentence;
import banner.types.SentenceWithOffset;
import banner.util.RankedList;
import banner.util.SentenceBreaker;
import com.google.common.io.Resources;
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
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.IOUtils;

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

@Slf4j
public class DNorm {

    private static CRFTagger tagger;
    private static XMLConfiguration config;
    private final SentenceBreaker breaker;
    private String DOWNLOAD_LOCATION = "https://www.ncbi.nlm.nih.gov/CBBresearch/Lu/Demo/tmTools/download/DNorm/DNorm-0.0.7.tgz";

    private SynonymTrainer syn;
    private Tokenizer tokenizer;

    public DNorm() {

        try {
            this.downloadAndUnzipFiles();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        String lexiconFilename = "dnorm-data/DNorm-0.0.7/data/CTD_diseases.tsv";
        String matrixFilename = "dnorm-data/DNorm-0.0.7/output/simmatrix_NCBIDisease_e4.bin";

        // Do the setup
        DiseaseNameAnalyzer analyzer = DiseaseNameAnalyzer.getDiseaseNameAnalyzer(true, true, false, true);
        MEDICLexiconLoader loader = new MEDICLexiconLoader();
        Lexicon lex = new Lexicon(analyzer);
        loader.loadLexicon(lex, lexiconFilename);
        lex.prepare();
        SynonymMatrix matrix = FullRankSynonymMatrix.load(new File(matrixFilename));
        syn = new SynonymTrainer(lex, matrix, 1000);

        try {
            prepareBANNER(Resources.getResource("dnorm/banner_NCBIDisease_TEST.xml"));
        } catch (Exception e) {
            throw new RuntimeException("Can't init DNorm", e);
        }

        breaker = new SentenceBreaker();

    }

    public void parse(String input) {
        List<Sentence> inputSentences = getNextSentenceList(input);
        List<Sentence> outputSentences = processSentences_BANNER(inputSentences);
        try {
            output(outputSentences);
        } catch (IOException e) {
            log.error("Error parsing", e);
        }
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

    private static void output(List<Sentence> outputSentences) throws IOException {
        for (int index = 0; index < outputSentences.size(); index++) {
            Sentence outputSentence = outputSentences.get(index);
            String documentId = outputSentence.getDocumentId();

            int offset = ((SentenceWithOffset) outputSentence).getOffset();
            for (Mention mention : outputSentence.getMentions()) {
                StringBuilder outputLine = new StringBuilder();
                outputLine.append(documentId);
                outputLine.append("\t");
                outputLine.append(mention.getStartChar() + offset);
                outputLine.append("\t");
                outputLine.append(mention.getEndChar() + offset);
                outputLine.append("\t");
                outputLine.append(mention.getText());
                outputLine.append("\t");
                if (mention.getConceptId() != null)
                    outputLine.append(mention.getConceptId());

                System.out.println(outputLine.toString());
            }
        }
    }

    public List<Sentence> processSentences_BANNER(List<Sentence> inputSentences) {
        // TODO Refactor this into separate NER and normalization methods
        List<Sentence> outputSentences = new ArrayList<Sentence>(inputSentences.size());
        for (Sentence inputSentence : inputSentences) {
            int offset = ((SentenceWithOffset) inputSentence).getOffset();
            Sentence bannerSentence = new SentenceWithOffset(inputSentence.getSentenceId(), inputSentence.getDocumentId(), inputSentence.getText(), offset);
            Tokenizer tokenizer = BANNER.getTokenizer(config);
            PostProcessor postProcessor = BANNER.getPostProcessor(config);
            Sentence outputSentence = BANNER.process(tagger, tokenizer, postProcessor, bannerSentence);

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


    private static void prepareBANNER(URL configurationFile) throws IOException, ConfigurationException {
        long start = System.currentTimeMillis();
        config = new XMLConfiguration(configurationFile);

        // patch locations
        EnvVariable.setDragonHome(".");
        EnvVariable.setCharSet("US-ASCII");
        EngLemmatiser lemmatiser = BANNER.getLemmatiser(config);
        Tagger posTagger = BANNER.getPosTagger(config);
        HierarchicalConfiguration localConfig = config.configurationAt(BANNER.class.getPackage().getName());
        String modelFilename = localConfig.getString("modelFilename");
        log.info("Model: " + modelFilename);
        tagger = CRFTagger.load(new File(modelFilename), lemmatiser, posTagger);
        log.info("Completed input: " + (System.currentTimeMillis() - start));
    }

    private void downloadAndUnzipFiles() throws IOException, ArchiveException {
        File location = new File("dnorm-data");
        File targetFile = new File(location, "DNorm-0.0.7.tgz");

        if (!location.exists() || !targetFile.exists()) {
            log.info("Downloading from {}", DOWNLOAD_LOCATION);
            unpackArchive(new URL(DOWNLOAD_LOCATION), targetFile, location);
        } else {
            log.info("Reusing linnaeus-data from {}", location.getAbsolutePath());
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
    private File unpackArchive(URL url, File downloadFile, File targetDir) throws IOException, ArchiveException {
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        if (!downloadFile.exists()) {
            try (InputStream in = new BufferedInputStream(url.openStream(), 1024)) {
                // make sure we get the actual file
                OutputStream out = new FileOutputStream(downloadFile);
                IOUtils.copy(in, out);
                log.info("Downloading done, unpacking");
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

        try (ArchiveInputStream input = new ArchiveStreamFactory()
                .createArchiveInputStream(new BufferedInputStream(new GzipCompressorInputStream(new FileInputStream(theFile))))) {
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

        return theFile;
    }

    private boolean buildDirectory(File file) {
        return file.exists() || file.mkdirs();
    }


}
