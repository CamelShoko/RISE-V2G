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

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für ChargeParameterDiscoveryReqType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="ChargeParameterDiscoveryReqType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:iso:15118:2:2013:MsgBody}BodyBaseType">
 *       &lt;sequence>
 *         &lt;element name="MaxEntriesSAScheduleTuple" type="{http://www.w3.org/2001/XMLSchema}unsignedShort" minOccurs="0"/>
 *         &lt;element name="RequestedEnergyTransferMode" type="{urn:iso:15118:2:2013:MsgDataTypes}EnergyTransferModeType"/>
 *         &lt;element ref="{urn:iso:15118:2:2013:MsgDataTypes}EVChargeParameter"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ChargeParameterDiscoveryReqType", namespace = "urn:iso:15118:2:2013:MsgBody", propOrder = {
    "maxEntriesSAScheduleTuple",
    "requestedEnergyTransferMode",
    "evChargeParameter"
})
public class ChargeParameterDiscoveryReqType
    extends BodyBaseType
{

    @XmlElement(name = "MaxEntriesSAScheduleTuple")
    @XmlSchemaType(name = "unsignedShort")
    protected Integer maxEntriesSAScheduleTuple;
    @XmlElement(name = "RequestedEnergyTransferMode", required = true)
    @XmlSchemaType(name = "string")
    protected EnergyTransferModeType requestedEnergyTransferMode;
    @XmlElementRef(name = "EVChargeParameter", namespace = "urn:iso:15118:2:2013:MsgDataTypes", type = JAXBElement.class)
    protected JAXBElement<? extends EVChargeParameterType> evChargeParameter;

    /**
     * Ruft den Wert der maxEntriesSAScheduleTuple-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getMaxEntriesSAScheduleTuple() {
        return maxEntriesSAScheduleTuple;
    }

    /**
     * Legt den Wert der maxEntriesSAScheduleTuple-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMaxEntriesSAScheduleTuple(Integer value) {
        this.maxEntriesSAScheduleTuple = value;
    }

    /**
     * Ruft den Wert der requestedEnergyTransferMode-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link EnergyTransferModeType }
     *     
     */
    public EnergyTransferModeType getRequestedEnergyTransferMode() {
        return requestedEnergyTransferMode;
    }

    /**
     * Legt den Wert der requestedEnergyTransferMode-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link EnergyTransferModeType }
     *     
     */
    public void setRequestedEnergyTransferMode(EnergyTransferModeType value) {
        this.requestedEnergyTransferMode = value;
    }

    /**
     * Ruft den Wert der evChargeParameter-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link EVChargeParameterType }{@code >}
     *     {@link JAXBElement }{@code <}{@link DCEVChargeParameterType }{@code >}
     *     {@link JAXBElement }{@code <}{@link ACEVChargeParameterType }{@code >}
     *     
     */
    public JAXBElement<? extends EVChargeParameterType> getEVChargeParameter() {
        return evChargeParameter;
    }

    /**
     * Legt den Wert der evChargeParameter-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link EVChargeParameterType }{@code >}
     *     {@link JAXBElement }{@code <}{@link DCEVChargeParameterType }{@code >}
     *     {@link JAXBElement }{@code <}{@link ACEVChargeParameterType }{@code >}
     *     
     */
    public void setEVChargeParameter(JAXBElement<? extends EVChargeParameterType> value) {
        this.evChargeParameter = value;
    }

}
