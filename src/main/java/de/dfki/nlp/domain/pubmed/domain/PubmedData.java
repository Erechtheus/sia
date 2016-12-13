//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// �nderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2016.12.13 um 03:20:53 PM CET 
//


package de.dfki.nlp.domain.pubmed.domain;

import javax.xml.bind.annotation.*;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "history",
    "publicationStatus",
    "articleIdList",
    "objectList"
})
@XmlRootElement(name = "PubmedData")
public class PubmedData {

    @XmlElement(name = "History")
    protected History history;
    @XmlElement(name = "PublicationStatus", required = true)
    protected String publicationStatus;
    @XmlElement(name = "ArticleIdList", required = true)
    protected ArticleIdList articleIdList;
    @XmlElement(name = "ObjectList")
    protected ObjectList objectList;

    /**
     * Ruft den Wert der history-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link History }
     *     
     */
    public History getHistory() {
        return history;
    }

    /**
     * Legt den Wert der history-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link History }
     *     
     */
    public void setHistory(History value) {
        this.history = value;
    }

    /**
     * Ruft den Wert der publicationStatus-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPublicationStatus() {
        return publicationStatus;
    }

    /**
     * Legt den Wert der publicationStatus-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPublicationStatus(String value) {
        this.publicationStatus = value;
    }

    /**
     * Ruft den Wert der articleIdList-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link ArticleIdList }
     *     
     */
    public ArticleIdList getArticleIdList() {
        return articleIdList;
    }

    /**
     * Legt den Wert der articleIdList-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link ArticleIdList }
     *     
     */
    public void setArticleIdList(ArticleIdList value) {
        this.articleIdList = value;
    }

    /**
     * Ruft den Wert der objectList-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link ObjectList }
     *     
     */
    public ObjectList getObjectList() {
        return objectList;
    }

    /**
     * Legt den Wert der objectList-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link ObjectList }
     *     
     */
    public void setObjectList(ObjectList value) {
        this.objectList = value;
    }

}
