package org.wso2.carbon.device.mgt.iot.agent.firealarm.enrollment.scep;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.jscep.client.Client;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;

public class CertificateManager {

    private static final Log log = LogFactory.getLog(CertificateManager.class);

    // URL of our SCEP server
    private static String scepUrl;
    private static final String sigAlg = "SHA1withRSA";
    private static final int keySize = 2048;
    // Seed to our PRNG. Make sure this is initialised randomly, NOT LIKE THIS

    private static final byte[] seed = ")(*&^%$#@!".getBytes();
    private static final int validity = 365;

    public static void getServerCACertificate(){

    }


    public static PKCS10CertificationRequest generateCertificateSignRequest(){

        URL url = null;

        Security.addProvider(new BouncyCastleProvider());

        // Generate our key pair
        KeyPairGenerator kpg = null;
        try {
            kpg = KeyPairGenerator.getInstance("RSA", "BC");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        kpg.initialize(keySize, new SecureRandom(seed));
        final KeyPair kp = kpg.genKeyPair();

        // Build the CN for the cert we are requesting. Here, the CN contains my email address
        X500NameBuilder nameBld = new X500NameBuilder(BCStyle.INSTANCE);
        nameBld.addRDN(BCStyle.CN, "Myemail@Mydomain.com");
        nameBld.addRDN(BCStyle.OU, "MyOU");
        nameBld.addRDN(BCStyle.O, "MyO");

        X500Name principal = nameBld.build();

//        X509Certificate entity = X509Util.createEphemeralCertificate(principal, keyPair);

        JcaContentSignerBuilder csb2 = new JcaContentSignerBuilder(sigAlg).setProvider("BC");
        ContentSigner cs = null;
        try {
            cs = csb2.build(kp.getPrivate());
        } catch (OperatorCreationException e) {
            e.printStackTrace();
        }

        // Generate the certificate signing request (csr = PKCS10)
        PKCS10CertificationRequestBuilder reqBuilder = new JcaPKCS10CertificationRequestBuilder(principal, kp.getPublic());

//        DERPrintableString password = new DERPrintableString("secret");
//        crb.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_challengePassword, password);

        PKCS10CertificationRequest csr = reqBuilder.build(cs);

        // The URL where we are going to request our cert from
//        try {
//            url = new URL(scepUrl);
//        } catch (MalformedURLException ex) {
//            log.error(ex);
//        }

        return csr;
    }


    public static void setScepUrl(String scepUrl) {
        CertificateManager.scepUrl = scepUrl;
    }
}
