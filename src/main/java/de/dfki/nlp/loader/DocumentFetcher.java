package de.dfki.nlp.loader;

import com.google.common.collect.Iterables;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.pubmed.Abstract;
import de.dfki.nlp.domain.pubmed.AbstractText;
import de.dfki.nlp.domain.pubmed.PubmedArticle;
import de.dfki.nlp.domain.pubmed.PubmedArticleSet;
import de.dfki.nlp.domain.rest.ServerRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
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

    private final RestTemplate restTemplate;


    private XPathFactory xpathFactory = XPathFactory.newInstance();
    private DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    @Value("${server.pubmed.url}")
    String pubmedUrl;

    @Value("${server.pmc.url}")
    String pmcUrl;

    @Value("${server.patent.url}")
    String patentUrl;


    public DocumentFetcher(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ParsedInputText load(ServerRequest.Document document) {

        // default, when there is an error retrieving the document
        ParsedInputText parsedInputText = new ParsedInputText(null, null, null);

        switch (document.getSource().toLowerCase(Locale.ENGLISH)) {
            case "pubmed":

                log.debug("Downloading pubmed {}", document.getDocument_id());

                try {
                    PubmedArticleSet pubmedArticleSet = restTemplate.getForObject(
                            pubmedUrl,
                            PubmedArticleSet.class, document.getDocument_id());

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

                    parsedInputText = new ParsedInputText(document.getDocument_id(), titleText, abstractText);
                } catch (RestClientException | NoSuchElementException | IllegalArgumentException | NullPointerException e) {
                    log.error("Error retrieving pubmed document from server", e);
                }

                break;

            case "pmc":


                try {

                    String pmc = restTemplate.getForObject(
                            pmcUrl,
                            String.class, document.getDocument_id());


                    InputSource source = new InputSource(new StringReader(pmc));

                    DocumentBuilder db = dbf.newDocumentBuilder();
                    Document xmlDocument = db.parse(source);

                    XPath xpath = xpathFactory.newXPath();

                    String title = xpath.evaluate("/pmc-articleset/article/front/article-meta/title-group/article-title", xmlDocument);
                    String abstractT = xpath.evaluate("/pmc-articleset/article/front/article-meta/abstract", xmlDocument);

                    title = StringUtils.defaultIfEmpty(StringUtils.trim(title), null);
                    abstractT = StringUtils.defaultIfEmpty(StringUtils.trim(abstractT), null);

                    parsedInputText = new ParsedInputText(document.getDocument_id(), title, abstractT);

                } catch (RestClientException | ParserConfigurationException | SAXException | IOException | XPathExpressionException | NullPointerException e) {
                    log.error("Error retrieving pmc document", e);
                }

                break;

            case "patent server":

                try {
                    parsedInputText = restTemplate.getForObject(patentUrl, ParsedInputText.class, document.getDocument_id());
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
