package de.dfki.nlp.loader;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import de.dfki.nlp.config.AnnotatorConfig;
import de.dfki.nlp.domain.IdList;
import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.io.RetryHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class PMCDocumentFetcher extends AbstractDocumentFetcher {

    private final AnnotatorConfig annotatorConfig;
    private final RetryHandler retryHandler;

    private final XPathFactory xpathFactory = XPathFactory.newInstance();
    private final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();


    public PMCDocumentFetcher(AnnotatorConfig annotatorConfig, RetryHandler retryHandler) {
        this.annotatorConfig = annotatorConfig;
        this.retryHandler = retryHandler;
    }

    @Override
    List<ParsedInputText> load(IdList idList) {

        // load multiple pmc documents at once

        String listOfIds = Joiner.on(",").join(idList.getIds());

        String pmc = retryHandler.retryableGet(
                annotatorConfig.pmc.url,
                String.class, listOfIds);

        List<ParsedInputText> res = Lists.newArrayList();

        if (StringUtils.isEmpty(pmc)) return res;

        InputSource source = new InputSource(new StringReader(pmc));

        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document xmlDocument = db.parse(source);
            XPath xpath = xpathFactory.newXPath();

            // loop article
            NodeList result = (NodeList) xpath.evaluate("/pmc-articleset/article", xmlDocument, XPathConstants.NODESET);

            for (int i = 0; i < result.getLength(); i++) {

                String title = xpath.evaluate("front/article-meta/title-group/article-title", result.item(i));
                String abstractT = xpath.evaluate("front/article-meta/abstract[not(@*)]", result.item(i));
                title = StringUtils.defaultIfEmpty(StringUtils.trim(title), null);
                abstractT = StringUtils.defaultIfEmpty(StringUtils.trim(abstractT), null);
                final String id = xpath.evaluate("front/article-meta/article-id[@pub-id-type='pmc']", result.item(i));

                // match id with incoming
                Optional<String> matchedID = idList.getIds().stream().filter(givenId -> StringUtils.contains(givenId, id)).findFirst();

                if (!matchedID.isPresent()) {
                    log.error("Did not find a matching ID {} in {}", id, idList.getIds().get(i));
                }

                res.add(new ParsedInputText(matchedID.orElse(id), title, abstractT, null));
            }

            //parsedInputText = new ParsedInputText(document.getDocument_id(), title, abstractT, null);
        } catch (ParserConfigurationException | IOException | XPathExpressionException | SAXException e) {
            log.error("Error parsing pmc results", e);
        }


        return res;
    }
}
