//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// �nderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2016.12.13 um 03:20:53 PM CET 
//


package de.dfki.nlp.domain.pubmed.domain;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "value"
})
@XmlRootElement(name = "ISSN")
public class ISSN {

    @XmlAttribute(name = "IssnType", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String issnType;
    @XmlValue
    protected String value;

    /**
     * Ruft den Wert der issnType-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIssnType() {
        return issnType;
    }

    /**
     * Legt den Wert der issnType-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIssnType(String value) {
        this.issnType = value;
    }

    /**
     * Ruft den Wert der value-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getvalue() {
        return value;
    }

    /**
     * Legt den Wert der value-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setvalue(String value) {
        this.value = value;
    }

}
