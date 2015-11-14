package org.wso2.carbon.device.mgt.iot.agent.firealarm.enrollment.scep;

public enum SCEPOperation {
    GET_CA_CERT("GetCACert"),
    GET_CA_CAPS("GetCACaps"),
    PKI_OPERATION("PKIOperation");

    private String value;

    private SCEPOperation(String value) {
        this.setValue(value);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
