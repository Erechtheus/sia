//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// �nderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2016.12.13 um 03:20:53 PM CET 
//


package de.dfki.nlp.domain.pubmed;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "pubmedArticleOrPubmedBookArticle",
    "deleteCitation"
})
@XmlRootElement(name = "PubmedArticleSet")
public class PubmedArticleSet {

    @XmlElements({
        @XmlElement(name = "PubmedArticle", required = true, type = PubmedArticle.class),
        @XmlElement(name = "PubmedBookArticle", required = true, type = PubmedBookArticle.class)
    })
    protected List<java.lang.Object> pubmedArticleOrPubmedBookArticle;
    @XmlElement(name = "DeleteCitation")
    protected DeleteCitation deleteCitation;

    /**
     * Gets the value of the pubmedArticleOrPubmedBookArticle property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the pubmedArticleOrPubmedBookArticle property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPubmedArticleOrPubmedBookArticle().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PubmedArticle }
     * {@link PubmedBookArticle }
     * 
     * 
     */
    public List<java.lang.Object> getPubmedArticleOrPubmedBookArticle() {
        if (pubmedArticleOrPubmedBookArticle == null) {
            pubmedArticleOrPubmedBookArticle = new ArrayList<java.lang.Object>();
        }
        return this.pubmedArticleOrPubmedBookArticle;
    }

    /**
     * Ruft den Wert der deleteCitation-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link DeleteCitation }
     *     
     */
    public DeleteCitation getDeleteCitation() {
        return deleteCitation;
    }

    /**
     * Legt den Wert der deleteCitation-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link DeleteCitation }
     *     
     */
    public void setDeleteCitation(DeleteCitation value) {
        this.deleteCitation = value;
    }

}
