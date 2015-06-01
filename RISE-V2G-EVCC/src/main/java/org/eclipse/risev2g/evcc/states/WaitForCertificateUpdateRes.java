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

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.HashMap;

import org.eclipse.risev2g.evcc.session.V2GCommunicationSessionEVCC;
import org.eclipse.risev2g.shared.enumerations.GlobalValues;
import org.eclipse.risev2g.shared.enumerations.V2GMessages;
import org.eclipse.risev2g.shared.messageHandling.ReactionToIncomingMessage;
import org.eclipse.risev2g.shared.messageHandling.TerminateSession;
import org.eclipse.risev2g.shared.utils.SecurityUtils;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.CertificateUpdateResType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.SignatureType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.V2GMessage;

public class WaitForCertificateUpdateRes extends ClientState {

	public WaitForCertificateUpdateRes(V2GCommunicationSessionEVCC commSessionContext) {
		super(commSessionContext);
	}

	@Override
	public ReactionToIncomingMessage processIncomingMessage(Object message) {
		if (isIncomingMessageValid(message, CertificateUpdateResType.class)) {
			V2GMessage v2gMessageRes = (V2GMessage) message;
			CertificateUpdateResType certificateUpdateRes = 
					(CertificateUpdateResType) v2gMessageRes.getBody().getBodyElement().getValue();
			
			if (!verifySignature(certificateUpdateRes, v2gMessageRes.getHeader().getSignature())) {
				return new TerminateSession("Signature verification failed");
			}
			
			ECPrivateKey contractCertPrivateKey = SecurityUtils.getPrivateKey(
					SecurityUtils.getKeyStore(
							GlobalValues.EVCC_KEYSTORE_FILEPATH.toString(),
							GlobalValues.PASSPHRASE_FOR_CERTIFICATES_AND_KEYS.toString()),
					GlobalValues.ALIAS_CONTRACT_CERTIFICATE.toString());
			
			// Save contract certificate chain
			if (!SecurityUtils.saveContractCertificateChain(
					GlobalValues.PASSPHRASE_FOR_CERTIFICATES_AND_KEYS.toString(),
					certificateUpdateRes.getContractSignatureCertChain(),
					SecurityUtils.decryptContractCertPrivateKey(
							certificateUpdateRes.getDHpublickey().getValue(), 
							certificateUpdateRes.getContractSignatureEncryptedPrivateKey().getValue(), 
							contractCertPrivateKey))) {
				return new TerminateSession("Contract certificate chain could not be saved");
			}
			
			return getSendMessage(getPaymentDetailsReq(), V2GMessages.PAYMENT_DETAILS_RES);
		} else {
			return new TerminateSession("Incoming message raised an error");
		}
	}
	
	
	private boolean verifySignature(CertificateUpdateResType certificateUpdateRes, SignatureType signature) {
		HashMap<String, byte[]> verifyXMLSigRefElements = new HashMap<String, byte[]>();
		verifyXMLSigRefElements.put(
				certificateUpdateRes.getContractSignatureCertChain().getId(),
				SecurityUtils.generateDigest(certificateUpdateRes.getContractSignatureCertChain(), false));
		verifyXMLSigRefElements.put(
				certificateUpdateRes.getContractSignatureEncryptedPrivateKey().getId(),
				SecurityUtils.generateDigest(certificateUpdateRes.getContractSignatureEncryptedPrivateKey(), false));
		verifyXMLSigRefElements.put(
				certificateUpdateRes.getDHpublickey().getId(),
				SecurityUtils.generateDigest(certificateUpdateRes.getDHpublickey(), false));
		verifyXMLSigRefElements.put(
				certificateUpdateRes.getEMAID().getId(),
				SecurityUtils.generateDigest(certificateUpdateRes.getEMAID(), false));
				
		ECPublicKey ecPublicKey = (ECPublicKey) SecurityUtils.getCertificate(
				certificateUpdateRes.getSAProvisioningCertificateChain().getCertificate())
				.getPublicKey();
		if (!SecurityUtils.verifySignature(signature, verifyXMLSigRefElements, ecPublicKey)) {
			return false;
		}
		
		return true;
	}
}
