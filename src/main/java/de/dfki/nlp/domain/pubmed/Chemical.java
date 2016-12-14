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
    "registryNumber",
    "nameOfSubstance"
})
@XmlRootElement(name = "Chemical")
public class Chemical {

    @XmlElement(name = "RegistryNumber", required = true)
    protected String registryNumber;
    @XmlElement(name = "NameOfSubstance", required = true)
    protected NameOfSubstance nameOfSubstance;

    /**
     * Ruft den Wert der registryNumber-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRegistryNumber() {
        return registryNumber;
    }

    /**
     * Legt den Wert der registryNumber-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRegistryNumber(String value) {
        this.registryNumber = value;
    }

    /**
     * Ruft den Wert der nameOfSubstance-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link NameOfSubstance }
     *     
     */
    public NameOfSubstance getNameOfSubstance() {
        return nameOfSubstance;
    }

    /**
     * Legt den Wert der nameOfSubstance-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link NameOfSubstance }
     *     
     */
    public void setNameOfSubstance(NameOfSubstance value) {
        this.nameOfSubstance = value;
    }

}
