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
package org.eclipse.risev2g.secc.states;

import org.eclipse.risev2g.secc.session.V2GCommunicationSessionSECC;
import org.eclipse.risev2g.shared.enumerations.V2GMessages;
import org.eclipse.risev2g.shared.messageHandling.ReactionToIncomingMessage;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.ChargeServiceType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.ServiceCategoryType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.ServiceDiscoveryReqType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.ServiceDiscoveryResType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.ServiceListType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.ServiceType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.SupportedEnergyTransferModeType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.V2GMessage;

public class WaitForServiceDiscoveryReq extends ServerState {
	
	private ServiceDiscoveryResType serviceDiscoveryRes;
	
	public WaitForServiceDiscoveryReq(V2GCommunicationSessionSECC commSessionContext) {
		super(commSessionContext);
		serviceDiscoveryRes = new ServiceDiscoveryResType();
	}

	@Override
	public ReactionToIncomingMessage processIncomingMessage(Object message) {
		if (isIncomingMessageValid(message, ServiceDiscoveryReqType.class, serviceDiscoveryRes)) {
			V2GMessage v2gMessageReq = (V2GMessage) message;
			ServiceDiscoveryReqType serviceDiscoveryReq = (ServiceDiscoveryReqType) v2gMessageReq.getBody().getBodyElement().getValue();
			ServiceListType offeredVASList = getServiceList(
												serviceDiscoveryReq.getServiceCategory(), 
												serviceDiscoveryReq.getServiceScope()
											 );
			
			serviceDiscoveryRes.setPaymentOptionList(getCommSessionContext().getPaymentOptions());
			serviceDiscoveryRes.setChargeService(getChargeService()); 
			
			// The ServiceList itself is optional, but if you send it, it shall not be empty
			if (offeredVASList.getService().size() > 0) {
				serviceDiscoveryRes.setServiceList(offeredVASList);
			}
			
			/*
			 * When processing PaymentServiceSelectionReq the SECC needs to check if the service
			 * chosen by the EVCC was previously offered
			 */
			getCommSessionContext().getOfferedServices().add(getChargeService());
			getCommSessionContext().getOfferedServices().addAll(offeredVASList.getService());
			
			((ForkState) getCommSessionContext().getStates().get(V2GMessages.FORK))
				.getAllowedRequests().add(V2GMessages.SERVICE_DETAIL_REQ);
			((ForkState) getCommSessionContext().getStates().get(V2GMessages.FORK))
				.getAllowedRequests().add(V2GMessages.PAYMENT_SERVICE_SELECTION_REQ);
		} else {
			setMandatoryFieldsForFailedRes();
		}
		
		return getSendMessage(serviceDiscoveryRes, 
				  			  (serviceDiscoveryRes.getResponseCode().toString().startsWith("OK") ? 
				  			  V2GMessages.FORK : V2GMessages.NONE)
	 			 			 );
	}
	
	
	private ChargeServiceType getChargeService() {
		SupportedEnergyTransferModeType supportedEnergyTransferModes = new SupportedEnergyTransferModeType();
		supportedEnergyTransferModes.getEnergyTransferMode().addAll(
				getCommSessionContext().getSupportedEnergyTransferModes());
		
		ChargeServiceType chargeService = new ChargeServiceType();
		chargeService.setSupportedEnergyTransferMode(supportedEnergyTransferModes);
		chargeService.setServiceCategory(ServiceCategoryType.EV_CHARGING);
		chargeService.setServiceID(1); // according to Table 105 ISO/IEC 15118-2
		
		/*
		 * Is an optional value, but fill it with a non-empty string if used, 
		 * otherwise an EXI decoding error could occur on the other side!
		 */
		chargeService.setServiceName("AC_DC_Charging"); 
		
		/*
		 * Is an optional value, but fill it with a non-empty string if used, 
		 * otherwise an EXI decoding error could occur on the other side!
		 */
		chargeService.setServiceScope("chargingServiceScope");
		
		chargeService.setFreeService(false); // it is supposed that charging is by default not for free
		
		return chargeService;
	}
	
	
	private ServiceListType getServiceList(ServiceCategoryType serviceCategoryFilter, String serviceScopeFilter) {
		ServiceListType serviceList = new ServiceListType();
		
		if (serviceCategoryFilter != null)
			getLogger().debug("EVCC filters offered services by category: " + serviceScopeFilter.toString());
		
		// Currently no filter based on service scope is applied since its string value is not standardized somehow
		if (getCommSessionContext().isTlsConnection() && (
				(serviceCategoryFilter != null && serviceCategoryFilter.equals(ServiceCategoryType.CONTRACT_CERTIFICATE)) ||
				serviceCategoryFilter == null)) {
			serviceList.getService().add(getCertificateService());
		}
		
		/*
		 * If more VAS (value added service) services beyond the certificate installation/update service 
		 * are to be offered, then they could be listed here.  
		 */
		
		return serviceList;
	}
	
	
	private ServiceType getCertificateService() {
		ServiceType certificateService = new ServiceType();
		certificateService.setFreeService(true);
		certificateService.setServiceCategory(ServiceCategoryType.CONTRACT_CERTIFICATE);
		certificateService.setServiceID(2); // according to Table 105 ISO/IEC 15118-2
		certificateService.setServiceName("Certificate"); // optional value
		
		/*
		 * Is an optional value, but fill it with a non-empty string if used, 
		 * otherwise an EXI decoding error could occur on the other side!
		 */
		certificateService.setServiceScope("certificateServiceScope"); 
		
		return certificateService;
	}

	
	@Override
	protected void setMandatoryFieldsForFailedRes() {
		serviceDiscoveryRes.setChargeService(getChargeService());
		serviceDiscoveryRes.setPaymentOptionList(getCommSessionContext().getPaymentOptions());
	}
}
