package de.dfki.nlp.linnaeus;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import lombok.extern.slf4j.Slf4j;
import martin.common.ArgParser;
import uk.ac.man.entitytagger.EntityTagger;
import uk.ac.man.entitytagger.Mention;
import uk.ac.man.entitytagger.matching.Matcher;

@Slf4j
public class LinnaeusTagger {

    public static final String DOWNLOAD_LOCATION = "https://sourceforge.net/projects/linnaeus/files/Entity_packs/species-1.3.tgz/download";
    private final Matcher matcher;

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

    }

    public List<Mention> match(String text) {
        return matcher.match(text);
    }

    private void downloadAndUnzipFiles() throws IOException, ArchiveException {
        File location = new File("linnaeus-data");
        File targetFile = new File(location, "species-1.3.tgz");

        if (!location.exists() || !targetFile.exists()) {
            log.info("Downloading from {}", DOWNLOAD_LOCATION);
            unpackArchive(DOWNLOAD_LOCATION, targetFile, location);
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
    private File unpackArchive(String url, File downloadFile, File targetDir) throws IOException, ArchiveException {
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        if (!downloadFile.exists()) {
            try (CloseableHttpClient httpclient = HttpClients
                    .custom()
                    .setSSLHostnameVerifier(new NoopHostnameVerifier())
                    .build()) {
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
