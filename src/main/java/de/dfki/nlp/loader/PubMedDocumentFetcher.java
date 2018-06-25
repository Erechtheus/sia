package de.dfki.nlp.loader;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import de.dfki.nlp.config.AnnotatorConfig;
import de.dfki.nlp.domain.IdList;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.pubmed.Abstract;
import de.dfki.nlp.domain.pubmed.AbstractText;
import de.dfki.nlp.domain.pubmed.PubmedArticle;
import de.dfki.nlp.domain.pubmed.PubmedArticleSet;
import de.dfki.nlp.io.RetryHandler;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.management.AttributeList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PubMedDocumentFetcher extends AbstractDocumentFetcher {

    private final AnnotatorConfig annotatorConfig;
    private final RetryHandler retryHandler;

    public PubMedDocumentFetcher(AnnotatorConfig annotatorConfig, RetryHandler retryHandler) {
        this.annotatorConfig = annotatorConfig;
        this.retryHandler = retryHandler;
    }

    public static ParsedInputText convert(PubmedArticle pubmedArticle) {
        // get abstract and title
        Abstract anAbstract = pubmedArticle.getMedlineCitation().getArticle().getAbstract();
        String abstractText = null;
        if (anAbstract != null) {
            List<AbstractText> abstracts = anAbstract.getAbstractText();
            abstractText = abstracts.stream().map(AbstractText::getvalue).collect(Collectors.joining("\n"));
        }

        final String id = pubmedArticle.getMedlineCitation().getPMID().getvalue();
        String titleText = pubmedArticle.getMedlineCitation().getArticle().getArticleTitle().getvalue();

        return new ParsedInputText(id, titleText, abstractText, null);

    }

    @Override
    List<ParsedInputText> load(IdList idList) {

        // load multiple pubmed documents at once

        String listOfIds = Joiner.on(",").join(idList.getIds());

        PubmedArticleSet pubmedArticleSet = retryHandler.retryableGet(
                annotatorConfig.pubmed.url,
                PubmedArticleSet.class, listOfIds);


        return MoreObjects.firstNonNull(pubmedArticleSet.getPubmedArticleOrPubmedBookArticle(), new AttributeList())
                .stream()
                .map(entry -> {

                    if (entry instanceof PubmedArticle) {
                        PubmedArticle pubmedArticle = (PubmedArticle) entry;

                        ParsedInputText parsedInputText = convert(pubmedArticle);

                        // match id with incoming
                        Optional<String> matchedID = idList
                                .getIds()
                                .stream()
                                .filter(givenId -> StringUtils.contains(givenId, parsedInputText.getExternalId()))
                                .findFirst();
                        matchedID.ifPresent(parsedInputText::setExternalId);

                        return parsedInputText;
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());


    }


}
