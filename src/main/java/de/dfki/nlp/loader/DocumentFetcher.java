package de.dfki.nlp.loader;

import com.google.common.collect.Iterables;
import de.dfki.nlp.config.AnnotatorConfig;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.pubmed.Abstract;
import de.dfki.nlp.domain.pubmed.AbstractText;
import de.dfki.nlp.domain.pubmed.PubmedArticle;
import de.dfki.nlp.domain.pubmed.PubmedArticleSet;
import de.dfki.nlp.domain.rest.ServerRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DocumentFetcher {

    private final AnnotatorConfig annotatorConfig;
    private final RetryHandler retryHandler;

    private final XPathFactory xpathFactory = XPathFactory.newInstance();
    private final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    public DocumentFetcher(AnnotatorConfig annotatorConfig, RetryHandler retryHandler) {
        this.annotatorConfig = annotatorConfig;
        this.retryHandler = retryHandler;
    }

    public ParsedInputText load(ServerRequest.Document document) {

        // default, when there is an error retrieving the document
        ParsedInputText parsedInputText = new ParsedInputText(null, null, null, null);

        switch (document.getSource().toLowerCase(Locale.ENGLISH)) {
            case "pubmed":

                log.debug("Downloading pubmed {}", document.getDocument_id());

                try {
                    PubmedArticleSet pubmedArticleSet = retryHandler.retryableGet(
                            annotatorConfig.pubmed.url,
                            PubmedArticleSet.class, document.getDocument_id());

                    // the retry handler can return an empty document .. if the parsing fails, or retry is exhausted
                    // or the document does not contain an article
                    if(pubmedArticleSet.getPubmedArticleOrPubmedBookArticle().size() == 0) return parsedInputText;
                    // now we get the article
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

                    parsedInputText = new ParsedInputText(document.getDocument_id(), titleText, abstractText, null);
                } catch (RestClientException | NoSuchElementException | IllegalArgumentException | NullPointerException e) {
                    log.error("Error retrieving pubmed document from server", e);
                }

                break;

            case "pmc":


                try {

                    String pmc = retryHandler.retryableGet(
                            annotatorConfig.pmc.url,
                            String.class, document.getDocument_id());

                    if(StringUtils.isEmpty(pmc)) return parsedInputText;

                    InputSource source = new InputSource(new StringReader(pmc));

                    DocumentBuilder db = dbf.newDocumentBuilder();
                    Document xmlDocument = db.parse(source);

                    XPath xpath = xpathFactory.newXPath();

                    String title = xpath.evaluate("/pmc-articleset/article/front/article-meta/title-group/article-title", xmlDocument);
                    String abstractT = xpath.evaluate("/pmc-articleset/article/front/article-meta/abstract", xmlDocument);

                    title = StringUtils.defaultIfEmpty(StringUtils.trim(title), null);
                    abstractT = StringUtils.defaultIfEmpty(StringUtils.trim(abstractT), null);

                    parsedInputText = new ParsedInputText(document.getDocument_id(), title, abstractT, null);

                } catch (RestClientException | ParserConfigurationException | SAXException | IOException | XPathExpressionException | NullPointerException e) {
                    log.error("Error retrieving pmc document from {}: {}", e.getMessage());
                }

                break;

            case "abstract server":

                try {
                    parsedInputText = retryHandler.retryableGet(annotatorConfig.abstractserver.url, ParsedInputText.class, document.getDocument_id());

                    // move text to abstract text
                    parsedInputText.setAbstractText(parsedInputText.getText());
                    parsedInputText.setText(null);

                } catch (RestClientException e) {
                    log.error("Error retrieving abstract {}", document.getDocument_id(), e);
                }


                break;

            case "patent server":

                try {
                    parsedInputText = retryHandler.retryableGet(annotatorConfig.patent.url, ParsedInputText.class, document.getDocument_id());
                } catch (RestClientException e) {
                    log.error("Error retrieving patent {}", document.getDocument_id(), e);
                }

                break;

            default:
                log.error("Don't know how to handle: {}", document.getSource());
                return parsedInputText;

        }


        log.debug("extracted {}", parsedInputText.toString());
        return parsedInputText;


    }


}
