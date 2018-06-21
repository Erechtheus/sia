package de.dfki.nlp.linnaeus;

import lombok.extern.slf4j.Slf4j;
import martin.common.ArgParser;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;
import uk.ac.man.entitytagger.EntityTagger;
import uk.ac.man.entitytagger.Mention;
import uk.ac.man.entitytagger.matching.Matcher;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Logger;

@Slf4j
public class LinnaeusTagger {

    public static final String DOWNLOAD_LOCATION = "https://sourceforge.net/projects/linnaeus/files/Entity_packs/species-1.3.tgz/download";
    private final Matcher matcher;

    public static void main(String[] args) {
        LinnaeusTagger linnaeusTagger = new LinnaeusTagger();
    }

    public LinnaeusTagger() {
        try {
            this.downloadAndUnzipFiles();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        File variantMatcher = new File("linnaeus-data/species-1.3", "dict-species.tsv");
        File ppStopTerms = new File("linnaeus-data/species-1.3", "stoplist.tsv");
        File ppAcrProbs = new File("linnaeus-data/species-1.3", "synonyms-acronyms.tsv");
        File ppSpeciesFreqs = new File("linnaeus-data/species-1.3", "species-frequency.tsv");
        String args = String.format("--postProcessing --variantMatcher %s --ppStopTerms %s --ppAcrProbs %s --ppSpeciesFreqs %s",
                variantMatcher.getAbsolutePath(),
                ppStopTerms.getAbsolutePath(),
                ppAcrProbs.getAbsolutePath(),
                ppSpeciesFreqs.getAbsolutePath()
        );


        matcher = EntityTagger.getMatcher(new ArgParser(args.split(" ")), Logger.getLogger(this.getClass().getCanonicalName()));

        String text = "The abilities of LHRH and a potent LHRH agonist ([D-Ser-(But),6, " +
                "des-Gly-NH210]LHRH ethylamide) inhibit FSH responses by rat " +
                "granulosa cells and Sertoli cells in vitro have been compared.";

        List<Mention> match = matcher.match(text);

        System.out.println(match);

    }

    public List<Mention> match(String text) {
        return matcher.match(text);
    }

    private void downloadAndUnzipFiles() throws IOException, ArchiveException {
        File location = new File("linnaeus-data");
        File targetFile = new File(location, "species-1.3.tgz");

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
                    try (OutputStream o = Files.newOutputStream(f.toPath())) {
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
