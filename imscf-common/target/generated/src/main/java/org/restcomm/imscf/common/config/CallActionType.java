//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.11.14 at 11:44:59 AM CET 
//


package org.restcomm.imscf.common.config;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CallActionType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CallActionType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="continue"/&gt;
 *     &lt;enumeration value="release"/&gt;
 *     &lt;enumeration value="failover"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "CallActionType")
@XmlEnum
public enum CallActionType {

    @XmlEnumValue("continue")
    CONTINUE("continue"),
    @XmlEnumValue("release")
    RELEASE("release"),
    @XmlEnumValue("failover")
    FAILOVER("failover");
    private final String value;

    CallActionType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CallActionType fromValue(String v) {
        for (CallActionType c: CallActionType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}