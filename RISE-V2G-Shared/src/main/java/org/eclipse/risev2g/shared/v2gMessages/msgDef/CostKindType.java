/*******************************************************************************
 *  Copyright (c) 2015 Marc Mültin (Chargepartner GmbH).
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Dr.-Ing. Marc Mültin (Chargepartner GmbH) - initial API and implementation and initial documentation
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
 * <p>Java-Klasse für costKindType.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <p>
 * <pre>
 * &lt;simpleType name="costKindType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="relativePricePercentage"/>
 *     &lt;enumeration value="RenewableGenerationPercentage"/>
 *     &lt;enumeration value="CarbonDioxideEmission"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "costKindType")
@XmlEnum
public enum CostKindType {

    @XmlEnumValue("relativePricePercentage")
    RELATIVE_PRICE_PERCENTAGE("relativePricePercentage"),
    @XmlEnumValue("RenewableGenerationPercentage")
    RENEWABLE_GENERATION_PERCENTAGE("RenewableGenerationPercentage"),
    @XmlEnumValue("CarbonDioxideEmission")
    CARBON_DIOXIDE_EMISSION("CarbonDioxideEmission");
    private final String value;

    CostKindType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CostKindType fromValue(String v) {
        for (CostKindType c: CostKindType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
