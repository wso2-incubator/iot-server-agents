package org.wso2.carbon.device.mgt.iot.agent.firealarm.enrollment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.jscep.client.Client;
import org.jscep.client.ClientException;
import org.jscep.client.EnrollmentResponse;
import org.jscep.client.verification.CertificateVerifier;
import org.jscep.client.verification.OptimisticCertificateVerifier;
import org.jscep.transaction.TransactionException;
import org.jscep.transport.response.Capabilities;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.core.AgentManager;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.exception.AgentCoreOperationException;
import sun.security.x509.X509CertImpl;

import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Date;

public class EnrollmentManager {
    private static final Log log = LogFactory.getLog(EnrollmentManager.class);
    private static EnrollmentManager enrollmentManager;

    private static final String KEY_PAIR_ALGORITHM = "RSA";
    private static final String PROVIDER = "BC";
    private static final String SIGNATURE_ALG = "SHA1withRSA";
    private static final String CIPHER_PADDING = "RSA/ECB/PKCS1Padding";
    private static final int KEY_SIZE = 2048;
    // Seed to our PRNG. Make sure this is initialised randomly, NOT LIKE THIS
    private static final byte[] SEED = ")(*&^%$#@!".getBytes();
    private static final int CERT_VALIDITY = 730;

    // URL of our SCEP server
    private String SCEPUrl;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private PublicKey serverPublicKey;
    private X509Certificate SCEPCertificate;


    private EnrollmentManager() {
        this.SCEPUrl = AgentManager.getInstance().getEnrollmentEP();
    }

    public static EnrollmentManager getInstance() {
        if (enrollmentManager == null) {
            enrollmentManager = new EnrollmentManager();
        }
        return enrollmentManager;
    }


    public void beginEnrollmentFlow() throws AgentCoreOperationException {
        Security.addProvider(new BouncyCastleProvider());

        KeyPair keyPair = generateKeyPair();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();

        log.info("----------------------------------\n");
        log.info("DevicePrivateKey: " + privateKey + "\n");
        log.info("----------------------------------\n");
        log.info("DevicePublicKey: " + publicKey + "\n");

        PKCS10CertificationRequest certSignRequest = generateCertSignRequest();

        /**
         *  -----------------------------------------------------------------------------------------------
         *  Generate an ephemeral self-signed certificate. This is needed to present to the CA in the SCEP
         *  request. In the future, add proper EKU and attributes in the request
         *  The CA does NOT have to honour any of this.
         *  -----------------------------------------------------------------------------------------------
         */
        X500Name issuer = new X500Name("CN=Temporary Issuer");
        BigInteger serial = new BigInteger(32, new SecureRandom());
        Date fromDate = new Date();
        Date toDate = new Date(System.currentTimeMillis() + (CERT_VALIDITY * 86400000L));

        // Build the self-signed cert using BC, sign it with our private key (self-signed)
        // Equivalent of the old X509Util ephemeral certificate
        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(issuer, serial, fromDate, toDate,
                                                                            certSignRequest.getSubject(),
                                                                            certSignRequest.getSubjectPublicKeyInfo());
        ContentSigner sigGen = null;
        X509Certificate tmpCert = null;

        try {
            sigGen = new JcaContentSignerBuilder(SIGNATURE_ALG).setProvider(PROVIDER).build(keyPair.getPrivate());
            tmpCert = new JcaX509CertificateConverter().setProvider(PROVIDER).getCertificate(certBuilder.build(sigGen));
        } catch (OperatorCreationException e) {
            String errorMsg = "Error occurred whilst creating a ContentSigner for the Temp-Self-Signed Certificate.";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        } catch (CertificateException e) {
            String errorMsg = "Error occurred whilst trying to create Temp-Self-Signed Certificate.";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        }

        log.info("----------------------------------\n");
        log.info("TempCertPublicKey: " + tmpCert.getPublicKey() + "\n");

        /**
         *  -----------------------------------------------------------------------------------------------
         */

        this.SCEPCertificate = getSignedCertificateFromServer(tmpCert, certSignRequest);
        this.serverPublicKey = getPublicKeyOfServer();

        log.info("----------------------------------\n");
        log.info("SignedCertPublic: " + SCEPCertificate.getPublicKey() + "\n");
        log.info("----------------------------------\n");
        log.info("ServerPublicKey: " + serverPublicKey + "\n");
        log.info("----------------------------------" + "\n");

    }


    private KeyPair generateKeyPair() throws AgentCoreOperationException {

        // Generate our key pair
        KeyPairGenerator keyPairGenerator = null;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance(KEY_PAIR_ALGORITHM, PROVIDER);
            keyPairGenerator.initialize(KEY_SIZE, new SecureRandom(SEED));
        } catch (NoSuchAlgorithmException e) {
            String errorMsg = "Algorithm [" + KEY_PAIR_ALGORITHM + "] provided for KeyPairGenerator is invalid.";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        } catch (NoSuchProviderException e) {
            String errorMsg = "Provider [" + PROVIDER + "] provided for KeyPairGenerator does not exist.";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        }

