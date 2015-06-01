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
package org.eclipse.risev2g.secc.states;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.risev2g.secc.session.V2GCommunicationSessionSECC;
import org.eclipse.risev2g.shared.enumerations.GlobalValues;
import org.eclipse.risev2g.shared.enumerations.V2GMessages;
import org.eclipse.risev2g.shared.messageHandling.ChangeProcessingState;
import org.eclipse.risev2g.shared.messageHandling.ReactionToIncomingMessage;
import org.eclipse.risev2g.shared.v2gMessages.SECCDiscoveryReq;
import org.eclipse.risev2g.shared.v2gMessages.appProtocol.AppProtocolType;
import org.eclipse.risev2g.shared.v2gMessages.appProtocol.ResponseCodeType;
import org.eclipse.risev2g.shared.v2gMessages.appProtocol.SupportedAppProtocolReq;
import org.eclipse.risev2g.shared.v2gMessages.appProtocol.SupportedAppProtocolRes;

public class WaitForSupportedAppProtocolReq extends ServerState {
	
	private SupportedAppProtocolRes supportedAppProtocolRes;
	
	public WaitForSupportedAppProtocolReq(V2GCommunicationSessionSECC commSessionContext) {
		super(commSessionContext);
	}
	
	@Override
	public ReactionToIncomingMessage processIncomingMessage(Object message) {
		supportedAppProtocolRes = new SupportedAppProtocolRes();
		
		if (message instanceof SupportedAppProtocolReq) {
			getLogger().debug("SupportedAppProtocolReq received");
			boolean match = false;
			ResponseCodeType responseCode = ResponseCodeType.FAILED_NO_NEGOTIATION;
			SupportedAppProtocolReq supportedAppProtocolReq = (SupportedAppProtocolReq) message;
			
			// The provided appProtocols might not be sorted by priority
			Collections.sort(supportedAppProtocolReq.getAppProtocol(), (appProtocol1, appProtocol2) ->
				Short.compare(appProtocol1.getPriority(), appProtocol2.getPriority()));
			
			/*
			 * If protocol and major version matches with more than one supported protocol,
			 * choose the one with highest priority
			 */
			for (AppProtocolType evccAppProtocol : supportedAppProtocolReq.getAppProtocol()) {
				/*
				 * A getSupportedAppProtocols().contains(evccAppProtocol) does not work here since 
				 * priority and schemaID are not provided in getSupportedAppProtocols()
				 */
				for (AppProtocolType seccAppProtocol : getSupportedAppProtocols()) {
					if (evccAppProtocol.getProtocolNamespace().equals(seccAppProtocol.getProtocolNamespace()) &&
						evccAppProtocol.getVersionNumberMajor() == seccAppProtocol.getVersionNumberMajor()) {
						if (evccAppProtocol.getVersionNumberMinor() == seccAppProtocol.getVersionNumberMinor()) {
							responseCode = ResponseCodeType.OK_SUCCESSFUL_NEGOTIATION;
						} else {
							responseCode = ResponseCodeType.OK_SUCCESSFUL_NEGOTIATION_WITH_MINOR_DEVIATION;
						}
						match = true;
						supportedAppProtocolRes.setSchemaID(evccAppProtocol.getSchemaID());
						break;
					}
				}
				
				if (match) break;
			}
				
			supportedAppProtocolRes.setResponseCode(responseCode);
		} else if (message instanceof SECCDiscoveryReq) {
			getLogger().debug("Another SECCDiscoveryReq was received, changing to state WaitForSECCDiscoveryReq");
			return new ChangeProcessingState(message, getCommSessionContext().getStates().get(V2GMessages.SECC_DISCOVERY_REQ));
		} else {
			getLogger().error("Invalid message (" + message.getClass().getSimpleName() + 
							  ") at this state (" + this.getClass().getSimpleName() + ")");
			supportedAppProtocolRes.setResponseCode(ResponseCodeType.FAILED_NO_NEGOTIATION);
		}
		
		return getSendMessage(supportedAppProtocolRes, 
							  (supportedAppProtocolRes.getResponseCode().toString().startsWith("OK") ? 
							  V2GMessages.SESSION_SETUP_REQ : V2GMessages.NONE)
							 );
	}
	
	
	/**
	 * All supported versions of the ISO/IEC 15118-2 protocol are listed here.
	 * Currently, only IS version of April 2014 is supported (see [V2G2-098]), more could be provided here.
	 * The values for priority and schema ID do not need to be set since these values are provided by
	 * the EVCC.
	 * 
	 * @return A list of supported of AppProtocol entries 
	 */
	private List<AppProtocolType> getSupportedAppProtocols() {
		List<AppProtocolType> supportedAppProtocols = new ArrayList<AppProtocolType>();
		
		AppProtocolType appProtocol1 = new AppProtocolType();
		appProtocol1.setProtocolNamespace(GlobalValues.V2G_CI_MSG_DEF_NAMESPACE.toString());
		appProtocol1.setVersionNumberMajor(2);
		appProtocol1.setVersionNumberMinor(0);
		
		supportedAppProtocols.add(appProtocol1);
		
		return supportedAppProtocols;
	}
	
}
