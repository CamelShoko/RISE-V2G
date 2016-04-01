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
package org.eclipse.risev2g.evcc.states;

import java.security.KeyStore;
import java.security.cert.X509Certificate;

import org.eclipse.risev2g.evcc.session.V2GCommunicationSessionEVCC;
import org.eclipse.risev2g.shared.enumerations.GlobalValues;
import org.eclipse.risev2g.shared.enumerations.V2GMessages;
import org.eclipse.risev2g.shared.messageHandling.ReactionToIncomingMessage;
import org.eclipse.risev2g.shared.messageHandling.TerminateSession;
import org.eclipse.risev2g.shared.utils.SecurityUtils;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.CertificateInstallationReqType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.CertificateUpdateReqType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.PaymentOptionType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.PaymentServiceSelectionResType;

public class WaitForPaymentServiceSelectionRes extends ClientState {
	
	public WaitForPaymentServiceSelectionRes(V2GCommunicationSessionEVCC commSessionContext) {
		super(commSessionContext);
	}

	@Override
	public ReactionToIncomingMessage processIncomingMessage(Object message) {
		if (isIncomingMessageValid(message, PaymentServiceSelectionResType.class)) {
			if (getCommSessionContext().getSelectedPaymentOption().equals(PaymentOptionType.CONTRACT)) {
				X509Certificate contractCert = SecurityUtils.getContractCertificate();
				
				/*
				 * 1. Check if certificate installation is needed
				 * No valid contract certificate means:
				 * - no contract certificate is stored, or
				 * - existing contract certificates are expired or revoked
				 */
				if (contractCert == null || (contractCert != null && !SecurityUtils.isCertificateValid(contractCert))) {
					if (getCommSessionContext().isCertificateServiceAvailable((short) 1)) {
						if (contractCert == null) getLogger().info("No contract certificate stored, trying to install contract certificate");
						else getLogger().info("Stored contract certificate not valid, trying to install new contract certificate");
						
						return getSendMessage(getCertificateInstallationReq(), V2GMessages.CERTIFICATE_INSTALLATION_RES);
					} else return new TerminateSession("Certificate installation needed but service is not available");
				} 
				
				// 2. Check if certificate update is needed (means: certificate is available but expires soon)
				short validityOfContractCert = SecurityUtils.getValidityPeriod(contractCert);
				
				if (validityOfContractCert <= GlobalValues.CERTIFICATE_EXPIRES_SOON_PERIOD.getShortValue()) {
					if (getCommSessionContext().isCertificateServiceAvailable((short) 2)) {
						getLogger().info("Stored contract certificate is about to expire in " + validityOfContractCert +
										 " days, trying to update contract certificate");
						return getSendMessage(getCertificateUpdateReq(), V2GMessages.CERTIFICATE_UPDATE_RES);
					} else return new TerminateSession("Certificate update needed but service is not available");
				} 
					
				return getSendMessage(getPaymentDetailsReq(), V2GMessages.PAYMENT_DETAILS_RES);
			} else if (getCommSessionContext().getSelectedPaymentOption().equals(PaymentOptionType.EXTERNAL_PAYMENT)) {
				return getSendMessage(getAuthorizationReq(null), V2GMessages.AUTHORIZATION_RES);
			} else {
				return new TerminateSession("No valid PaymentOptionType available");
			}
		} else {
			return new TerminateSession("Incoming message raised an error");
		}
	}
	
	
	private CertificateInstallationReqType getCertificateInstallationReq() {
		KeyStore evccKeyStore = SecurityUtils.getKeyStore(
				GlobalValues.EVCC_KEYSTORE_FILEPATH.toString(),
				GlobalValues.PASSPHRASE_FOR_CERTIFICATES_AND_KEYS.toString());
		
		CertificateInstallationReqType certInstallationReq = new CertificateInstallationReqType();
		certInstallationReq.setId("certificateInstallationReq");
		certInstallationReq.setListOfRootCertificateIDs(
				SecurityUtils.getListOfRootCertificateIDs(
						GlobalValues.EVCC_TRUSTSTORE_FILEPATH.toString(),
						GlobalValues.PASSPHRASE_FOR_CERTIFICATES_AND_KEYS.toString()));
		certInstallationReq.setOEMProvisioningCert(
				SecurityUtils.getCertificateChain(
						evccKeyStore, GlobalValues.ALIAS_OEM_PROV_CERTIFICATE.toString()).getCertificate());
		
		// Set xml reference element
		getXMLSignatureRefElements().put(certInstallationReq.getId(), SecurityUtils.generateDigest(certInstallationReq, false));
		
		// Set signing private key
		setSignaturePrivateKey(SecurityUtils.getPrivateKey(
				SecurityUtils.getKeyStore(
						GlobalValues.EVCC_KEYSTORE_FILEPATH.toString(),
						GlobalValues.PASSPHRASE_FOR_CERTIFICATES_AND_KEYS.toString()), 
				GlobalValues.ALIAS_OEM_PROV_CERTIFICATE.toString())
		);
		
		return certInstallationReq;
	}
	
	
	private CertificateUpdateReqType getCertificateUpdateReq() {
		CertificateUpdateReqType certificateUpdateReq = new CertificateUpdateReqType();
		certificateUpdateReq.setContractSignatureCertChain(
				SecurityUtils.getCertificateChain(
						SecurityUtils.getKeyStore(
								GlobalValues.EVCC_KEYSTORE_FILEPATH.toString(),
								GlobalValues.PASSPHRASE_FOR_CERTIFICATES_AND_KEYS.toString()), 
						GlobalValues.ALIAS_CONTRACT_CERTIFICATE.toString()));	
		certificateUpdateReq.setEMAID(SecurityUtils.getEMAID(GlobalValues.PASSPHRASE_FOR_CERTIFICATES_AND_KEYS.toString()).getValue());
		certificateUpdateReq.setId("certificateUpdateReq");
		certificateUpdateReq.setListOfRootCertificateIDs(
				SecurityUtils.getListOfRootCertificateIDs(
						GlobalValues.EVCC_TRUSTSTORE_FILEPATH.toString(),
						GlobalValues.PASSPHRASE_FOR_CERTIFICATES_AND_KEYS.toString()));
		
		// Set xml reference element
		getXMLSignatureRefElements().put(
				certificateUpdateReq.getId(), 
				SecurityUtils.generateDigest(certificateUpdateReq, false));
		
		// Set signing private key
		setSignaturePrivateKey(SecurityUtils.getPrivateKey(
				SecurityUtils.getKeyStore(
						GlobalValues.EVCC_KEYSTORE_FILEPATH.toString(),
						GlobalValues.PASSPHRASE_FOR_CERTIFICATES_AND_KEYS.toString()), 
				GlobalValues.ALIAS_CONTRACT_CERTIFICATE.toString())
		);
		
		return certificateUpdateReq;
	}
}
