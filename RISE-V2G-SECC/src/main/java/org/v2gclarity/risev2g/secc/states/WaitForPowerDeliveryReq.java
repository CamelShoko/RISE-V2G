/*******************************************************************************
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-207  V2G Clarity (Dr.-Ing. Marc Mültin) 
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *******************************************************************************/
package org.v2gclarity.risev2g.secc.states;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.v2gclarity.risev2g.secc.session.V2GCommunicationSessionSECC;
import org.v2gclarity.risev2g.shared.enumerations.V2GMessages;
import org.v2gclarity.risev2g.shared.messageHandling.ReactionToIncomingMessage;
import org.v2gclarity.risev2g.shared.v2gMessages.msgDef.ACEVSEStatusType;
import org.v2gclarity.risev2g.shared.v2gMessages.msgDef.ChargeProgressType;
import org.v2gclarity.risev2g.shared.v2gMessages.msgDef.ChargingProfileType;
import org.v2gclarity.risev2g.shared.v2gMessages.msgDef.DCEVSEStatusCodeType;
import org.v2gclarity.risev2g.shared.v2gMessages.msgDef.DCEVSEStatusType;
import org.v2gclarity.risev2g.shared.v2gMessages.msgDef.EVSENotificationType;
import org.v2gclarity.risev2g.shared.v2gMessages.msgDef.PowerDeliveryReqType;
import org.v2gclarity.risev2g.shared.v2gMessages.msgDef.PowerDeliveryResType;
import org.v2gclarity.risev2g.shared.v2gMessages.msgDef.ResponseCodeType;
import org.v2gclarity.risev2g.shared.v2gMessages.msgDef.SAScheduleTupleType;
import org.v2gclarity.risev2g.shared.v2gMessages.msgDef.V2GMessage;

public class WaitForPowerDeliveryReq extends ServerState {

	private PowerDeliveryResType powerDeliveryRes;
	
	public WaitForPowerDeliveryReq(
			V2GCommunicationSessionSECC commSessionContext) {
		super(commSessionContext);
		powerDeliveryRes = new PowerDeliveryResType();
	}