        return keyPairGenerator.genKeyPair();
    }


    private PKCS10CertificationRequest generateCertSignRequest() throws AgentCoreOperationException {
        // Build the CN for the cert we are requesting. Here, the CN contains my email address
        X500NameBuilder nameBld = new X500NameBuilder(BCStyle.INSTANCE);
        nameBld.addRDN(BCStyle.CN, "shabir@wso2.com");
        nameBld.addRDN(BCStyle.OU, "Engineering");
        nameBld.addRDN(BCStyle.O, "WSO2");
        nameBld.addRDN(BCStyle.UNIQUE_IDENTIFIER, AgentManager.getInstance().getAgentConfigs().getDeviceId());
        X500Name principal = nameBld.build();

        JcaContentSignerBuilder contentSignerBuilder = new JcaContentSignerBuilder(SIGNATURE_ALG).setProvider(
                PROVIDER);
        ContentSigner contentSigner = null;

        try {
            contentSigner = contentSignerBuilder.build(this.privateKey);
        } catch (OperatorCreationException e) {
            String errorMsg = "Could not create content signer with private key.";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        }

        // Generate the certificate signing request (csr = PKCS10)
        PKCS10CertificationRequestBuilder reqBuilder = new JcaPKCS10CertificationRequestBuilder(principal, this.publicKey);
        PKCS10CertificationRequest certSignRequest = reqBuilder.build(contentSigner);
        return certSignRequest;
    }


    private X509Certificate getSignedCertificateFromServer(X509Certificate tempCert,
                                                           PKCS10CertificationRequest certSignRequest)
            throws AgentCoreOperationException {

        X509Certificate signedSCEPCertificate = null;
        URL url = null;
        EnrollmentResponse enrolResponse = null;
        CertStore certStore = null;

        try {
            // The URL where we are going to request our cert from
            url = new URL(this.SCEPUrl);

            /*  // This is called when we get the certificate for our CSR signed by CA
                // Implement this handler to check the CA cert in prod. We can do cert pinning here
            CallbackHandler cb = new CallbackHandler() {
                @Override
                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated
                    methods, choose Tools | Templates.
                }
            };*/

            // I did not implement any verification of the CA cert. DO NOT DO THAT.
            // For testing this is OK, in Prod make sure to VERIFY the CA
            CertificateVerifier ocv = new OptimisticCertificateVerifier();

            // Instantiate our SCEP client
            Client scepClient = new Client(url, ocv);

            // Submit our cert for signing. iosTrustpoint allows the client to specify
            // the SCEP CA to issue the request against, if there are multiple CAs
            enrolResponse = scepClient.enrol(tempCert, this.privateKey, certSignRequest);

            // Verify we got what we want, and just print out the cert.
            certStore = enrolResponse.getCertStore();

            for (java.security.cert.Certificate x509Certificate : certStore.getCertificates(null)) {
                if (log.isDebugEnabled()) {
                    log.debug(x509Certificate.toString());
                }
                signedSCEPCertificate = (X509Certificate) x509Certificate;
            }

        } catch (MalformedURLException ex) {
            String errorMsg = "Could not create valid URL from given SCEP URI: " + SCEPUrl;
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, ex);
        } catch (TransactionException | ClientException e) {
            String errorMsg = "Enrollment process to SCEP Server at: " + SCEPUrl + " failed.";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        } catch (CertStoreException e) {
            String errorMsg = "Could not retrieve [Signed-Certificate] from the response message from SCEP-Server.";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        }

        return signedSCEPCertificate;
    }


    private PublicKey getPublicKeyOfServer() throws AgentCoreOperationException {
        URL url = null;
        CertStore certStore = null;
        PublicKey serverCertPublicKey = null;

        try {
            // The URL where we are going to request our cert from
            url = new URL(this.SCEPUrl);

            /*  // This is called when we get the certificate for our CSR signed by CA
                // Implement this handler to check the CA cert in prod. We can do cert pinning here
                CallbackHandler cb = new CallbackHandler() {
                    @Override
                    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated
                        methods, choose Tools | Templates.
                }
            };*/

            // I did not implement any verification of the CA cert. DO NOT DO THAT.
            // For testing this is OK, in Prod make sure to VERIFY the CA
            CertificateVerifier ocv = new OptimisticCertificateVerifier();

            // Instantiate our SCEP client
            Client scepClient = new Client(url, ocv);

            // Get the CA capabilities. For some reason the IOS router does not return
            // correct information here. Do not trust it. Should return SHA1withRSA for
            // strongest hash and sig. Returns MD5.

            if (log.isDebugEnabled()) {
                Capabilities cap = scepClient.getCaCapabilities();
                log.debug(String.format(
                        "\nStrongestCipher: %s,\nStrongestMessageDigest: %s,\nStrongestSignatureAlgorithm: %s," +
                                "\nIsRenewalSupported: %s,\nIsRolloverSupported: %s",
                        cap.getStrongestCipher(), cap.getStrongestMessageDigest(), cap.getStrongestSignatureAlgorithm(),
                        cap.isRenewalSupported(), cap.isRolloverSupported()));
            }

            certStore = scepClient.getCaCertificate();

            for (Certificate cert : certStore.getCertificates(null)) {
                if (cert instanceof X509Certificate) {
                    if (log.isDebugEnabled()) {
                        log.debug(((X509Certificate) cert).getIssuerDN().getName());
                    }

                    //TODO: Need to identify the correct certificate.
                    // I have chosen the CA cert based on its BasicConstraint criticality being set to "true"
                    if (((X509CertImpl) cert).getBasicConstraintsExtension().isCritical()) {
                        serverCertPublicKey = cert.getPublicKey();
                    }

                    serverCertPublicKey = cert.getPublicKey();
                }
            }

        } catch (MalformedURLException ex) {
            String errorMsg = "Could not create valid URL from given SCEP URI: " + SCEPUrl;
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, ex);
        } catch (ClientException e) {
            String errorMsg = "Could not retrieve [Server-Certificate] from the SCEP-Server.";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        } catch (CertStoreException e) {
            String errorMsg = "Could not retrieve [Server-Certificates] from the response message from SCEP-Server.";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        }

        return serverCertPublicKey;
    }



    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public X509Certificate getSCEPCertificate() {
        return SCEPCertificate;
    }

    public PublicKey getServerPublicKey() {
        return serverPublicKey;
    }
}
