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
    "grantID",
    "acronym",
    "agency",
    "country"
})
@XmlRootElement(name = "Grant")
public class Grant {

    @XmlElement(name = "GrantID")
    protected String grantID;
    @XmlElement(name = "Acronym")
    protected String acronym;
    @XmlElement(name = "Agency", required = true)
    protected String agency;
    @XmlElement(name = "Country", required = true)
    protected String country;

    /**
     * Ruft den Wert der grantID-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGrantID() {
        return grantID;
    }

    /**
     * Legt den Wert der grantID-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGrantID(String value) {
        this.grantID = value;
    }

    /**
     * Ruft den Wert der acronym-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAcronym() {
        return acronym;
    }

    /**
     * Legt den Wert der acronym-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAcronym(String value) {
        this.acronym = value;
    }

    /**
     * Ruft den Wert der agency-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAgency() {
        return agency;
    }

    /**
     * Legt den Wert der agency-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAgency(String value) {
        this.agency = value;
    }

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

}
