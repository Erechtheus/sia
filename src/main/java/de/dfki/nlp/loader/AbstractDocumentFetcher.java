package de.dfki.nlp.loader;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.rest.ServerRequest;

import java.util.List;

public abstract class AbstractDocumentFetcher {

    abstract List<ParsedInputText> load(List<ServerRequest.Document> document);

    public ParsedInputText load(ServerRequest.Document document) {
        List<ParsedInputText> parsedInputTexts = load(Lists.newArrayList(document));
        return Iterables.getOnlyElement(parsedInputTexts);
    }

}
