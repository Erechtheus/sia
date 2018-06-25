import com.google.common.collect.Iterables;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.pubmed.PubmedArticle;
import de.dfki.nlp.domain.pubmed.PubmedArticleSet;
import de.dfki.nlp.loader.PubMedDocumentFetcher;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.oro.io.GlobFilenameFilter;
import org.junit.Ignore;
import org.junit.Test;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;

public class T {

    @Test
    @Ignore
    public void t() throws IOException, CompressorException {

        Jaxb2RootElementHttpMessageConverter converter = new Jaxb2RootElementHttpMessageConverter();
        converter.setSupportDtd(true);

        File directory = new File("tools/pubmedcache");
        String[] list = directory.list(new GlobFilenameFilter("*.gz"));

        try (DB db = DBMaker
                .fileDB("file3.db")
                .fileMmapEnable()
                .concurrencyDisable()
                .make()) {

            BTreeMap map = db.treeMap("map")
                    .counterEnable()
                    .valuesOutsideNodesEnable()
                    .keySerializer(Serializer.STRING_ASCII)
                    .createOrOpen();

            for (String filename : list) {
                try (CompressorInputStream is = new CompressorStreamFactory().createCompressorInputStream(
                        new BufferedInputStream(
                                new FileInputStream(
                                        new File(directory, filename)), 1024 * 64))) {

                    PubmedArticleSet read = (PubmedArticleSet) converter.read(PubmedArticleSet.class, new MockHttpInputMessage(is));

                    System.out.println(map.size());
                    read.getPubmedArticleOrPubmedBookArticle().forEach(a -> {
                        ParsedInputText convert = PubMedDocumentFetcher.convert((PubmedArticle) a);
                        map.put(convert.getExternalId(), convert);
                    });

                    System.out.println(map.size());

                }


            }

        }


    }

    @Test
    public void r() {
        try (DB db = DBMaker
                .fileDB("file3.db")
                .fileMmapEnableIfSupported()
                .cleanerHackEnable()
                .closeOnJvmShutdown()
                .readOnly()
                .make()) {

            BTreeMap map = db
                    .treeMap("map")
                    .createOrOpen();

            System.out.println(map.size());

            Set<String> strings = map.keySet();

            for (String aKey : Iterables.limit(strings, 1)) {

                System.out.println(map.get(aKey));
            }

        }
    }
}
