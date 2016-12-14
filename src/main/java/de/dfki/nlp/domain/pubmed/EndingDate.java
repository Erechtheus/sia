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
    "year",
    "monthOrDayOrSeason"
})
@XmlRootElement(name = "EndingDate")
public class EndingDate {

    @XmlElement(name = "Year", required = true)
    protected Year year;
    @XmlElements({
        @XmlElement(name = "Month", type = Month.class),
        @XmlElement(name = "Day", type = Day.class),
        @XmlElement(name = "Season", type = Season.class)
    })
    protected List<java.lang.Object> monthOrDayOrSeason;

    /**
     * Ruft den Wert der year-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Year }
     *     
     */
    public Year getYear() {
        return year;
    }

    /**
     * Legt den Wert der year-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Year }
     *     
     */
    public void setYear(Year value) {
        this.year = value;
    }

    /**
     * Gets the value of the monthOrDayOrSeason property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the monthOrDayOrSeason property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMonthOrDayOrSeason().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Month }
     * {@link Day }
     * {@link Season }
     * 
     * 
     */
    public List<java.lang.Object> getMonthOrDayOrSeason() {
        if (monthOrDayOrSeason == null) {
            monthOrDayOrSeason = new ArrayList<java.lang.Object>();
        }
        return this.monthOrDayOrSeason;
    }

}
