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
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PubMedFileLoader extends AbstractDocumentFetcher {

    public final BTreeMap<String, ParsedInputText> map;

    public PubMedFileLoader() {
        DB db = DBMaker
                .fileDB("file3.db")
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

    @Override
    List<ParsedInputText> load(IdList idList) {

        return idList.getIds().stream().map(map::get).map(p -> {
            p.setAbstractText(StringUtils.trimToNull(p.getAbstractText()));
            p.setTitle(StringUtils.trimToNull(p.getTitle()));
            return p;
        }).collect(Collectors.toList());
    }

    @PreDestroy
    public void close() {
        map.close();
    }
}
