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
package org.eclipse.risev2g.shared.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;
import javax.xml.bind.JAXBElement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.risev2g.shared.enumerations.GlobalValues;
import org.eclipse.risev2g.shared.exiCodec.ExiCodec;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.CanonicalizationMethodType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.CertificateChainType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.ContractSignatureEncryptedPrivateKeyType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.DiffieHellmanPublickeyType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.DigestMethodType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.EMAIDType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.ListOfRootCertificateIDsType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.ReferenceType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.SignatureMethodType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.SignatureType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.SignedInfoType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.SubCertificatesType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.TransformType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.TransformsType;
import org.eclipse.risev2g.shared.v2gMessages.msgDef.X509IssuerSerialType;

public final class SecurityUtils {

	static Logger logger = LogManager.getLogger(SecurityUtils.class.getSimpleName());
	static ExiCodec exiCodec;
	
	public static enum ContractCertificateStatus {
		UPDATE_NEEDED,
		INSTALLATION_NEEDED,
		OK,
		UNKNOWN // is used as default for communication session context
	}
	
	public static Logger getLogger() {
		return logger;
	}
	
	
	/**
	 * Returns the standard JKS keystore which holds the respective credentials (private key and 
	 * certificate chain) for the EVCC or SECC (whoever calls this method).
	 * 
	 * @param keyStorePath The relative path and file name of the keystore 
	 * @param keyStorePassword The password which protects the keystore
	 * @return The respective keystore
	 */
	public static KeyStore getKeyStore(String keyStorePath, String keyStorePassword) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(keyStorePath);
			return getKeyStore(fis, keyStorePassword, "jks");
		} catch (FileNotFoundException e) {
			getLogger().error("FileNotFoundException occurred while trying to access keystore at location '" +
							  keyStorePath + "'");
			return null;
		}
	}
	
	/**
	 * Returns the standard JKS truststore which holds the respective trusted certificates for the EVCC 
	 * or SECC (whoever calls this method).
	 * 
	 * @param trustStorePath The relative path and file name of the truststore
	 * @param trustStorePassword The password which protects the truststore
	 * @return The respective truststore
	 */
	public static KeyStore getTrustStore(String trustStorePath, String trustStorePassword) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(trustStorePath);
			return getKeyStore(fis, trustStorePassword, "jks");
		} catch (FileNotFoundException e) {
			getLogger().error("FileNotFoundException occurred while trying to access keystore at location '" +
							  trustStorePath + "'");
			return null;
		}
	}
	
	
	/**
	 * Returns a PKCS#12 container which holds the respective credentials (private key and certificate chain)
	 * 
	 * @param pkcs12Path The relative path and file name of the PKCS#12 container
	 * @param password The password which protects the PKCS#12 container
	 * @return The respective keystore
	 */
	public static KeyStore getPKCS12KeyStore(String pkcs12Path, String password) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(pkcs12Path);
			return getKeyStore(fis, password, "pkcs12");
		} catch (FileNotFoundException e) {
			getLogger().error("FileNotFoundException occurred while trying to access PKCS#12 container at " +
							  "location '" + pkcs12Path + "'");
			return null;
		}
	}


	/**
	 * Returns a standard keystore which holds the respective credentials (private key and certificate chain).
	 * 
	 * @param keyStoreIS The input stream of the keystore
	 * @param keyStorePassword The password which protects the keystore
	 * @param keyStoreType The type of the keystore, either "jks" or "pkcs12"
	 * @return The respective keystore
	 */
	private static KeyStore getKeyStore(InputStream keyStoreIS, String keyStorePassword, String keyStoreType) {
		KeyStore keyStore = null;
		
		try {
			keyStore = KeyStore.getInstance(keyStoreType);
			keyStore.load(keyStoreIS, keyStorePassword.toCharArray());
			keyStoreIS.close();
			return keyStore;
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | 
				IOException | NullPointerException e) {
			getLogger().error(e.getClass().getSimpleName() + " occurred while trying to load keystore", e);
		} 
		
		return null;
	}
	
	
	/**
	 * Checks whether the given certificate is currently valid. 
	 * 
	 * @param certificate The X509Certificiate to be checked for validity
	 * @return True, if the current date lies within the notBefore and notAfter attribute of the 
	 * 		   certificate, false otherwise
	 */
	public static boolean isCertificateValid(X509Certificate certificate) {
		try {
			certificate.checkValidity();		
			return true;
		} catch (CertificateExpiredException e) {
			X500Principal subject = certificate.getSubjectX500Principal();
			
			getLogger().warn("Certificate with distinguished name '" + subject.getName().toString() + 
							 "' already expired (not after " + certificate.getNotAfter().toString() + ")");
		} catch (CertificateNotYetValidException e) {
			X500Principal subject = certificate.getSubjectX500Principal();
			getLogger().warn("Certificate with distinguished name '" + subject.getName().toString() + 
							 "' not yet valid (not before " + certificate.getNotBefore().toString() + ")");
		} 
		
		return false;
	}
	
	
	/**
	 * 
	 * [V2G2-925] states:
	 * A leaf certificate shall be treated as invalid, if the trust anchor at the end of the chain does not
	 * match the specific root certificate required for a certain use, or if the required Domain
	 * Component value is not present.
	 * 
	 * Domain Component restrictions:
	 * - SECC certificate: "CPO" (verification by EVCC) 
	 * - provisioning certificate (signer certificate of a contract certificate: "CPS" (verification by EVCC)
	 * - OEM Provisioning Certificate: "OEM" (verification by provisioning service (not EVCC or SECC))
	 * 
	 * @param certificate The X509Certificiate to be checked for validity
	 * @param domainComponent The domain component to be checked for in the distinguished name of the certificate
	 * @return True, if the current date lies within the notBefore and notAfter attribute of the 
	 * 		   certificate and the given domain component is present in the distinguished name, false otherwise
	 */
	public static boolean isCertificateValid(X509Certificate certificate, String domainComponent) {
		if (isCertificateValid(certificate)) {
			String dn = certificate.getSubjectX500Principal().getName();
			LdapName ln;
			
			try {
				ln = new LdapName(dn);
				
				for (Rdn rdn : ln.getRdns()) {
				    if (rdn.getType().equalsIgnoreCase("DC") && rdn.getValue().equals(domainComponent)) {
				        return true;
				    }
				}
			} catch (InvalidNameException e) {
				getLogger().warn("InvalidNameException occurred while trying to check domain component of certificate", e);
			}

			return false;
		} else return false;
	}
	
	
	/**
	 * Checks how many days a given certificate is still valid. 
	 * If the certificate is not valid any more, a negative number will be returned according to the number
	 * of days the certificate is already expired.
	 * 
	 * @param certificate The X509Certificiate to be checked for validity period
	 * @return The number of days the given certificate is still valid, a negative number if already expired.
	 */
	public static short getValidityPeriod(X509Certificate certificate) {
		Date today = Calendar.getInstance().getTime();
		Date certificateExpirationDate = certificate.getNotAfter();
		long diff = certificateExpirationDate.getTime() - today.getTime();
		
		return (short) TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
	}
	

	/**
	 * Checks whether each certificate in the given certificate chain is currently valid. 

	 * @param certChain The certificate chain to iterate over to check for validity
	 * @return True, if the current date lies within the notBefore and notAfter attribute of each 
	 * 		   certificate contained in the provided certificate chain, false otherwise
	 */
	public static boolean isCertificateChainValid(CertificateChainType certChain) {
		if (certChain == null) {
			getLogger().error("Certificate chain is NULL");
			return false;
		}
		
		if (!isCertificateValid(getCertificate(certChain.getCertificate()))) 
			return false;
		
		SubCertificatesType subCertificates = certChain.getSubCertificates();
		for (byte[] cert : subCertificates.getCertificate()) {
			if (!isCertificateValid(getCertificate(cert))) return false;
		}
		
		return true;
	}
	
	/**
	 * Checks whether each certificate in the given certificate chain is currently valid and if a given
	 * domain component (DC) in the distinguished name is set.
	 *
	 * @param certChain The certificate chain to iterate over to check for validity
	 * @param domainComponent The domain component 
	 * @return True, if the domain component is correctly set and if the current date lies within the notBefore 
	 * 		   and notAfter attribute of each certificate contained in the provided certificate chain, 
	 * 		   false otherwise
	 */
	public static boolean isCertificateChainValid(CertificateChainType certChain, String domainComponent) {
		if (isCertificateChainValid(certChain)) {
			if (isCertificateValid(getCertificate(certChain.getCertificate()), domainComponent)) return true;
			else return false;
		} else return false;
	}
	
	
	/**
	 * Verifies that the given certificate was signed using the private key that corresponds to the 
	 * public key of the provided certificate.
	 * 
	 * @param certificate The X509Certificate which is to be checked
	 * @param issuingCertificate The X.509 certificate which holds the public key corresponding to the private 
	 * 		  key with which the given certificate should have been signed
	 * @return True, if the verification was successful, false otherwise
	 */
	public static boolean isCertificateVerified(X509Certificate certificate, X509Certificate issuingCertificate) {
		X500Principal subject = certificate.getSubjectX500Principal();
		X500Principal expectedIssuerSubject = certificate.getIssuerX500Principal();
		X500Principal issuerSubject = issuingCertificate.getSubjectX500Principal();
		PublicKey publicKeyForSignature = issuingCertificate.getPublicKey();
		
		try {
			certificate.verify(publicKeyForSignature);
			return true;
		} catch (InvalidKeyException | CertificateException | NoSuchAlgorithmException | 
				 NoSuchProviderException | SignatureException e) {
			getLogger().warn("Signature verification of certificate having distinguished name '" + 
							  subject.getName() + "' with certificate having distinguished name (the issuer) '" + 
							  issuerSubject.getName() + "' failed. Expected issuer has distinguished name '" +
							  expectedIssuerSubject.getName() + "' (" + e.getClass().getSimpleName() + ")", e);
		} 
		
		return false;
	}
	
	
	/**
	 * Verifies for each certificate in the given certificate chain that it was signed using the private key 
	 * that corresponds to the public key of a certificate contained in the certificate chain or the truststore.
	 * 
	 * @param trustStoreFileName The relative path and file name of the truststore 
	 * @param certChain The certificate chain holding the leaf certificate and zero or more intermediate 
	 * 		  certificates (sub CAs) 
	 * @return True, if the verification was successful, false otherwise
	 */
	public static boolean isCertificateChainVerified(String trustStoreFileName, CertificateChainType certChain) {
		X509Certificate issuingCertificate = null; 
		
		if (certChain != null) {
			X509Certificate leafCertificate = getCertificate(certChain.getCertificate());
			if (leafCertificate != null) {
				SubCertificatesType subCertificates = certChain.getSubCertificates();
				if (subCertificates != null) {
					// Sub certificates must be in the right order (leaf -> SubCA2 -> SubCA1 -> ... -> RootCA)
					issuingCertificate = getCertificate(subCertificates.getCertificate().get(0));
					if (!isCertificateVerified(leafCertificate, issuingCertificate)) return false;
					
					for (int i=0; i < subCertificates.getCertificate().size(); i++) {
						if ((i+1) < subCertificates.getCertificate().size()) {
							issuingCertificate = getCertificate(subCertificates.getCertificate().get(i+1));
							if (!isCertificateVerified(getCertificate(subCertificates.getCertificate().get(i)), issuingCertificate)) 
								return false;
						} else {
							if (isCertificateTrusted(trustStoreFileName, getCertificate(subCertificates.getCertificate().get(i)))) return true;
							else return false;
						}
					}
				} else {
					if (!isCertificateTrusted(trustStoreFileName, leafCertificate)) return false;
				}
			} else {
				getLogger().error("No leaf certificate available in provided certificate chain, " + 
								  "therefore no verification possible");
				return false;
			}
		} else {
			getLogger().error("Provided certificate chain is null, could therefore not be verified");
			return false;
		}
		
		return false;
	}
	
	
	/**
	 * Iterates over the certificates stored in the truststore to check if one of the respective public
	 * keys of the certificates is the corresponding key to the private key with which the provided 
	 * certificate has been signed.
	 * 
	 * @param trustStoreFilename The relative path and file name of the truststore
	 * @param certificate The certificate whose signature needs to be signed
	 * @return True, if the provided certificate has been signed by one of the certificates in the 
	 * 		   truststore, false otherwise
	 */
	public static boolean isCertificateTrusted(String trustStoreFilename, X509Certificate certificate) {
		/*
		 * Use one of the root certificates in the truststore to verify the signature of the
		 * last certificate in the chain
		 */
		KeyStore trustStore = SecurityUtils.getTrustStore(trustStoreFilename, GlobalValues.PASSPHRASE_FOR_CERTIFICATES_AND_KEYS.toString());
		X500Principal expectedIssuer = certificate.getIssuerX500Principal();
		
		try {
			Enumeration<String> aliases = trustStore.aliases();
			while (aliases.hasMoreElements()) {
				X509Certificate rootCA = (X509Certificate) trustStore.getCertificate(aliases.nextElement());
				if (rootCA.getSubjectX500Principal().getName().equals(expectedIssuer.getName()) &&
					isCertificateVerified(certificate, rootCA)) return true;
			}
		} catch (KeyStoreException | NullPointerException e) {
			getLogger().error(e.getClass().getSimpleName() + " occurred while trying to verify trust " +
							  " status of certificate with distinguished name '" + 
							  certificate.getSubjectX500Principal().getName() + "' with truststore at " +
							  "location '" + trustStoreFilename + "'", e);
		}
		
		return false;
	}
	
	
	/**
	 * Returns the leaf certificate from a given certificate chain.
	 * 
	 * @param certChain The certificate chain given as an array of Certificate instances
	 * @return The leaf certificate (begin not a CA)
	 */
	public static X509Certificate getLeafCertificate(Certificate[] certChain) {
		for (Certificate cert : certChain) {
			X509Certificate x509Cert = (X509Certificate) cert;
			// Check whether the pathLen constraint is set which indicates if this certificate is a CA
			if (x509Cert.getBasicConstraints() == -1) return x509Cert;
		}
		
		getLogger().warn("No leaf certificate found in given certificate chain");
		return null;
	}
	
	
	/**
	 * Returns the intermediate certificates (sub CAs) from a given certificate chain.
	 * 
	 * @param certChain The certificate chain given as an array of Certificate instances
	 * @return The sub certificates given as a list of byte arrays contained in a SubCertiticatesType instance
	 */
	public static SubCertificatesType getSubCertificates(Certificate[] certChain) {
		SubCertificatesType subCertificates = new SubCertificatesType();
		
		for (Certificate cert : certChain) {
			X509Certificate x509Cert = (X509Certificate) cert;
			// Check whether the pathLen constraint is set which indicates if this certificate is a CA
			if (x509Cert.getBasicConstraints() != -1)
				try {
					subCertificates.getCertificate().add(x509Cert.getEncoded());
				} catch (CertificateEncodingException e) {
					X500Principal subject = x509Cert.getIssuerX500Principal();
					getLogger().error("A CertificateEncodingException occurred while trying to get certificate " +
									  "with distinguished name '" + subject.getName().toString() + "'", e);
				}
		}
		
		if (subCertificates.getCertificate().size() == 0) {
			getLogger().warn("No intermediate CAs found in given certificate array");
		}
		
		return subCertificates;
	}
	
	
	/**
	 * Returns the list of X509IssuerSerialType instances of the root CAs contained in the truststore.
	 * 
	 * @param trustStoreFileName The relative path and file name of the truststore
	 * @param trustStorePassword The password which protects the truststore
	 * @return The list of X509IssuerSerialType instances of the root CAs
	 */
	public static ListOfRootCertificateIDsType getListOfRootCertificateIDs(
			String trustStoreFileName,
			String trustStorePassword) {
		KeyStore evccTrustStore = getTrustStore(trustStoreFileName, trustStorePassword);
		ListOfRootCertificateIDsType rootCertificateIDs = new ListOfRootCertificateIDsType();
		
		X509Certificate cert = null;
		try {
			Enumeration<String> aliases = evccTrustStore.aliases();
			while (aliases.hasMoreElements()) {
				cert = (X509Certificate) evccTrustStore.getCertificate(aliases.nextElement());
				X509IssuerSerialType serialType = new X509IssuerSerialType();
				serialType.setX509IssuerName(cert.getIssuerX500Principal().getName());
				serialType.setX509SerialNumber(cert.getSerialNumber());
				rootCertificateIDs.getRootCertificateID().add(serialType);
			}
		} catch (KeyStoreException | NullPointerException e) {
			getLogger().error(e.getClass().getSimpleName() + " occurred while trying to get list of " +
							  "root certificate IDs from truststore at location '" + trustStoreFileName + "'", e);
		}
		
		return rootCertificateIDs;
	}
	
	
	/**
	 * Returns an instance of a X.509 certificate created from its raw byte array
	 * 
	 * @param certificate The byte array representing a X.509 certificate
	 * @return The X.509 certificate
	 */
	public static X509Certificate getCertificate(byte[] certificate) {
		X509Certificate cert = null;
		
		try {
			InputStream in = new ByteArrayInputStream(certificate);
			CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
			cert = (X509Certificate) certFactory.generateCertificate(in);
		} catch (CertificateException e) {
			getLogger().error("CertificateException occurred when trying to create X.509 certificate from byte array", e);
		}
		
		return cert;
	}
	
	
	/**
	 * Returns the mobility operator sub 2 certificate (MOSub2Certificate) which signs the contract 
	 * certificate from the given keystore. The MOSub2Certificate is then used to verify the signature of
	 * sales tariffs.
	 * 
	 * @param keyStoreFileName The relative path and file name of the keystore
	 * @return The X.509 mobility operator sub 2 certficiate (a certificate from a sub CA)
	 */
	public static X509Certificate getMOSub2Certificate(String keyStoreFileName) {
		KeyStore keystore = getKeyStore(keyStoreFileName, GlobalValues.PASSPHRASE_FOR_CERTIFICATES_AND_KEYS.toString());
		X509Certificate moSub2Certificate = null;
		
		try {
			Certificate[] certChain = keystore.getCertificateChain(GlobalValues.ALIAS_CONTRACT_CERTIFICATE.toString());
			X509Certificate contractCertificate = getLeafCertificate(certChain);
			SubCertificatesType subCertificates = getSubCertificates(certChain); 
			
			for (byte[] certificate : subCertificates.getCertificate()) {
				X509Certificate x509Cert = getCertificate(certificate);
				if (contractCertificate.getIssuerX500Principal().getName().equals(
					x509Cert.getSubjectX500Principal().getName())) {
					moSub2Certificate = x509Cert;
					break;
				}
			}
		} catch (KeyStoreException e) {
			getLogger().error("KeyStoreException occurred while trying to get MOSub2 certificate");
		}
		
		return moSub2Certificate;
	}

	
	/**
	 * Returns the ECPublicKey instance from its raw bytes
	 * 
	 * @param dhPublicKeyBytes The byte array representing the ECPublicKey instance
	 * @return The ECPublicKey instance
	 */
	public static ECPublicKey getPublicKey(byte[] publicKeyBytes) {
	    try {
	    	X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
	    	ECPublicKey publicKey = (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(keySpec);
	    	return publicKey;
	    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
	        getLogger().error(e.getClass().getSimpleName() + " occurred when trying to get public key from raw bytes", e);
	        return null;
	    }
	}
	
	
	/**
	 * Returns the public key part of an elliptic curve Diffie-Hellman keypair
	 * 
	 * @param ecdhKeyPair The elliptic curve Diffie-Hellman keypair
	 * @return The respective public key
	 */
	public static DiffieHellmanPublickeyType getDHPublicKey(KeyPair ecdhKeyPair) {
		DiffieHellmanPublickeyType dhPublicKey = new DiffieHellmanPublickeyType();
		dhPublicKey.setId("dhPublicKey"); 
		dhPublicKey.setValue(ecdhKeyPair.getPublic().getEncoded());
		
		return dhPublicKey;
	}
	
	
	/**
	 * Returns the ECPrivateKey instance from its raw bytes
	 * 
	 * @param privateKeyBytes The byte array representing the ECPrivateKey instance
	 * @return The ECPrivateKey instance
	 */
	public static ECPrivateKey getPrivateKey(byte[] privateKeyBytes) {
		try {
			AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
			parameters.init(new ECGenParameterSpec("secp256r1"));
			
			ECParameterSpec ecParameterSpec = parameters.getParameterSpec(ECParameterSpec.class);
			ECPrivateKeySpec ecPrivateKeySpec = new ECPrivateKeySpec(new BigInteger(privateKeyBytes), ecParameterSpec);
			
			ECPrivateKey privateKey = (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(ecPrivateKeySpec);
			return privateKey;
		} catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidParameterSpecException e) {
			getLogger().error(e.getClass().getSimpleName() + " occurred when trying to get private key from raw bytes", e);
			return null;
		}
	}
	
	
	/**
	 * Searches the given keystore for the private key. It is assumed that the given keystore holds
	 * only one private key entry whose alias is not known before, which is the case during certificate
	 * installation when the SECC receives a PKCS#12 container from a secondary actor encapsulating the 
	 * contract certificate, its private key and an optional chain of intermediate CAs.
	 * 
	 * @param keyStore The PKCS#12 keystore provided by the secondary actor
	 * @return The private key contained in the given keystore
	 */
	public static ECPrivateKey getPrivateKey(KeyStore keyStore) {
		/*
		 * For testing purposes, the respective PKCS12 container file chain has already been put in the 
		 * resources folder. However, when implementing a real interface to a secondary actor's backend, 
		 * the retrieval of a PKCS12 container file must be done via some other online mechanism.
		 */
		
		ECPrivateKey privateKey = null;
		
		try {
			Enumeration<String> aliases = keyStore.aliases();
			// Only one certificate chain (and therefore alias) should be available
			while (aliases.hasMoreElements()) {
				privateKey = (ECPrivateKey) keyStore.getKey(
						aliases.nextElement(), 
						GlobalValues.PASSPHRASE_FOR_CERTIFICATES_AND_KEYS.toString().toCharArray());
			}
		} catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException | 
				 NullPointerException e) {
			getLogger().error(e.getClass().getSimpleName() + " occurred while trying to get private " +
							  "key from keystore", e);
		}
		
		return privateKey;
	}
	
	
	/**
	 * Searches the given keystore for the private key which corresponds to the provided alias.
	 * Example: In case of the EVCC and during certificate installation, the private key of the
	 * OEM provisioning certificate is needed. During certificate update, the private key of the 
	 * existing contract certificate is needed.
	 * 
	 * @param keyStore The keystore of EVCC or SECC
	 * @param alias The alias of a specific private key entry
	 * @return The private key corresponding to the respective alias in the given keystore
	 */
	public static ECPrivateKey getPrivateKey(KeyStore keyStore, String alias) {
		ECPrivateKey privateKey = null;
		
		try {
			privateKey = (ECPrivateKey) keyStore.getKey(
						alias, 
						GlobalValues.PASSPHRASE_FOR_CERTIFICATES_AND_KEYS.toString().toCharArray());
		} catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException e) {
			getLogger().error("The private key from keystore with alias '" + alias + 
							  "' could not be retrieved (" + e.getClass().getSimpleName() + ")", e);
		}
		
		return privateKey;
	}

	
	/**
	 * Returns the SecretKey instance from its raw bytes
	 * 
	 * @param key The byte array representing the symmetric SecretKey instance
	 * @return The SecretKey instance
	 */
	public static SecretKey getSecretKey(byte[] key) {
		SecretKey secretKey = new SecretKeySpec(key, 0, key.length, "DiffieHellman");
		
		return secretKey;
	}
	
	
	/**
	 * Returns the certificate chain from a PKCS#12 container holding credentials such as private key,
	 * leaf certificate and zero or more intermediate certificates.
	 * 
	 * @param pkcs12Resource The PKCS#12 container
	 * @return The certificate chain
	 */
	public static CertificateChainType getCertificateChain(String pkcs12Resource) {
		CertificateChainType certChain = new CertificateChainType();
		
		/*
		 * For testing purposes, the respective PKCS12 container file has already been put in the 
		 * resources folder. However, when implementing a real interface to a secondary actor's backend, 
		 * the retrieval of a certificate must be done via some other online mechanism.
		 */
		KeyStore contractCertificateKeystore = getPKCS12KeyStore(pkcs12Resource, GlobalValues.PASSPHRASE_FOR_CERTIFICATES_AND_KEYS.toString());
		
		if (contractCertificateKeystore == null) {
			getLogger().error("Unable to access certificate chain because no PKCS#12 container found at " +
							  "location '" + pkcs12Resource + "'");
			return null;
		}
		
		try {
			Enumeration<String> aliases = contractCertificateKeystore.aliases();
			Certificate[] tempCertChain = null;
			// Only one certificate chain (and therefore alias) should be available
			while (aliases.hasMoreElements()) {
				tempCertChain = contractCertificateKeystore.getCertificateChain(aliases.nextElement());
				certChain.setCertificate(getLeafCertificate(tempCertChain).getEncoded());
				certChain.setSubCertificates(getSubCertificates(tempCertChain));
			}
		} catch (KeyStoreException | CertificateEncodingException | NullPointerException e) {
			getLogger().error(e.getClass().getSimpleName() + " occurred while  trying to get " +
							  "certificate chain from resource '" + pkcs12Resource + "'", e);
		}
		
		return certChain;
	}
	
	
	/**
	 * Returns the SignedInfo element of the V2GMessage header, based on the provided HashMap which holds
	 * the reference IDs (URIs) and the corresponding SHA-256 digests.
	 * 
	 * @param xmlSignatureRefElements A HashMap of Strings (reflecting the reference IDs) and digest values
	 * @return The SignedInfoType instance
	 */
	public static SignedInfoType getSignedInfo(HashMap<String, byte[]> xmlSignatureRefElements) {
		/*
		 * According to requirement [V2G2-771] in ISO/IEC 15118-2 the following messages elements of the 
		 * XML signature framework shall not be used:
		 * - Id (attribute in SignedInfo)
 		 * - ##any in SignedInfo – CanonicalizationMethod
 		 * - HMACOutputLength in SignedInfo – SignatureMethod
 		 * - ##other in SignedInfo – SignatureMethod
 		 * - Type (attribute in SignedInfo-Reference)
 		 * - ##other in SignedInfo – Reference – Transforms – Transform
 		 * - XPath in SignedInfo – Reference – Transforms – Transform
 		 * - ##other in SignedInfo – Reference – DigestMethod
 		 * - Id (attribute in SignatureValue)
 		 * - Object (in Signature)
 		 * - KeyInfo
		 */
		DigestMethodType digestMethod = new DigestMethodType();
		digestMethod.setAlgorithm("http://www.w3.org/2001/04/xmlenc#sha256");
		
		TransformType transform = new TransformType();
		transform.setAlgorithm("http://www.w3.org/TR/canonical-exi");
		TransformsType transforms = new TransformsType();
		transforms.getTransform().add(transform);
		
		List<ReferenceType> references = new ArrayList<ReferenceType>();
		xmlSignatureRefElements.forEach( (k,v) -> {
			ReferenceType reference = new ReferenceType();
			reference.setDigestMethod(digestMethod);
			reference.setDigestValue(v);
			reference.setId(k);
			reference.setTransforms(transforms);
			reference.setURI("#" + k);
			
			references.add(reference);
		});
		
		CanonicalizationMethodType canonicalizationMethod = new CanonicalizationMethodType();
		canonicalizationMethod.setAlgorithm("http://www.w3.org/TR/canonical-exi");
		
		SignatureMethodType signatureMethod = new SignatureMethodType(); 
		signatureMethod.setAlgorithm("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256");
		
		SignedInfoType signedInfo = new SignedInfoType();
		signedInfo.setCanonicalizationMethod(canonicalizationMethod);
		signedInfo.setSignatureMethod(signatureMethod);
		signedInfo.getReference().addAll(references);
		
		return signedInfo;
	}
	
	
	/**
	 * Saves the newly received contract certificate chain, provided by CertificateInstallationRes or 
	 * CertificateUpdateRes.
	 * 
	 * @param keyStorePassword The password which protects the EVCC keystore
	 * @param contractCertChain The certificate chain belonging to the contract certificate
	 * @param contractCertPrivateKey The private key corresponding to the public key of the leaf certificate 
	 * 								 stored in the certificate chain
	 * @return True, if the contract certificate chain and private key could be saved, false otherwise
	 */
	public static boolean saveContractCertificateChain(
			String keyStorePassword, 
			CertificateChainType contractCertChain,
			PrivateKey contractCertPrivateKey) {
		KeyStore keyStore = getKeyStore(GlobalValues.EVCC_KEYSTORE_FILEPATH.toString(), keyStorePassword);

		try {
			keyStore.setKeyEntry(
					GlobalValues.ALIAS_CONTRACT_CERTIFICATE.toString(), 
					contractCertPrivateKey, 
					keyStorePassword.toCharArray(), 
					getCertificateChain(contractCertChain)); 
			
			// Save the keystore persistently
			FileOutputStream fos = new FileOutputStream("evccKeystore.jks");
			keyStore.store(fos, GlobalValues.PASSPHRASE_FOR_CERTIFICATES_AND_KEYS.toString().toCharArray());
			fos.close();
			
			X509Certificate contractCert = getCertificate(contractCertChain.getCertificate());
			
			getLogger().info("Contract certificate with distinguished name '" + 
							 contractCert.getSubjectX500Principal().getName() + "' saved. " + 
							 "Valid until " + contractCert.getNotAfter()
							 ); 
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | NullPointerException e) {
			getLogger().error(e.getClass().getSimpleName() + " occurred while trying to save contract " +
							  "certificate chain", e);
			return false;
		}
		
		return true;
	}
	
	
	/**
	 * Gets the contract certificate from the EVCC keystore.
	 * 
	 * @return The contract certificate if present, null otherwise
	 */
	public static X509Certificate getContractCertificate() {
		X509Certificate contractCertificate = null;
		
		KeyStore evccKeyStore = getKeyStore(
				GlobalValues.EVCC_KEYSTORE_FILEPATH.toString(), 
				GlobalValues.PASSPHRASE_FOR_CERTIFICATES_AND_KEYS.toString()
			);
		 
		try {
			contractCertificate = (X509Certificate) evccKeyStore.getCertificate(GlobalValues.ALIAS_CONTRACT_CERTIFICATE.toString());
		} catch (KeyStoreException e) {
			getLogger().error("KeyStoreException occurred while trying to get contract certificate from keystore", e);
		}
		
		return contractCertificate;
	}
	
	
	/**
	 * A convenience function which checks if a contract certificate installation is needed.
	 * Normally not needed because of function getContractCertificateStatus().
	 * 
	 * @return True, if no contract certificate is store or if the stored certificate is not valid, false otherwise
	 */
	public static boolean isContractCertificateInstallationNeeded() {
		X509Certificate contractCert = getContractCertificate();
		
		if (contractCert == null) {
			getLogger().info("No contract certificate stored");
			return true;
		} else if (contractCert != null && !isCertificateValid(contractCert)) {
			getLogger().info("Stored contract certificate with distinguished name '" + 
							 contractCert.getSubjectX500Principal().getName() + "' is not valid");
			return true;
		} else return false;
	}
	
	
	/**
	 * A convenience function which checks if a contract certificate update is needed.
	 * Normally not needed because of function getContractCertificateStatus().
	 * 
	 * @return True, if contract certificate is still valid but about to expire, false otherwise.
	 * 		   The expiration period is given in GlobalValues.CERTIFICATE_EXPIRES_SOON_PERIOD.
	 */
	public static boolean isContractCertificateUpdateNeeded() {
		X509Certificate contractCert = getContractCertificate();
		short validityOfContractCert = getValidityPeriod(contractCert);
		
		if (validityOfContractCert < 0) {
			getLogger().warn("Contract certificate with distinguished name '" + 
							 contractCert.getSubjectX500Principal().getName() + "' is not valid any more, expired " + 
							 Math.abs(validityOfContractCert) + " days ago");
			return false;
		} else if (validityOfContractCert <= GlobalValues.CERTIFICATE_EXPIRES_SOON_PERIOD.getShortValue()) {
			getLogger().info("Contract certificate with distinguished name '" + 
							 contractCert.getSubjectX500Principal().getName() + "' is about to expire in " + 
							 validityOfContractCert + " days");
			return true;
		} else return false;
	}
	
	
	/**
	 * Checks whether a contract certificate 
	 * - is stored
	 * - in case it is stored, if it is valid
	 * - in case it is valid, if it expires soon
	 * 
	 * This method is intended to reduce cryptographic computation overhead by checking both, if installation or
	 * update is needed, at the same time. When executing either method by itself (isContractCertificateUpdateNeeded() and
	 * isContractCertificateInstallationNeeded()), each time the certificate is read anew from the Java keystore
	 * holding the contract certificate. With this method the contract certificate is read just once from the keystore.
	 * 
	 * @return An enumeration value ContractCertificateStatus (either UPDATE_NEEDED, INSTALLATION_NEEDED, or OK)
	 */
	public static ContractCertificateStatus getContractCertificateStatus() {
		X509Certificate contractCert = getContractCertificate();
		
		if (contractCert == null) {
			getLogger().info("No contract certificate stored");
			return ContractCertificateStatus.INSTALLATION_NEEDED;
		} else if (contractCert != null && !isCertificateValid(contractCert)) {
			getLogger().info("Stored contract certificate with distinguished name '" + 
							 contractCert.getSubjectX500Principal().getName() + "' is not valid");
			return ContractCertificateStatus.INSTALLATION_NEEDED;
		} else {
			short validityOfContractCert = getValidityPeriod(contractCert);
			// Checking for a negative value of validityOfContractCert is not needed because the method
			// isCertificateValid() already checks for that
			if (validityOfContractCert <= GlobalValues.CERTIFICATE_EXPIRES_SOON_PERIOD.getShortValue()) {
				getLogger().info("Contract certificate with distinguished name '" + 
							 	 contractCert.getSubjectX500Principal().getName() + "' is about to expire in " + 
							 	 validityOfContractCert + " days");
				return ContractCertificateStatus.UPDATE_NEEDED;
			}
			return ContractCertificateStatus.OK;
		}
	}
	
	
	/**
	 * Returns a list of certificates from the given CertificateChainType with the leaf certificate 
	 * being the first element and potential subcertificates (intermediate CA certificatess) 
	 * in the array of certificates.
	 * 
	 * @param certChainType The CertificateChainType instance which holds a leaf certificate and
	 * 						possible intermediate certificates to verify the leaf certificate up to 
	 * 						some root certificate.
	 * @return An array of Certificates
	 */
	public static Certificate[] getCertificateChain(CertificateChainType certChainType) {
		List<byte[]> subCertificates = certChainType.getSubCertificates().getCertificate();
		Certificate[] certChain = new Certificate[subCertificates.size() + 1];
		
		certChain[0] = getCertificate(certChainType.getCertificate());
		
		for (int i = 0; i < subCertificates.size(); i++) {
			certChain[i+1] = getCertificate(subCertificates.get(i));
		}
		
		return certChain;
	}
	
	
	
	/**
	 * Generates an elliptic curve Diffie-Hellman keypair.
	 * 
	 * To use ECC (elliptic curve cryptography), SECC as well as EVCC must agree on all the elements 
	 * defining the elliptic curve, that is, the "domain parameters" of the scheme. Such domain 
	 * parameters are predefined by standardisation bodies and are commonly known as "standard curves" 
	 * or "named curves"; a named curve can be referenced either by name or by the unique object 
	 * identifier defined in the standard documents. For the ISO/IEC 15118-2 document, the named curve 
	 * "secp256r1" (SECG notation, see http://www.secg.org/sec2-v2.pdf) is used.
	 * See [V2G2-818] in ISO/IEC 15118-2 for further information.
	 * 
	 * @return The Diffie-Hellman keypair for the elliptic curve 'secp256r1'
	 */
	public static KeyPair getECDHKeyPair() {
		KeyPair keyPair = null;
		
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
			ECGenParameterSpec ecParameterSpec = new ECGenParameterSpec("secp256r1");
			keyPairGenerator.initialize(ecParameterSpec, new SecureRandom());
			keyPair = keyPairGenerator.generateKeyPair();
		} catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
			getLogger().error(e.getClass().getSimpleName() + " occurred while trying to generate ECDH key pair", e);
		} 
		
		return keyPair;
	}
	
	
	/**
	 * The shared secret which is to be generated with this function is used as input to a key derivation
	 * function. A key derivation function (KDF) is a deterministic algorithm to derive a key of a given
	 * size from some secret value. If two parties use the same shared secret value and the same KDF, 
	 * they should always derive exactly the same key.
	 * 
	 * @param privateKey The elliptic curve private key of a given certificate
	 * @param publicKey The elliptic curve Diffie-Hellman public key
	 * @return The computed shared secret of the elliptic curve Diffie-Hellman key exchange protocol
	 */
	public static byte[] generateSharedSecret(ECPrivateKey privateKey, ECPublicKey publicKey) {
	    try {
	        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
	        keyAgreement.init(privateKey, new SecureRandom());
	        keyAgreement.doPhase(publicKey, true);

	        return keyAgreement.generateSecret();
	    } catch (InvalidKeyException | NoSuchAlgorithmException e) {
	        getLogger().error(e.getClass().getSimpleName() + " occurred while trying to generate the shared secret (ECDH)", e);
	        return null;
	    } 
	}
	
	
	/**
	 * The key derivation function (KDF). See [V2G2-818] in ISO/IEC 15118-2 for further information.
	 * 
	 * @param sharedSecret The shared secret derived from the ECDH algorithm
	 */
	public static SecretKey generateSessionKey(byte[] sharedSecret) {
	    MessageDigest md = null;
	    /*
	     * TODO it is unclear to me what should be the content of suppPubInfo or suppPrivInfo 
	     * according to page 49 of http://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-56Ar2.pdf
	     * Requirement [V2G2-818] is not clear about that.
	     */
	    byte[] suppPubInfo = null;
	    byte[] suppPrivInfo = null;
	    
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e1) {
			getLogger().error("Message digest algorithm SHA-256 not supported");
			return null;
		}
        
        ByteArrayOutputStream baosOtherInfo = new ByteArrayOutputStream();
        try {
            baosOtherInfo.write(ByteUtils.toByteArrayFromHexString("01")); // algorithm ID
            baosOtherInfo.write(ByteUtils.toByteArrayFromHexString("55")); // partyUInfo
            baosOtherInfo.write(ByteUtils.toByteArrayFromHexString("56")); // partyVInfo
            if (suppPubInfo != null) baosOtherInfo.write(suppPubInfo); 
            if (suppPrivInfo != null) baosOtherInfo.write(suppPrivInfo);
        } catch (IOException e) {
            getLogger().error("IOException occurred while trying to write OtherInfo for session key generation", e);
        }
        
        byte[] otherInfo = baosOtherInfo.toByteArray();
        
        // A symmetric encryption key of exactly 128 bits shall be derived.
		byte[] sessionKeyAsByteArray = concatKDF(md, sharedSecret, 128, otherInfo);
		
		SecretKey sessionKey = null;
		try {
			sessionKey = new SecretKeySpec(sessionKeyAsByteArray, "AES");
		} catch (IllegalArgumentException e) {
			getLogger().error("IllegalArgumentException occurred while trying to generate session key", e);
		}
		
		return sessionKey;
    }
	
	
	/**
	 * Implementation of Concatenation Key Derivation Function <br/>
	 * http://csrc.nist.gov/publications/nistpubs/800-56A/SP800-56A_Revision1_Mar08-2007.pdf
	 *
	 * Author: NimbusDS  Lai Xin Chu and Vladimir Dzhuvinov
	 * 
	 * See https://code.google.com/p/openinfocard/source/browse/trunk/testsrc/org/xmldap/crypto/ConcatKeyDerivationFunction.java?r=770
	 */
	private static byte[] concatKDF(MessageDigest md, byte[] z, int keyDataLen, byte[] otherInfo) {
		final long MAX_HASH_INPUTLEN = Long.MAX_VALUE;
		final long UNSIGNED_INT_MAX_VALUE = 4294967295L;
		keyDataLen = keyDataLen/8;
        byte[] key = new byte[keyDataLen];
        
        int hashLen = md.getDigestLength();
        int reps = keyDataLen / hashLen;
        
        if (reps > UNSIGNED_INT_MAX_VALUE) {
        	getLogger().error("Key derivation failed");
        	return null;
        }
        
        int counter = 1;
        byte[] counterInBytes = intToFourBytes(counter);
        
        if ((counterInBytes.length + z.length + otherInfo.length) * 8 > MAX_HASH_INPUTLEN) {
        	getLogger().error("Key derivation failed");
        	return null;
        }
        
        for (int i = 0; i <= reps; i++) {
            md.reset();
            md.update(intToFourBytes(i+1));
            md.update(z);
            md.update(otherInfo);
            
            byte[] hash = md.digest();
            if (i < reps) {
                System.arraycopy(hash, 0, key, hashLen * i, hashLen);
            } else {
                if (keyDataLen % hashLen == 0) {
                    System.arraycopy(hash, 0, key, hashLen * i, hashLen);
                } else {
                    System.arraycopy(hash, 0, key, hashLen * i, keyDataLen % hashLen);
                }
            }
        }
        
        return key;
    }

	
    private static byte[] intToFourBytes(int i) {
        byte[] res = new byte[4];
        res[0] = (byte) (i >>> 24);
        res[1] = (byte) ((i >>> 16) & 0xFF);
        res[2] = (byte) ((i >>> 8) & 0xFF);
        res[3] = (byte) (i & 0xFF);
        return res;
    }
	

    private static ContractSignatureEncryptedPrivateKeyType getContractSignatureEncryptedPrivateKey(
    		SecretKey sessionKey, ECPrivateKey contractCertPrivateKey) {
    	ContractSignatureEncryptedPrivateKeyType encryptedPrivateKey = new ContractSignatureEncryptedPrivateKeyType();
    	encryptedPrivateKey.setValue(encryptPrivateKey(sessionKey, contractCertPrivateKey));
		
		return encryptedPrivateKey;
    }
    
    
    /**
     * Encrypts the private key of the contract certificate which is to be sent to the EVCC. First, the
     * shared secret based on the ECDH paramters is calculated, then the symmetric session key with which
     * the private key of the contract certificate is to be encrypted.
     * 
     * @param certificateECPublicKey The public key of either the OEM provisioning certificate (in case of 
     * 								 CertificateInstallation) or the to be updated contract certificate
     * 								 (in case of CertificateUpdate)
     * @param ecdhKeyPair The ECDH keypair
     * @param contractCertPrivateKey The private key of the contract certificate
     * @return The encrypted private key of the to be installed contract certificate
     */
	public static ContractSignatureEncryptedPrivateKeyType encryptContractCertPrivateKey(
			ECPublicKey certificateECPublicKey, 
			KeyPair ecdhKeyPair,
			ECPrivateKey contractCertPrivateKey) {
		// Generate the shared secret by using the public key of either OEMProvCert or ContractCert
		byte[] sharedSecret = generateSharedSecret((ECPrivateKey) ecdhKeyPair.getPrivate(), certificateECPublicKey);
		
		if (sharedSecret == null) {
			getLogger().error("Shared secret could not be generated");
			return null;
		}
		
		// The session key is generated using the computed shared secret
		SecretKey sessionKey = generateSessionKey(sharedSecret);
		
		// Finally, the private key of the contract certificate is encrypted using the session key
		ContractSignatureEncryptedPrivateKeyType encryptedContractCertPrivateKey = 
				getContractSignatureEncryptedPrivateKey(sessionKey, contractCertPrivateKey);
		
		return encryptedContractCertPrivateKey;
	}
	
    
	/**
	 * Applies the algorithm AES-CBC-128 according to NIST Special Publication 800-38A.
	 * The initialization vector IV shall be randomly generated before encryption and shall have a 
	 * length of 128 bit and never be reused.
	 * The IV shall be transmitted in the 16 most significant bytes of the 
	 * ContractSignatureEncryptedPrivateKey field.
	 * 
	 * @param sessionKey The symmetric session key with which the private key will be encrypted
	 * @param contractCertPrivateKey The private key which is to be encrypted
	 * @return The encrypted private key of the contract certificate given as a byte array
	 */
	private static byte[] encryptPrivateKey(SecretKey sessionKey, ECPrivateKey contractCertPrivateKey) {
		try {
			/*
			 * Padding of the plain text (private key) is not required as its length (256 bit) is a 
			 * multiple of the block size (128 bit) of the used encryption algorithm (AES)
			 */
			Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
			IvParameterSpec ivParamSpec = new IvParameterSpec(generateRandomNumber(16));
			cipher.init(Cipher.ENCRYPT_MODE, sessionKey, ivParamSpec);
			
			/*
			 * Not the complete ECPrivateKey container, but the private value s represents the 256 bit 
			 * private key which must be encoded. 
			 * The private key is stored as an ASN.1 integer which may need to have zero padding 
			 * in the most significant bits removed (if 33 bytes)
			 */
			byte[] encryptedKey;
			if (contractCertPrivateKey.getS().toByteArray().length == 33) {
				byte[] temp = new byte[32];
				System.arraycopy(contractCertPrivateKey.getS().toByteArray(), 1, temp, 0, contractCertPrivateKey.getS().toByteArray().length-1);
				encryptedKey = cipher.doFinal(temp);
			} else {
				encryptedKey = cipher.doFinal(contractCertPrivateKey.getS().toByteArray());
			}
			
			/*
			 * The IV must be transmitted in the 16 most significant bytes of the
			 * ContractSignatureEncryptedPrivateKey
			 */
			byte[] encryptedKeyWithIV = new byte[ivParamSpec.getIV().length + encryptedKey.length];
			System.arraycopy(ivParamSpec.getIV(), 0, encryptedKeyWithIV, 0, ivParamSpec.getIV().length);
			System.arraycopy(encryptedKey, 0, encryptedKeyWithIV, ivParamSpec.getIV().length, encryptedKey.length);
			
			return encryptedKeyWithIV;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | 
				 InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			getLogger().error(e.getClass().getSimpleName() + " occurred while trying to encrypt private key." +
							  "\nSession key (" + sessionKey.getEncoded().length + " bytes): " +
							  ByteUtils.toHexString(sessionKey.getEncoded()) +
							  "\nContract certificate private key (" + contractCertPrivateKey.getS().toByteArray().length + " bytes): " +
							  ByteUtils.toHexString(contractCertPrivateKey.getS().toByteArray()), e);
		} 
		
		return null;
	}
	
	
	/**
	 * Decrypts the encrypted private key of the contract certificate which is to be installed.
	 * 
	 * @param dhPublicKey The ECDH public key received the the respective response message
	 * 		  (either CertificateInstallationRes or CertificateUpdateRes)
	 * @param contractSignatureEncryptedPrivateKey The encrypted private key of the contract certificate
	 * @param certificateECPrivateKey The private key of either OEMProvisioningCertificate (in case of
	 * 		  receipt of CertificateInstallationRes) or the existing ContractCertificate which is to be
	 * 		  updated (in case of receipt of CertificateUpdateRes).
	 * @return The decrypted private key of the contract certificate which is to be installed
	 */
	public static ECPrivateKey decryptContractCertPrivateKey(
			byte[] dhPublicKey,
			byte[] contractSignatureEncryptedPrivateKey,
			ECPrivateKey certificateECPrivateKey) {
		// Generate ECDH key pair
		KeyPair ecdhKeyPair = getECDHKeyPair();
		if (ecdhKeyPair == null) {
			getLogger().error("ECDH keypair could not be generated");
			return null;
		}
		
		// Generate shared secret
		ECPublicKey publicKey = (ECPublicKey) getPublicKey(dhPublicKey);
		byte[] sharedSecret = generateSharedSecret(certificateECPrivateKey, publicKey);
		if (sharedSecret == null) {
			getLogger().error("Shared secret could not be generated");
			return null;
		}
		
		// Generate the session key ...
		SecretKey sessionKey = generateSessionKey(sharedSecret);
		if (sessionKey == null) {
			getLogger().error("Session key secret could not be generated");
			return null;
		}
		
		// ... to decrypt the contract certificate private key
		ECPrivateKey contractCertPrivateKey = decryptPrivateKey(sessionKey, contractSignatureEncryptedPrivateKey);
		if (contractCertPrivateKey == null) {
			getLogger().error("Contract certificate private key secret could not be decrypted");
			return null;
		}
		
		return contractCertPrivateKey;
	}
	
	
	/**
	 * The private key corresponding to the contract certificate is to be decrypted by 
	 * the receiver (EVCC) using the session key derived in the ECDH protocol.
	 * Applies the algorithm AES-CBC-128 according to NIST Special Publication 800-38A.
	 * The initialization vector IV shall be read from the 16 most significant bytes of the 
	 * ContractSignatureEncryptedPrivateKey field.
	 * 
	 * @param sessionKey The symmetric session key with which the encrypted private key is to be decrypted
	 * @param encryptedKeyWithIV The encrypted private key of the contract certificate given as a byte array
	 * 							 whose first 16 byte hold the initialization vector
	 * @return The decrypted private key of the contract certificate
	 */
	private static ECPrivateKey decryptPrivateKey(SecretKey sessionKey, byte[] encryptedKeyWithIV) {
		byte[] initVector = new byte[16];
		byte[] encryptedKey = null;
		
		try {
			// Get the first 16 bytes of the encrypted private key which hold the IV
			
			encryptedKey = new byte[encryptedKeyWithIV.length - 16];
			System.arraycopy(encryptedKeyWithIV, 0, initVector, 0, 16);
			System.arraycopy(encryptedKeyWithIV, 16, encryptedKey, 0, encryptedKeyWithIV.length - 16);
			
			IvParameterSpec ivParamSpec = new IvParameterSpec(initVector);
			Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
			
			/*
			 * You must have the Java Cryptography Extension (JCE) Unlimited Strength 
			 * Jurisdiction Policy Files 8 installed, otherwise this cipher.init call will yield a
			 * "java.security.InvalidKeyException: Illegal key size"
			 */
			cipher.init(Cipher.DECRYPT_MODE, sessionKey, ivParamSpec);
			byte[] decrypted = cipher.doFinal(encryptedKey);

			return getPrivateKey(decrypted);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
				InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException |
				NegativeArraySizeException e) {
			getLogger().error(e.getClass().getSimpleName() + " occurred while trying to decrypt private key" +
					  "\nSession key (" + (sessionKey != null ? sessionKey.getEncoded().length : 0) + " bytes): " +
					  ByteUtils.toHexString(sessionKey.getEncoded()) +
					  "\nEncrypted key (" + (encryptedKey != null ? encryptedKey.length : 0) + " bytes): " +
					  ByteUtils.toHexString(encryptedKey) +
					  "\nEncrypted key with IV (" + (encryptedKeyWithIV != null ? encryptedKeyWithIV.length : 0) + " bytes): " +
					  ByteUtils.toHexString(encryptedKey), e);
		} 
		
		return null;
	}
	
	
	/**
	 * Returns the EMAID (e-mobility account identifier) from the contract certificate.
	 * 
	 * @param contractCertificateChain The certificate chain holding the contract certificate
	 * @return The EMAID
	 */
	public static EMAIDType getEMAID(CertificateChainType contractCertificateChain) {
		X509Certificate contractCertificate = getCertificate(contractCertificateChain.getCertificate());
		return getEMAIDFromDistinguishedName(contractCertificate.getSubjectX500Principal().getName());
	}
	
	
	/**
	 * Returns the EMAID (e-mobility account identifier) from the contract certificate.
	 * 
	 * @param keyStorePassword The password which protects the keystore holding the contract certificate
	 * @return The EMAID
	 */
	public static EMAIDType getEMAID(String keyStorePassword) {
		KeyStore keyStore = getKeyStore(GlobalValues.EVCC_KEYSTORE_FILEPATH.toString(), keyStorePassword);
		
		try {
			X509Certificate contractCertificate = 
					(X509Certificate) keyStore.getCertificate(GlobalValues.ALIAS_CONTRACT_CERTIFICATE.toString());
			
			if (contractCertificate == null) {
				getLogger().error("No contract certificate with alias '" +
									 GlobalValues.ALIAS_CONTRACT_CERTIFICATE.toString() + "' found");
				return null;
			}
			
			return getEMAIDFromDistinguishedName(contractCertificate.getSubjectX500Principal().getName());
		} catch (KeyStoreException e) {
			getLogger().error("KeyStoreException occurred while trying to get EMAID from keystore", e);
			return null;
		}
	}
	
	
	/**
	 * Reads the EMAID (e-mobility account identifier) from the distinguished name (DN) of a certificate. 
	 * 
	 * @param distinguishedName The distinguished name whose 'CN' component holds the EMAID
	 * @return The EMAID
	 */
	private static EMAIDType getEMAIDFromDistinguishedName(String distinguishedName) {	
		EMAIDType emaid = new EMAIDType();
		
		LdapName ln = null;
		try {
			ln = new LdapName(distinguishedName);
		} catch (InvalidNameException e) {
			getLogger().error("InvalidNameException occurred while trying to get EMAID from distinguished name", e);
		}

		for(Rdn rdn : ln.getRdns()) {
		    if (rdn.getType().equalsIgnoreCase("CN")) {
		    	// Optional hyphens used for better human readability must be omitted here
		    	emaid.setId(rdn.getValue().toString().replace("-", ""));
		    	emaid.setValue(rdn.getValue().toString().replace("-", ""));
		        break;
		    }
		}
		
		return emaid;
	}
	
	
	/**
	 * Searches a given keystore either for a contract certificate chain or OEM provisioning certificate
	 * chain, determined by the alias (the alias is associated with the certificate chain and the private
	 * key). 
	 * However, it may be the case that more than once contract certificate is installed in the EV, 
	 * in which case an OEM specific implementation would need to interact at this point with a HMI in
	 * order to enable the user to select the certificate which is to be used for contract based charging.
	 * 
	 * @param evccKeyStore The keystore to check for the respective certificate chain
	 * @param alias The alias associated with a key entry and certificate chain
	 * @return The respective certificate chain if present, null otherwise
	 */
	public static CertificateChainType getCertificateChain(KeyStore evccKeyStore, String alias) {
		CertificateChainType certChain = new CertificateChainType();
		SubCertificatesType subCertificates = new SubCertificatesType();
		
		try {
			Certificate[] certChainArray = evccKeyStore.getCertificateChain(alias);
			
			if (certChainArray == null) {
				getLogger().info("No certificate chain found for alias '" + alias + "'");
				return null;
			}
			
			certChain.setCertificate(certChainArray[0].getEncoded());

			for (int i = 1; i < certChainArray.length; i++) {
				subCertificates.getCertificate().add(certChainArray[i].getEncoded());
			}
			
			certChain.setSubCertificates(subCertificates);
			
			return certChain;
		} catch (KeyStoreException | CertificateEncodingException e) {
			getLogger().error(e.getClass().getSimpleName() + " occurred while trying to get certificate chain", e);
			return null;
		}
	}
	
	
	/**
	 * Returns a random number of a given length of bytes.
	 * 
	 * @param lengthOfBytes The number of bytes which hold the generated random number
	 * @return A random number given as a byte array
	 */
	public static byte[] generateRandomNumber(int lengthOfBytes) {
		// TODO how to assure that the entropy of the genChallenge is at least 120 bits according to [V2G2-826]?
		
		SecureRandom random = new SecureRandom();
		byte[] randomNumber = new byte[lengthOfBytes];
		random.nextBytes(randomNumber);
		
		return randomNumber;
	}
	
	
	/**
	 * Generates a digest for a complete message or field (which ever is handed over as first parameter).
	 * During digest (SHA-256) generation, the parameter is converted to a JAXBElement and then EXI encoded 
	 * using the respective EXI schema-informed grammar. If the digest for the signature is to be generated,  
	 * the second parameter is to be set to true, for all other messages or fields the second parameter 
	 * needs to be set to false.
	 * 
	 * @param messageOrField The message or field for which a digest is to be generated
	 * @param signature True if a digest for a signature is to be generated, false otherwise
	 * @return The SHA-256 digest for message or field
	 */
	public static byte[] generateDigest(Object messageOrField, boolean signature) {
		JAXBElement jaxbElement = MiscUtils.getJaxbElement(messageOrField);
		byte[] encoded; 
		
		// TODO what was again the difference?
		if (signature) encoded = getExiCodec().encodeEXI(jaxbElement, false);
		else encoded = getExiCodec().encodeEXI(jaxbElement, false);
			
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(encoded);
			
			return md.digest();
		} catch (NoSuchAlgorithmException e) {
			getLogger().error("NoSuchAlgorithmException occurred while trying to create digest", e);
			return null;
		}
	}
	
	
	/**
	 * Signs the SignatureInfo element of the V2GMessage header.
	 * 
	 * @param signatureInfo The SignatureInfo given as a byte array
	 * @param ecPrivateKey The private key which is used to sign the SignatureInfo element
	 * @return The signed SignatureInfo element given as a byte array
	 */
	public static byte[] signSignatureInfo(byte[] signatureInfo, ECPrivateKey ecPrivateKey) {
		try {
			Signature ecdsa = Signature.getInstance("SHA256withECDSA");
			ecdsa.initSign(ecPrivateKey);
			ecdsa.update(signatureInfo);
			byte[] signature = ecdsa.sign();
			
			return signature;
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
			getLogger().error(e.getClass().getSimpleName() + " occurred while trying to create signature", e);
			return null;
		}
	}
	
	
	/**
	 * Verifies the signature given in the received header of an EVCC or SECC message
	 * 
	 * @param signature The received header's signature
	 * @param verifyXMLSigRefElements The HashMap of signature IDs and digest values of the message body 
	 * 		  or fields respectively of the received message (to cross-check against the XML reference
	 * 		  elements contained in the received message header)
	 * @param ecPublicKey The public key corresponding to the private key which was used for the signature
	 * @return True, if digest validation of all XML reference elements and signature validation was 
	 * 		   successful, false otherwise
	 */
	public static boolean verifySignature(
				SignatureType signature, 
				HashMap<String, byte[]> verifyXMLSigRefElements, 
				ECPublicKey ecPublicKey) {
		byte[] providedDigest; 
		boolean match;
		
		/*
		 * 1. step: 
		 * Iterate over all element IDs of the message which should have been signed and find the 
		 * respective Reference element in the given message header
		 */
		for (String id : verifyXMLSigRefElements.keySet()) {
			getLogger().debug("Verifying digest for element '" + id + "'");
			match = false;
			providedDigest = verifyXMLSigRefElements.get(id);
			
			// A bit inefficient, but there are max. 4 elements to iterate over (what would be more efficient?)
			for (ReferenceType reference : signature.getSignedInfo().getReference()) {
				if (reference.getId().equals(id) && Arrays.equals(reference.getDigestValue(), providedDigest))
					match = true;
			}
			
			if (!match) {
				getLogger().error("No matching signature found for ID '" + id + "' and digest value " + 
								  ByteUtils.toHexString(providedDigest));
				return false;
			}
		}
		
		
		/*
		 * 2. step:
		 * Check the signature value from the header with the computed signature value
		 */
		byte[] computedSignedInfoDigest = generateDigest(signature.getSignedInfo(), true);
		Signature ecdsa;
		boolean verified; 
		
		try {
			getLogger().debug("Verifying signature of signed info element");
			ecdsa = Signature.getInstance("SHA256withECDSA");
			ecdsa.initVerify(ecPublicKey);
			ecdsa.update(computedSignedInfoDigest);
			verified = ecdsa.verify(signature.getSignatureValue().getValue());
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
			getLogger().error(e.getClass().getSimpleName() + " occurred while trying to verify signature value", e);
			return false;
		}
		
		return verified;
	}
	
	
	/**
	 * Sets the SSLContext of the TLSServer and TLSClient with the given keystore and truststore locations as
	 * well as the password protecting the keystores/truststores.
	 * 
	 * @param keyStorePath The relative path and filename for the keystore
	 * @param trustStorePath The relative path and filename for the truststore
	 * @param keyStorePassword The password protecting the keystore
	 */
	public static void setSSLContext(
			String keyStorePath, 
			String trustStorePath,
			String keyStorePassword) {
	    KeyStore keyStore = SecurityUtils.getKeyStore(keyStorePath, keyStorePassword);
	    KeyStore trustStore = SecurityUtils.getKeyStore(trustStorePath, keyStorePassword);

		try {
			// Initialize a key manager factory with the keystore
		    KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyFactory.init(keyStore, keyStorePassword.toCharArray());
		    KeyManager[] keyManagers = keyFactory.getKeyManagers();

		    // Initialize a trust manager factory with the truststore
		    TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());    
		    trustFactory.init(trustStore);
		    TrustManager[] trustManagers = trustFactory.getTrustManagers();

		    // Initialize an SSL context to use these managers and set as default
		    SSLContext sslContext = SSLContext.getInstance("TLS");
		    sslContext.init(keyManagers, trustManagers, null);
		    SSLContext.setDefault(sslContext); 
		} catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | 
				KeyManagementException e) {
			getLogger().error(e.getClass().getSimpleName() + " occurred while trying to initialize SSL context");
		}    
	}
	
	public static void setExiCodec(ExiCodec exiCodecChoice) {
		exiCodec = exiCodecChoice;
	}
	
	private static ExiCodec getExiCodec() {
		return exiCodec;
	}
}
