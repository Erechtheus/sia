package de.dfki.nlp.chemspot;

import de.berlin.hu.chemspot.ChemSpot;
import de.berlin.hu.chemspot.ChemSpotFactory;
import de.berlin.hu.chemspot.Mention;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;

@Slf4j
public class ChemSpotRunner {

    private final ChemSpot tagger;
    String DOWNLOAD_LOCATION = "https://www.informatik.hu-berlin.de/forschung/gebiete/wbi/resources/chemspot/chemspot-2.0.zip";

    public ChemSpotRunner() {

        try {
            this.downloadAndUnzipFiles();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        File datadir = new File("chemspot-data", "chemspot-2.0");

        tagger = ChemSpotFactory.createChemSpot(
                new File(datadir, "dict.zip").toString(),
                new File(datadir, "ids.zip").toString(),
                new File(datadir, "multiclass.bin").toString());
        String text = "The abilities of LHRH and a potent LHRH agonist ([D-Ser-(But),6, " +
                "des-Gly-NH210]LHRH ethylamide) inhibit FSH responses by rat " +
                "granulosa cells and Sertoli cells in vitro have been compared.";

        /**
         * Example output
         *
         * 50	63	D-Ser-(But),6	null	eumed_tagger,	FORMULA
         * 65	78	des-Gly-NH210	null	eumed_tagger,	FORMULA
         * 84	94	ethylamide	null	eumed_tagger,	SYSTEMATIC
         * 104	107	FSH	009002680	dictionary,	SYSTEMATIC
         */

        for (Mention mention : tagger.tag(text)) {
            System.out.printf("%d\t%d\t%s\t%s\t%s,\t%s%n",
                    mention.getStart(), mention.getEnd(), mention.getText(),
                    mention.getCHID(), mention.getSource(), mention.getType().toString());
        }

    }

    private void downloadAndUnzipFiles() throws IOException, ArchiveException {
        File location = new File("chemspot-data");
        File targetFile = new File(location, "chemspot-2.0.zip");

        if (!location.exists() || !targetFile.exists()) {
            log.info("Downloading from {}", DOWNLOAD_LOCATION);
            unpackArchive(new URL(DOWNLOAD_LOCATION), targetFile, location);
        } else {
            log.info("Reusing chemspot-data from {}", location.getAbsolutePath());
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
                .createArchiveInputStream(new BufferedInputStream(new FileInputStream(theFile)))) {
            ArchiveEntry entry;
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

    public List<Mention> parse(String analyzetext) {
        return tagger.tag(analyzetext);
    }
}
