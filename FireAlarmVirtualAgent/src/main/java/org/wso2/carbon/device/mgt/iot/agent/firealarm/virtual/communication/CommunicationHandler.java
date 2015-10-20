package org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.communication;

public interface CommunicationHandler<T> {
	int DEFAULT_TIMEOUT_INTERVAL = 5000;      // millis ~ 10 sec

	void initializeConnection();

	void attemptReconnection();

	boolean isConnected();

	void processIncomingMessage(T message);

	void processIncomingMessage();

	void publishDeviceData(int publishInterval);

	void terminateConnection();
}
