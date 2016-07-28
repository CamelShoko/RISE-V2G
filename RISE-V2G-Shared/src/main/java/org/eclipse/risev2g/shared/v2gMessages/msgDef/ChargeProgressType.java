/*******************************************************************************
 *  Copyright (c) 2016 Dr.-Ing. Marc Mültin.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Dr.-Ing. Marc Mültin - initial API and implementation and initial documentation
 *******************************************************************************/
//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2014.10.07 um 04:55:05 PM CEST 
//


package org.eclipse.risev2g.shared.v2gMessages.msgDef;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für chargeProgressType.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <p>
 * <pre>
 * &lt;simpleType name="chargeProgressType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Start"/>
 *     &lt;enumeration value="Stop"/>
 *     &lt;enumeration value="Renegotiate"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "chargeProgressType")
@XmlEnum
public enum ChargeProgressType {

    @XmlEnumValue("Start")
    START("Start"),
    @XmlEnumValue("Stop")
    STOP("Stop"),
    @XmlEnumValue("Renegotiate")
    RENEGOTIATE("Renegotiate");
    private final String value;

    ChargeProgressType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ChargeProgressType fromValue(String v) {
        for (ChargeProgressType c: ChargeProgressType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
