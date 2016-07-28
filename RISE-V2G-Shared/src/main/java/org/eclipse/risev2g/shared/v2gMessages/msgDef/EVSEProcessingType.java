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
 * <p>Java-Klasse für EVSEProcessingType.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <p>
 * <pre>
 * &lt;simpleType name="EVSEProcessingType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Finished"/>
 *     &lt;enumeration value="Ongoing"/>
 *     &lt;enumeration value="Ongoing_WaitingForCustomerInteraction"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "EVSEProcessingType")
@XmlEnum
public enum EVSEProcessingType {

    @XmlEnumValue("Finished")
    FINISHED("Finished"),
    @XmlEnumValue("Ongoing")
    ONGOING("Ongoing"),
    @XmlEnumValue("Ongoing_WaitingForCustomerInteraction")
    ONGOING_WAITING_FOR_CUSTOMER_INTERACTION("Ongoing_WaitingForCustomerInteraction");
    private final String value;

    EVSEProcessingType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static EVSEProcessingType fromValue(String v) {
        for (EVSEProcessingType c: EVSEProcessingType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
