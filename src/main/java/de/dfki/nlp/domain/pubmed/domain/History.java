//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// �nderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2016.12.13 um 03:20:53 PM CET 
//


package de.dfki.nlp.domain.pubmed.domain;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "pubMedPubDate"
})
@XmlRootElement(name = "History")
public class History {

    @XmlElement(name = "PubMedPubDate", required = true)
    protected List<PubMedPubDate> pubMedPubDate;

    /**
     * Gets the value of the pubMedPubDate property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the pubMedPubDate property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPubMedPubDate().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PubMedPubDate }
     * 
     * 
     */
    public List<PubMedPubDate> getPubMedPubDate() {
        if (pubMedPubDate == null) {
            pubMedPubDate = new ArrayList<PubMedPubDate>();
        }
        return this.pubMedPubDate;
    }

}
