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
    "country",
    "medlineTA",
    "nlmUniqueID",
    "issnLinking"
})
@XmlRootElement(name = "MedlineJournalInfo")
public class MedlineJournalInfo {

    @XmlElement(name = "Country")
    protected String country;
    @XmlElement(name = "MedlineTA", required = true)
    protected String medlineTA;
    @XmlElement(name = "NlmUniqueID")
    protected String nlmUniqueID;
    @XmlElement(name = "ISSNLinking")
    protected String issnLinking;

    /**
     * Ruft den Wert der country-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCountry() {
        return country;
    }

    /**
     * Legt den Wert der country-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCountry(String value) {
        this.country = value;
    }

    /**
     * Ruft den Wert der medlineTA-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMedlineTA() {
        return medlineTA;
    }

    /**
     * Legt den Wert der medlineTA-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMedlineTA(String value) {
        this.medlineTA = value;
    }

    /**
     * Ruft den Wert der nlmUniqueID-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNlmUniqueID() {
        return nlmUniqueID;
    }

    /**
     * Legt den Wert der nlmUniqueID-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNlmUniqueID(String value) {
        this.nlmUniqueID = value;
    }

    /**
     * Ruft den Wert der issnLinking-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getISSNLinking() {
        return issnLinking;
    }

    /**
     * Legt den Wert der issnLinking-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setISSNLinking(String value) {
        this.issnLinking = value;
    }

}
