/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.communication.CommunicationHandler;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.communication.CommunicationUtils;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.communication.http
		.HTTPCommunicationHandlerImpl;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.communication.mqtt
		.MQTTCommunicationHandlerImpl;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.communication.xmpp
		.XMPPCommunicationHandlerImpl;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.exception.AgentCoreOperationException;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.ui.AgentUI;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class AgentManager {

	private static final Log log = LogFactory.getLog(AgentManager.class);

	private static AgentManager agentManager = new AgentManager();
	private AgentUI agentUI;

	private int temperature = 30, humidity = 30;
	private int temperatureMin = 20, temperatureMax = 50, humidityMin = 20, humidityMax = 50;
	private boolean isTemperatureRandomized, isHumidityRandomized;

	private String deviceMgtControlUrl, deviceMgtAnalyticUrl;
	private String deviceName, deviceStatus;
	private int dataPushInterval;       // millis = 15 seconds
	private String prevProtocol, protocol;

	private String networkInterface;

	private AgentConfiguration agentConfigs;

	private String deviceIP;
	private String controllerAPIEP;
	private String ipRegistrationEP;
	private String pushDataAPIEP;

	private AgentManager() {

	}

	public static AgentManager getInstance() {
		return agentManager;
	}

	public void init() {

		// Read IoT-Server specific configurations from the 'deviceConfig.properties' file
		this.agentConfigs = AgentCoreOperations.readIoTServerConfigs();

		String analyticsPageContext = String.format(AgentConstants.DEVICE_ANALYTICS_PAGE_URL,
		                                            agentConfigs.getDeviceId(),
		                                            AgentConstants.DEVICE_TYPE);

		String controlPageContext = String.format(AgentConstants.DEVICE_DETAILS_PAGE_EP,
		                                          AgentConstants.DEVICE_TYPE,
		                                          agentConfigs.getDeviceId());

		this.deviceMgtAnalyticUrl = agentConfigs.getHTTPS_ServerEndpoint() + analyticsPageContext;
		this.deviceMgtControlUrl = agentConfigs.getHTTPS_ServerEndpoint() + controlPageContext;
		this.deviceName = this.agentConfigs.getDeviceName();

		this.dataPushInterval = AgentConstants.DEFAULT_DATA_PUBLISH_INTERVAL;
		this.networkInterface = AgentConstants.DEFAULT_NETWORK_INTERFACE;

		this.protocol = AgentConstants.HTTP_PROTOCOL;
		this.prevProtocol = AgentConstants.HTTP_PROTOCOL;
		this.deviceStatus = AgentConstants.NOT_REGISTERED;

		try {
			// Set System L&F
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException e) {
			log.error(
					"An 'UnsupportedLookAndFeelException' error occurred whilst initializing the " +
							"Agent UI.");
		} catch (ClassNotFoundException e) {
			log.error(
					"An 'ClassNotFoundException' error occurred whilst initializing the Agent UI" +
							".");
		} catch (InstantiationException e) {
			log.error(
					"An 'InstantiationException' error occurred whilst initializing the Agent UI" +
							".");
		} catch (IllegalAccessException e) {
			log.error(
					"An 'IllegalAccessException' error occurred whilst initializing the Agent UI" +
							".");
		}

		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				agentUI = new AgentUI();
				agentUI.setVisible(true);
			}
		});


		// Initialise IoT-Server URL endpoints from the configuration read from file
		AgentCoreOperations.initializeHTTPEndPoints();

		Map<String, String> ipPortMap = null;
		try {
			ipPortMap = CommunicationUtils.getHostAndPort(agentConfigs.getXmppServerEndpoint());
		} catch (AgentCoreOperationException e) {
//			TODO:: Handle Exception
		}

		String server = ipPortMap.get("Host");
		int port = Integer.parseInt(ipPortMap.get("Port"));

		CommunicationHandler httpCommunicator = new HTTPCommunicationHandlerImpl();
		CommunicationHandler xmppCommunicator = new XMPPCommunicationHandlerImpl(server, port);
		CommunicationHandler mqttCommunicator = new MQTTCommunicationHandlerImpl(
				agentConfigs.getDeviceOwner(), agentConfigs.getDeviceId(),
				agentConfigs.getMqttBrokerEndpoint(), AgentConstants.MQTT_SUBSCRIBE_TOPIC);

		Map<String, CommunicationHandler> agentCommunicator = new HashMap<>();
		agentCommunicator.put(AgentConstants.HTTP_PROTOCOL, httpCommunicator);
		agentCommunicator.put(AgentConstants.XMPP_PROTOCOL, xmppCommunicator);
		agentCommunicator.put(AgentConstants.MQTT_PROTOCOL, mqttCommunicator);

		agentCommunicator.get(protocol).initializeConnection();

		while (true) {
			if (!protocol.equals(prevProtocol)) {
				agentCommunicator.get(prevProtocol).terminateConnection();
				agentCommunicator.get(protocol).initializeConnection();
			}
		}
	}


	public void changeBulbStatus(boolean isOn) {
		agentUI.setBulbStatus(isOn);
	}

	public void updateAgentStatus(String status) {
		this.deviceStatus = status;
	}

	private int getRandom(int max, int min) {
		double rnd = Math.random() * (max - min) + min;
		return (int) Math.round(rnd);
	}

	/*------------------------------------------------------------------------------------------*/
	/* 		            Getter and Setter Methods for the private variables                 	*/
	/*------------------------------------------------------------------------------------------*/

	public int getTemperature() {
		if (isTemperatureRandomized) {
			temperature = getRandom(temperatureMax, temperatureMin);
			agentUI.updateTemperature(temperature);
		}
		return temperature;
	}

	public void setTemperature(int temperature) {
		this.temperature = temperature;
	}

	public int getHumidity() {
		if (isHumidityRandomized) {
			humidity = getRandom(humidityMax, humidityMin);
			agentUI.updateHumidity(humidity);
		}
		return humidity;
	}

	public void setHumidity(int humidity) {
		this.humidity = humidity;
	}

	public void setTemperatureMin(int temperatureMin) {
		this.temperatureMin = temperatureMin;
	}

	public void setTemperatureMax(int temperatureMax) {
		this.temperatureMax = temperatureMax;
	}

	public void setHumidityMin(int humidityMin) {
		this.humidityMin = humidityMin;
	}

	public void setHumidityMax(int humidityMax) {
		this.humidityMax = humidityMax;
	}

	public void setIsHumidityRandomized(boolean isHumidityRandomized) {
		this.isHumidityRandomized = isHumidityRandomized;
	}

	public void setIsTemperatureRandomized(boolean isTemperatureRandomized) {
		this.isTemperatureRandomized = isTemperatureRandomized;
	}

	public String getDeviceMgtControlUrl() {
		return deviceMgtControlUrl;
	}

	public String getDeviceMgtAnalyticUrl() {
		return deviceMgtAnalyticUrl;
	}

	public AgentConfiguration getAgentConfigs() {
		return agentConfigs;
	}

	public void setAgentConfigs(AgentConfiguration agentConfigs) {
		this.agentConfigs = agentConfigs;
	}

	public String getDeviceIP() {
		return deviceIP;
	}

	public void setDeviceIP(String deviceIP) {
		this.deviceIP = deviceIP;
	}

	public String getControllerAPIEP() {
		return controllerAPIEP;
	}

	public void setControllerAPIEP(String controllerAPIEP) {
		this.controllerAPIEP = controllerAPIEP;
	}

	public String getIpRegistrationEP() {
		return ipRegistrationEP;
	}

	public void setIpRegistrationEP(String ipRegistrationEP) {
		this.ipRegistrationEP = ipRegistrationEP;
	}

	public String getPushDataAPIEP() {
		return pushDataAPIEP;
	}

	public void setPushDataAPIEP(String pushDataAPIEP) {
		this.pushDataAPIEP = pushDataAPIEP;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public String getDeviceStatus() {
		return deviceStatus;
	}

	public int getDataPushInterval() {
		return dataPushInterval;
	}

	public String getNetworkInterface() {
		return networkInterface;
	}
}
