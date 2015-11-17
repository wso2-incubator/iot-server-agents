//TODO:: Licence Text and need to move this to a CDMF components

package org.wso2.carbon.device.mgt.iot.agent.firealarm.transport;

public interface CommunicationHandler<T> {
	int DEFAULT_TIMEOUT_INTERVAL = 5000;      // millis ~ 10 sec

	void connect();

	boolean isConnected();

	//TODO:: Any errors needs to be thrown ahead
	void processIncomingMessage(T message);

	void processIncomingMessage();

	void publishDeviceData(int publishInterval);

	void disconnect();
}
