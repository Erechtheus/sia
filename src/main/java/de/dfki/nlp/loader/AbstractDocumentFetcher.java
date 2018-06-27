package de.dfki.nlp.loader;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import de.dfki.nlp.domain.IdList;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.rest.ServerRequest;

import java.util.List;

public abstract class AbstractDocumentFetcher {

    abstract List<ParsedInputText> load(IdList idList);

    public ParsedInputText load(ServerRequest.Document document) {
        List<ParsedInputText> parsedInputTexts = load(new IdList(document.getSource(), Lists.newArrayList(document)));
        // default, when there is an error retrieving the document
        return Iterables.getFirst(parsedInputTexts, new ParsedInputText());
    }

}
