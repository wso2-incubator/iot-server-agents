package org.wso2.carbon.device.mgt.iot.agent.firealarm.enrollment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSSignedData;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.core.AgentConstants;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.core.AgentManager;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.enrollment.scep.CertificateManager;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.enrollment.scep.SCEPOperation;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.transport.CommunicationHandlerException;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.transport.CommunicationUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.security.cert.X509Certificate;

public class EnrollmentManager {
    private static final Log log = LogFactory.getLog(EnrollmentManager.class);
    private static EnrollmentManager enrollmentManager;

    private String enrollementEndpoint;
    private X509Certificate serverCACertificate;
    private String serverCACapabilities;

    private EnrollmentManager() {
        this.enrollementEndpoint = AgentManager.getInstance().getEnrollmentEP();
    }

    public static EnrollmentManager getInstance() {
        if (enrollmentManager == null) {
            enrollmentManager = new EnrollmentManager();
        }
        return enrollmentManager;
    }


    public void beginEnrollmentFlow() {
        getCACertificateFromServer();
        getCACapabilitiesFromServer();
        CertificateManager.generateCertificateSignRequest();
//        getSignedSCEPCertificate();
    }

    public void getCACertificateFromServer() {
        X509Certificate CACertificateFromServer;

        int responseCode = -1;
        String responseFromServer;

        HttpURLConnection httpConnection = null;
        String CACertRequestEndpoint = String.format(enrollementEndpoint,
                                                     SCEPOperation.GET_CA_CERT.getValue());

        try {
            httpConnection = CommunicationUtils.getHttpConnection(CACertRequestEndpoint);
            httpConnection.setRequestMethod(AgentConstants.HTTP_GET);
//          httpConnection.setRequestProperty("Content-Type", AgentConstants.APPLICATION_JSON_TYPE);
            httpConnection.setDoOutput(false);

            responseCode = httpConnection.getResponseCode();
            responseFromServer = CommunicationUtils.readResponseFromHttpRequest(httpConnection);
            httpConnection.disconnect();

            CMSSignedData signedData = new CMSSignedData(responseFromServer.getBytes());
            CMSProcessable signedContent = signedData.getSignedContent();


            //TODO:: Need get server cert
            log.info("Response COde for GetCA:" + responseCode);
            log.info("Response from server:" + responseFromServer);
            log.info("Response COntent from server:" + signedContent.toString());

        } catch (CommunicationHandlerException comHandlerEx) {

        } catch (ProtocolException protocolEx) {

        } catch (IOException IOEx) {

        } catch (CMSException e) {
            e.printStackTrace();
        }
    }

    public void getCACapabilitiesFromServer() {
        int responseCode = -1;
        String responseFromServer;

        HttpURLConnection httpConnection = null;
        String CACertRequestEndpoint = String.format(enrollementEndpoint,
                                                     SCEPOperation.GET_CA_CAPS.getValue());

        try {
            httpConnection = CommunicationUtils.getHttpConnection(CACertRequestEndpoint);
            httpConnection.setRequestMethod(AgentConstants.HTTP_GET);
//          httpConnection.setRequestProperty("Content-Type", AgentConstants.APPLICATION_JSON_TYPE);
            httpConnection.setDoOutput(false);

            responseCode = httpConnection.getResponseCode();
            responseFromServer = CommunicationUtils.readResponseFromHttpRequest(httpConnection);
            httpConnection.disconnect();

            this.serverCACapabilities = responseFromServer;

            //TODO:: Need get server cert
            log.info("Response Code for GetCACAps:" + responseCode);
            log.info("Response from server:" + responseFromServer);

        } catch (CommunicationHandlerException comHandlerEx) {

        } catch (ProtocolException protocolEx) {

        } catch (IOException IOEx) {

        }
    }
}
