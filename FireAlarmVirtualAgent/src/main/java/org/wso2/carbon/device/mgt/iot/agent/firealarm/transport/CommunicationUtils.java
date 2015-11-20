package org.wso2.carbon.device.mgt.iot.agent.firealarm.transport;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.exception.AgentCoreOperationException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

public class CommunicationUtils {
    private static final Log log = LogFactory.getLog(TransportUtils.class);

    private static final String SIGNATURE_ALG = "SHA1withRSA";
    private static final String CIPHER_PADDING = "RSA/ECB/PKCS1Padding";
    
public static String encryptMessage(String message, Key encryptionKey) throws AgentCoreOperationException {
        Cipher encrypter;
        byte[] cipherData;

        try {
            encrypter = Cipher.getInstance(CIPHER_PADDING);
            encrypter.init(Cipher.ENCRYPT_MODE, encryptionKey);
            cipherData = encrypter.doFinal(message.getBytes(StandardCharsets.UTF_8));

        } catch (NoSuchAlgorithmException e) {
            String errorMsg = "Algorithm not found exception occurred for Cipher instance of [" + CIPHER_PADDING + "]";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        } catch (NoSuchPaddingException e) {
            String errorMsg = "No Padding error occurred for Cipher instance of [" + CIPHER_PADDING + "]";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        } catch (InvalidKeyException e) {
            String errorMsg = "InvalidKey exception occurred for encryptionKey \n[\n" + encryptionKey + "\n]\n";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        } catch (BadPaddingException e) {
            String errorMsg = "Bad Padding error occurred for Cipher instance of [" + CIPHER_PADDING + "]";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        } catch (IllegalBlockSizeException e) {
            String errorMsg = "Illegal blockSize error occurred for Cipher instance of [" + CIPHER_PADDING + "]";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        }

        return Base64.encodeBase64String(cipherData);
    }


    public static String signMessage(String encryptedData, PrivateKey signatureKey) throws AgentCoreOperationException {

        Signature signature;
        String signedEncodedString;

        try {
            signature = Signature.getInstance(SIGNATURE_ALG);
            signature.initSign(signatureKey);
            signature.update(Base64.decodeBase64(encryptedData));

            byte[] signatureBytes = signature.sign();
            signedEncodedString = Base64.encodeBase64String(signatureBytes);

        } catch (NoSuchAlgorithmException e) {
            String errorMsg = "Algorithm not found exception occurred for Signature instance of [" + SIGNATURE_ALG + "]";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        } catch (SignatureException e) {
            String errorMsg = "Signature exception occurred for Signature instance of [" + SIGNATURE_ALG + "]";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        } catch (InvalidKeyException e) {
            String errorMsg = "InvalidKey exception occurred for signatureKey \n[\n" + signatureKey + "\n]\n";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        }

        return signedEncodedString;
    }


    public static boolean verifySignature(String data, String signedData, PublicKey verificationKey)
            throws AgentCoreOperationException {

        Signature signature;
        boolean verified;

        try {
            signature = Signature.getInstance(SIGNATURE_ALG);
            signature.initVerify(verificationKey);
            signature.update(Base64.decodeBase64(data));

            verified = signature.verify(Base64.decodeBase64(signedData));

        } catch (NoSuchAlgorithmException e) {
            String errorMsg = "Algorithm not found exception occurred for Signature instance of [" + SIGNATURE_ALG + "]";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        } catch (SignatureException e) {
            String errorMsg = "Signature exception occurred for Signature instance of [" + SIGNATURE_ALG + "]";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        } catch (InvalidKeyException e) {
            String errorMsg = "InvalidKey exception occurred for signatureKey \n[\n" + verificationKey + "\n]\n";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        }

        return verified;
    }


    public static String decryptMessage(String encryptedMessage, Key decryptKey) throws AgentCoreOperationException {

        Cipher decrypter;
        String decryptedMessage;

        try {

            decrypter = Cipher.getInstance(CIPHER_PADDING);
            decrypter.init(Cipher.DECRYPT_MODE, decryptKey);
            decryptedMessage = new String(decrypter.doFinal(Base64.decodeBase64(encryptedMessage)), StandardCharsets.UTF_8);

        } catch (NoSuchAlgorithmException e) {
            String errorMsg = "Algorithm not found exception occurred for Cipher instance of [" + CIPHER_PADDING + "]";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        } catch (NoSuchPaddingException e) {
            String errorMsg = "No Padding error occurred for Cipher instance of [" + CIPHER_PADDING + "]";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        } catch (InvalidKeyException e) {
            String errorMsg = "InvalidKey exception occurred for encryptionKey \n[\n" + decryptKey + "\n]\n";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        } catch (BadPaddingException e) {
            String errorMsg = "Bad Padding error occurred for Cipher instance of [" + CIPHER_PADDING + "]";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        } catch (IllegalBlockSizeException e) {
            String errorMsg = "Illegal blockSize error occurred for Cipher instance of [" + CIPHER_PADDING + "]";
            log.error(errorMsg);
            throw new AgentCoreOperationException(errorMsg, e);
        }

        return decryptedMessage;
    }
}
