package de.dfki.nlp.loader;

import com.google.common.base.MoreObjects;
import de.dfki.nlp.config.AnnotatorConfig;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.pubmed.Abstract;
import de.dfki.nlp.domain.pubmed.AbstractText;
import de.dfki.nlp.domain.pubmed.PubmedArticle;
import de.dfki.nlp.domain.pubmed.PubmedArticleSet;
import de.dfki.nlp.domain.rest.ServerRequest;
import de.dfki.nlp.io.RetryHandler;

import javax.management.AttributeList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PubMedDocumentFetcher extends AbstractDocumentFetcher {

    private final AnnotatorConfig annotatorConfig;
    private final RetryHandler retryHandler;

    public PubMedDocumentFetcher(AnnotatorConfig annotatorConfig, RetryHandler retryHandler) {
        this.annotatorConfig = annotatorConfig;
        this.retryHandler = retryHandler;
    }

    @Override
    List<ParsedInputText> load(List<ServerRequest.Document> document) {

        // load multiple pubmed documents at once

        String listOfIds = document.stream().map(ServerRequest.Document::getDocument_id).collect(Collectors.joining(","));

        PubmedArticleSet pubmedArticleSet = retryHandler.retryableGet(
                annotatorConfig.pubmed.url,
                PubmedArticleSet.class, listOfIds);


        return MoreObjects.firstNonNull(pubmedArticleSet.getPubmedArticleOrPubmedBookArticle(), new AttributeList())
                .stream()
                .map(entry -> {

                    if (entry instanceof PubmedArticle) {
                        PubmedArticle pubmedArticle = (PubmedArticle) entry;

                        // get abstract and title
                        Abstract anAbstract = pubmedArticle.getMedlineCitation().getArticle().getAbstract();
                        String abstractText = null;
                        if (anAbstract != null) {
                            List<AbstractText> abstracts = anAbstract.getAbstractText();
                            abstractText = abstracts.stream().map(AbstractText::getvalue).collect(Collectors.joining("\n"));
                        }

                        String id = pubmedArticle.getMedlineCitation().getPMID().getvalue();

                        String titleText = pubmedArticle.getMedlineCitation().getArticle().getArticleTitle().getvalue();

                        return new ParsedInputText(id, titleText, abstractText, null);
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());


    }


}
