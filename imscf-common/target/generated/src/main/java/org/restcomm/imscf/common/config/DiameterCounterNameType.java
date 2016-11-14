//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.11.14 at 11:44:58 AM CET 
//


package org.restcomm.imscf.common.config;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DiameterCounterNameType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="DiameterCounterNameType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="balanceQueryReceivedCount"/&gt;
 *     &lt;enumeration value="balanceQueryAnsweredCount"/&gt;
 *     &lt;enumeration value="debitQueryReceivedCount"/&gt;
 *     &lt;enumeration value="debitQueryAnsweredCount"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "DiameterCounterNameType")
@XmlEnum
public enum DiameterCounterNameType {

    @XmlEnumValue("balanceQueryReceivedCount")
    BALANCE_QUERY_RECEIVED_COUNT("balanceQueryReceivedCount"),
    @XmlEnumValue("balanceQueryAnsweredCount")
    BALANCE_QUERY_ANSWERED_COUNT("balanceQueryAnsweredCount"),
    @XmlEnumValue("debitQueryReceivedCount")
    DEBIT_QUERY_RECEIVED_COUNT("debitQueryReceivedCount"),
    @XmlEnumValue("debitQueryAnsweredCount")
    DEBIT_QUERY_ANSWERED_COUNT("debitQueryAnsweredCount");
    private final String value;

    DiameterCounterNameType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static DiameterCounterNameType fromValue(String v) {
        for (DiameterCounterNameType c: DiameterCounterNameType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}