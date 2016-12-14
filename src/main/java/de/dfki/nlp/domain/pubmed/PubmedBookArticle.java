//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// �nderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2016.12.13 um 03:20:53 PM CET 
//


package de.dfki.nlp.domain.pubmed;

import javax.xml.bind.annotation.*;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "bookDocument",
    "pubmedBookData"
})
@XmlRootElement(name = "PubmedBookArticle")
public class PubmedBookArticle {

    @XmlElement(name = "BookDocument", required = true)
    protected BookDocument bookDocument;
    @XmlElement(name = "PubmedBookData")
    protected PubmedBookData pubmedBookData;

    /**
     * Ruft den Wert der bookDocument-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link BookDocument }
     *     
     */
    public BookDocument getBookDocument() {
        return bookDocument;
    }

    /**
     * Legt den Wert der bookDocument-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link BookDocument }
     *     
     */
    public void setBookDocument(BookDocument value) {
        this.bookDocument = value;
    }

    /**
     * Ruft den Wert der pubmedBookData-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link PubmedBookData }
     *     
     */
    public PubmedBookData getPubmedBookData() {
        return pubmedBookData;
    }

    /**
     * Legt den Wert der pubmedBookData-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link PubmedBookData }
     *     
     */
    public void setPubmedBookData(PubmedBookData value) {
        this.pubmedBookData = value;
    }

}
