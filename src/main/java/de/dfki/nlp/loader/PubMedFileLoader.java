package de.dfki.nlp.loader;

import de.dfki.nlp.domain.IdList;
import de.dfki.nlp.domain.ParsedInputText;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PubMedFileLoader extends AbstractDocumentFetcher {

    public BTreeMap<String, ParsedInputText> map;

    public PubMedFileLoader() {

        File file = new File("file3.db");

        if (file.exists()) {
            DB db = DBMaker
                    .fileDB(file)
                    .fileMmapEnableIfSupported()
                    .cleanerHackEnable()
                    .closeOnJvmShutdown()
                    .readOnly()
                    .make();

            //noinspection unchecked
            map = (BTreeMap<String, ParsedInputText>) db
                    .treeMap("map")
                    .createOrOpen();
        }


    }

    @Override
    List<ParsedInputText> load(IdList idList) {

        if (map != null) {
            return idList.getIds().stream().map(map::get).map(p -> {
                p.setAbstractText(StringUtils.trimToNull(p.getAbstractText()));
                p.setTitle(StringUtils.trimToNull(p.getTitle()));
                p.setText(StringUtils.trimToNull(p.getText()));
                return p;
            }).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }


    }

    @PreDestroy
    public void close() {
        map.close();
    }
}
