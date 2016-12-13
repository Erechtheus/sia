package de.dfki.nlp.loader;

import com.google.common.collect.Iterables;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.pubmed.domain.Abstract;
import de.dfki.nlp.domain.pubmed.domain.AbstractText;
import de.dfki.nlp.domain.pubmed.domain.PubmedArticle;
import de.dfki.nlp.domain.pubmed.domain.PubmedArticleSet;
import de.dfki.nlp.domain.rest.ServerRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DocumentLoader {

    private final RestTemplate restTemplate;

    public DocumentLoader(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ParsedInputText load(ServerRequest.Document document) {

        switch (document.getSource().toLowerCase(Locale.ENGLISH)) {
            case "pubmed":

                log.info("Downloading pubmed {}", document.getDocument_id());

                PubmedArticleSet pubmedArticleSet = restTemplate.getForObject(
                        "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id={id}&retmode=xml",
                        PubmedArticleSet.class, document.getDocument_id());

                // now we get article
                Object first = Iterables.getOnlyElement(pubmedArticleSet.getPubmedArticleOrPubmedBookArticle());

                String titleText = null;
                String abstractText = null;
                if (first instanceof PubmedArticle) {
                    PubmedArticle pubmedArticle = (PubmedArticle) first;

                    // get abstract and title
                    Abstract anAbstract = pubmedArticle.getMedlineCitation().getArticle().getAbstract();
                    if (anAbstract != null) {
                        List<AbstractText> abstracts = anAbstract.getAbstractText();
                        abstractText = abstracts.stream().map(AbstractText::getvalue).collect(Collectors.joining("\n"));
                    }
                    titleText = pubmedArticle.getMedlineCitation().getArticle().getArticleTitle().getvalue();
                }

                ParsedInputText parsedInputText = new ParsedInputText(document.getDocument_id(), titleText, abstractText);
                log.info("extracted {}", parsedInputText.toString());
                return parsedInputText;

            default:
                log.error("Don't know how to handle: {}", document.getSource());

        }

        return new ParsedInputText(null, null, null);
    }

}
