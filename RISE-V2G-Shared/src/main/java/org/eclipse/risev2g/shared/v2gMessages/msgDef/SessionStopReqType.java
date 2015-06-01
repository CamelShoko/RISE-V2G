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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für SessionStopReqType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="SessionStopReqType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:iso:15118:2:2013:MsgBody}BodyBaseType">
 *       &lt;sequence>
 *         &lt;element name="ChargingSession" type="{urn:iso:15118:2:2013:MsgDataTypes}chargingSessionType"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SessionStopReqType", namespace = "urn:iso:15118:2:2013:MsgBody", propOrder = {
    "chargingSession"
})
public class SessionStopReqType
    extends BodyBaseType
{

    @XmlElement(name = "ChargingSession", required = true)
    @XmlSchemaType(name = "string")
    protected ChargingSessionType chargingSession;

    /**
     * Ruft den Wert der chargingSession-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link ChargingSessionType }
     *     
     */
    public ChargingSessionType getChargingSession() {
        return chargingSession;
    }

    /**
     * Legt den Wert der chargingSession-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link ChargingSessionType }
     *     
     */
    public void setChargingSession(ChargingSessionType value) {
        this.chargingSession = value;
    }

}