	@Override
	public ReactionToIncomingMessage processIncomingMessage(Object message) {
		if (isIncomingMessageValid(message, PowerDeliveryReqType.class, powerDeliveryRes)) {
			V2GMessage v2gMessageReq = (V2GMessage) message;
			PowerDeliveryReqType powerDeliveryReq = (PowerDeliveryReqType) v2gMessageReq.getBody().getBodyElement().getValue();

			if (isResponseCodeOK(powerDeliveryReq)) {
				getCommSessionContext().setChosenSAScheduleTuple(powerDeliveryReq.getSAScheduleTupleID());
				
				// For debugging purposes, log the ChargeProgress value
				getLogger().debug("ChargeProgress of PowerDeliveryReq set to '" + 
								  powerDeliveryReq.getChargeProgress().toString() + "'");
				
				// TODO regard [V2G2-866]
				
				setEVSEStatus(powerDeliveryRes);
				
				if (powerDeliveryReq.getChargeProgress().equals(ChargeProgressType.START)) {
					if (getCommSessionContext().getRequestedEnergyTransferMode().toString().startsWith("AC"))
						return getSendMessage(powerDeliveryRes, V2GMessages.CHARGING_STATUS_REQ);
					else
						return getSendMessage(powerDeliveryRes, V2GMessages.CURRENT_DEMAND_REQ);
				} else if (powerDeliveryReq.getChargeProgress().equals(ChargeProgressType.STOP)) {
					if (getCommSessionContext().getRequestedEnergyTransferMode().toString().startsWith("AC")) {
						return getSendMessage(powerDeliveryRes, V2GMessages.SESSION_STOP_REQ);
					} else {
						((ForkState) getCommSessionContext().getStates().get(V2GMessages.FORK))
						.getAllowedRequests().add(V2GMessages.WELDING_DETECTION_REQ);
						((ForkState) getCommSessionContext().getStates().get(V2GMessages.FORK))
						.getAllowedRequests().add(V2GMessages.SESSION_STOP_REQ);
						
						return getSendMessage(powerDeliveryRes, V2GMessages.FORK);
					}
				} else {
					return getSendMessage(powerDeliveryRes, V2GMessages.CHARGE_PARAMETER_DISCOVERY_REQ);
				}
			} else {
				getLogger().error("Response code '" + powerDeliveryRes.getResponseCode() + "' will be sent");
				setMandatoryFieldsForFailedRes();
			}
		} else {
			setMandatoryFieldsForFailedRes();
		}
		
		return getSendMessage(powerDeliveryRes, V2GMessages.NONE);
	}
	
	
	public boolean isResponseCodeOK(PowerDeliveryReqType powerDeliveryReq) {
		SAScheduleTupleType chosenSASchedule = getChosenSASCheduleTuple(powerDeliveryReq.getSAScheduleTupleID());
		
		if (chosenSASchedule == null) {
			getLogger().warn("Chosen SAScheduleTupleID in PowerDeliveryReq is null, but parameter is mandatory");
			powerDeliveryRes.setResponseCode(ResponseCodeType.FAILED_TARIFF_SELECTION_INVALID);
			return false;
		}
		
		// Important to call this AFTER checking for valid tariff selection because of possible null-value!
		if (!isChargingProfileValid(chosenSASchedule, powerDeliveryReq.getChargingProfile())) {
			powerDeliveryRes.setResponseCode(ResponseCodeType.FAILED_CHARGING_PROFILE_INVALID);
			return false;
		}
		
		// Not sure if these values are the ones to monitor when checking for FAILED_POWER_DELIVERY_NOT_APPLIED 
		if (getCommSessionContext().getRequestedEnergyTransferMode().toString().startsWith("AC")) {
			if (getCommSessionContext().getACEvseController().getACEVSEStatus(null).isRCD()) {
				getLogger().error("RCD has detected an error");
				powerDeliveryRes.setResponseCode(ResponseCodeType.FAILED_POWER_DELIVERY_NOT_APPLIED);
				return false;
			}
		} else {
			DCEVSEStatusCodeType dcEVSEStatusCode = 
					getCommSessionContext().getDCEvseController().getDCEVSEStatus(null).getEVSEStatusCode();
			
			if (dcEVSEStatusCode.equals(DCEVSEStatusCodeType.EVSE_NOT_READY) ||
				dcEVSEStatusCode.equals(DCEVSEStatusCodeType.EVSE_SHUTDOWN) ||
				dcEVSEStatusCode.equals(DCEVSEStatusCodeType.EVSE_EMERGENCY_SHUTDOWN) || 
				dcEVSEStatusCode.equals(DCEVSEStatusCodeType.EVSE_MALFUNCTION)) {
				getLogger().error("EVSE status code is '" + dcEVSEStatusCode.toString() + "'");
				powerDeliveryRes.setResponseCode(ResponseCodeType.FAILED_POWER_DELIVERY_NOT_APPLIED);
				return false;
			}
					
		}
		
		if ((powerDeliveryReq.getChargeProgress().equals(ChargeProgressType.START) &&
			 !getCommSessionContext().getEvseController().closeContactor()) ||
			(powerDeliveryReq.getChargeProgress().equals(ChargeProgressType.STOP) &&
			 !getCommSessionContext().getEvseController().openContactor())) {
			powerDeliveryRes.setResponseCode(ResponseCodeType.FAILED_CONTACTOR_ERROR);
			return false;
		}
		
		return true;
	}
	
	
	private void setEVSEStatus(PowerDeliveryResType powerDeliveryRes) {
		if (getCommSessionContext().getRequestedEnergyTransferMode().toString().startsWith("AC")) {
			/*
			 * The MiscUtils method getJAXBElement() cannot be used here because of the difference in the
			 * class name (ACEVSEStatus) and the name in the XSD (AC_EVSEStatus)
			 */
			JAXBElement jaxbEVSEStatus = new JAXBElement(new QName("urn:iso:15118:2:2013:MsgDataTypes", "AC_EVSEStatus"), 
					ACEVSEStatusType.class, 
					getCommSessionContext().getACEvseController().getACEVSEStatus(EVSENotificationType.NONE));
			powerDeliveryRes.setEVSEStatus(jaxbEVSEStatus);
		} else if (getCommSessionContext().getRequestedEnergyTransferMode().toString().startsWith("DC")) {
			/*
			 * The MiscUtils method getJAXBElement() cannot be used here because of the difference in the
			 * class name (DCEVSEStatus) and the name in the XSD (DC_EVSEStatus)
			 */
			JAXBElement jaxbACEVSEStatus = new JAXBElement(new QName("urn:iso:15118:2:2013:MsgDataTypes", "DC_EVSEStatus"), 
					DCEVSEStatusType.class, 
					getCommSessionContext().getDCEvseController().getDCEVSEStatus(EVSENotificationType.NONE));
			powerDeliveryRes.setEVSEStatus(jaxbACEVSEStatus);
		} else {
			getLogger().warn("RequestedEnergyTransferMode '" + getCommSessionContext().getRequestedEnergyTransferMode().toString() + 
										"is neither of type AC nor DC");
		}
	}
	
	
	private SAScheduleTupleType getChosenSASCheduleTuple(short chosenSAScheduleTupleID) {
		for (SAScheduleTupleType saSchedule : getCommSessionContext().getSaSchedules().getSAScheduleTuple()) {
			if (saSchedule.getSAScheduleTupleID() == chosenSAScheduleTupleID) return saSchedule;
		}
		return null;
	}
	
	
	private boolean isChargingProfileValid(
			SAScheduleTupleType chosenSAScheduleTuple, 
			ChargingProfileType chargingProfile) {
		// TODO check for validity of charging profile
		
		return true;
	}
	

	@Override
	protected void setMandatoryFieldsForFailedRes() {
		setEVSEStatus(powerDeliveryRes);
	}
}
