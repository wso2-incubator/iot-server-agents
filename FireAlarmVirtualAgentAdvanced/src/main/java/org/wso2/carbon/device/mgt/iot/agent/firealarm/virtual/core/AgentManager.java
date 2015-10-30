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
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.communication
		.CommunicationHandlerException;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.communication.CommunicationUtils;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.sidhdhi.SidhdhiQuery;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.ui.AgentUI;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.utils.http
		.HTTPCommunicationHandlerImpl;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.utils.mqtt
		.MQTTCommunicationHandlerImpl;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.utils.xmpp
		.XMPPCommunicationHandlerImpl;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgentManager {

	private static final Log log = LogFactory.getLog(AgentManager.class);

	private static AgentManager agentManager = new AgentManager();
	private AgentUI agentUI;
	private String rootPath = "";

	private boolean UIReady = false;
	private static Boolean policyUpdated = false;
	private static final Object lock = new Object();
	private String initialPolicy;
	private Sequencer sequencer = null;

	private int temperature = 30, humidity = 30;
	private int temperatureMin = 20, temperatureMax = 50, humidityMin = 20, humidityMax = 50;
	private int temperatureSVF = 50, humiditySVF = 50;
	private boolean isTemperatureRandomized, isHumidityRandomized;
	private boolean isTemperatureSmoothed, isHumiditySmoothed;
	private String deviceMgtControlUrl, deviceMgtAnalyticUrl;
	private String deviceName, agentStatus;

	private int pushInterval;               // seconds
	private String prevProtocol, protocol;

	private String networkInterface;
	private List<String> interfaceList, protocolList;
	private Map<String, CommunicationHandler> agentCommunicator = new HashMap<>();

	private AgentConfiguration agentConfigs;

	private String deviceIP;
	private String ipRegistrationEP;
	private String pushDataAPIEP;

	private AgentManager() {

	}

	public static AgentManager getInstance() {
		return agentManager;
	}

	public void init() {

		// Read IoT-Server specific configurations from the 'deviceConfig.properties' file
		this.agentConfigs = AgentUtilOperations.readIoTServerConfigs();

		// Initialize Audio Sequencer to play Fire-Alarm-Audio
//		setAudioSequencer();

		// Initialise IoT-Server URL endpoints from the configuration read from file
		AgentUtilOperations.initializeHTTPEndPoints();

		String analyticsPageContext = String.format(AgentConstants.DEVICE_ANALYTICS_PAGE_URL,
		                                            agentConfigs.getDeviceId(),
		                                            AgentConstants.DEVICE_TYPE);

		String controlPageContext = String.format(AgentConstants.DEVICE_DETAILS_PAGE_EP,
		                                          AgentConstants.DEVICE_TYPE,
		                                          agentConfigs.getDeviceId());

		this.deviceMgtAnalyticUrl = agentConfigs.getHTTPS_ServerEndpoint() + analyticsPageContext;
		this.deviceMgtControlUrl = agentConfigs.getHTTPS_ServerEndpoint() + controlPageContext;

		this.agentStatus = AgentConstants.NOT_REGISTERED;
		this.deviceName = this.agentConfigs.getDeviceName();

		this.pushInterval = this.agentConfigs.getDataPushInterval();
		this.networkInterface = AgentConstants.DEFAULT_NETWORK_INTERFACE;

		this.protocol = AgentConstants.DEFAULT_PROTOCOL;
		this.prevProtocol = protocol;


		Map<String, String> xmppIPPortMap = null;
		try {
			xmppIPPortMap = CommunicationUtils.getHostAndPort(agentConfigs.getXmppServerEndpoint
					());
		} catch (CommunicationHandlerException e) {
			log.error("XMPP Endpoint String - " + agentConfigs.getXmppServerEndpoint() +
					          ", provided in the configuration file is invalid.");
		}

		String xmppServer = xmppIPPortMap.get("Host");
		int xmppPort = Integer.parseInt(xmppIPPortMap.get("Port"));

		String mqttTopic = String.format(AgentConstants.MQTT_SUBSCRIBE_TOPIC,
		                                 agentConfigs.getDeviceOwner(),
		                                 agentConfigs.getDeviceId());

		CommunicationHandler httpCommunicator = new HTTPCommunicationHandlerImpl();
		CommunicationHandler xmppCommunicator = new XMPPCommunicationHandlerImpl(xmppServer,
		                                                                         xmppPort);
		CommunicationHandler mqttCommunicator = new MQTTCommunicationHandlerImpl(
				agentConfigs.getDeviceOwner(), agentConfigs.getDeviceId(),
				agentConfigs.getMqttBrokerEndpoint(), mqttTopic);

		agentCommunicator.put(AgentConstants.HTTP_PROTOCOL, httpCommunicator);
		agentCommunicator.put(AgentConstants.XMPP_PROTOCOL, xmppCommunicator);
		agentCommunicator.put(AgentConstants.MQTT_PROTOCOL, mqttCommunicator);

		try {
			interfaceList = new ArrayList<String>(CommunicationUtils.getInterfaceIPMap().keySet());
			protocolList = new ArrayList<String>(agentCommunicator.keySet());
		} catch (CommunicationHandlerException e) {
			log.error("An error occurred whilst retrieving all NetworkInterface-IP mappings");
		}

 		try {
			// Set System L&F
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException e) {
			log.error(
					"'UnsupportedLookAndFeelException' error occurred whilst initializing the" +
							" Agent UI.");
		} catch (ClassNotFoundException e) {
			log.error(
					"'ClassNotFoundException' error occurred whilst initializing the Agent UI.");
		} catch (InstantiationException e) {
			log.error(
					"'InstantiationException' error occurred whilst initializing the Agent UI.");
		} catch (IllegalAccessException e) {
			log.error(
					"'IllegalAccessException' error occurred whilst initializing the Agent UI.");
		}

		String siddhiQueryFilePath = rootPath + AgentConstants.CEP_FILE_NAME;
		initialPolicy = SidhdhiQuery.readFile(siddhiQueryFilePath, StandardCharsets.UTF_8);
		(new Thread(new SidhdhiQuery())).start();

		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				agentUI = new AgentUI();
				agentUI.setVisible(true);
			}
		});


		while (!UIReady) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				log.info(AgentConstants.LOG_APPENDER + "Sleep error in 'UIReady-flag' checking thread");
			}
		}

		setAudioSequencer();
		agentCommunicator.get(protocol).connect();
	}


	public static void setUpdated(Boolean x) {
		synchronized (lock) {
			policyUpdated = x;
		}
	}

	public static Boolean isUpdated() {
		synchronized (lock) {
			Boolean temp = policyUpdated;
			policyUpdated = false;
			return temp;
		}
	}

	private void setAudioSequencer(){
		InputStream audioSrc = AgentUtilOperations.class.getResourceAsStream(
				"/" + AgentConstants.AUDIO_FILE_NAME);
		Sequence sequence = null;

		try {
			sequence = MidiSystem.getSequence(audioSrc);
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
			sequencer.setSequence(sequence);
		} catch (InvalidMidiDataException e) {
			log.error("AudioReader: Error whilst setting MIDI Audio reader sequence");
		} catch (IOException e) {
			log.error("AudioReader: Error whilst getting audio sequence from stream");
		} catch (MidiUnavailableException e) {
			log.error("AudioReader: Error whilst openning MIDI Audio reader sequencer");
		}

		sequencer.setLoopCount(Clip.LOOP_CONTINUOUSLY);
	}

	private void switchCommunicator(String stopProtocol, String startProtocol) {
		agentCommunicator.get(stopProtocol).disconnect();

		while (agentCommunicator.get(stopProtocol).isConnected()) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				log.info(AgentConstants.LOG_APPENDER +
						         "Sleep error in 'Switch-Communicator' Thread's shutdown wait.");
			}
		}

		agentCommunicator.get(startProtocol).connect();
	}


	public void setPushInterval(int pushInterval) {
		this.pushInterval = pushInterval;
		CommunicationHandler communicationHandler = agentCommunicator.get(protocol);

		switch (protocol) {
			case AgentConstants.HTTP_PROTOCOL:
				((HTTPCommunicationHandlerImpl) communicationHandler).getDataPushServiceHandler()
						.cancel(true);
				break;
			case AgentConstants.MQTT_PROTOCOL:
				((MQTTCommunicationHandlerImpl) communicationHandler).getDataPushServiceHandler()
						.cancel(true);
				break;
			case AgentConstants.XMPP_PROTOCOL:
				((XMPPCommunicationHandlerImpl) communicationHandler).getDataPushServiceHandler()
						.cancel(true);
				break;
		}
		communicationHandler.publishDeviceData(pushInterval);

		if (log.isDebugEnabled()) {
			log.debug("The Data Publish Interval was changed to: " + pushInterval);
		}
	}


	public void setInterface(int interfaceId) {
		if (interfaceId != -1) {
			String newInterface = interfaceList.get(interfaceId);

			if (!newInterface.equals(networkInterface)) {
				networkInterface = newInterface;

				if (protocol.equals(AgentConstants.HTTP_PROTOCOL) && !protocol.equals(
						prevProtocol)) {
					switchCommunicator(protocol, protocol);
				}
			}
		}
	}

	public void setProtocol(int protocolId) {
		if (protocolId != -1) {
			String newProtocol = protocolList.get(protocolId);

			if (!protocol.equals(newProtocol)) {
				prevProtocol = protocol;
				protocol = newProtocol;
				switchCommunicator(prevProtocol, protocol);
			}
		}
	}

	public void changeBulbStatus(boolean isOn) {
		agentUI.setBulbStatus(isOn);

		if (isOn) {
			sequencer.start();
		} else {
			sequencer.stop();
		}

		SidhdhiQuery.isBulbOn = isOn;
	}

	public void updateAgentStatus(String status) {
		this.agentStatus = status;
	}

	private int getRandom(int max, int min, int current, boolean isSmoothed, int svf) {

		if (isSmoothed) {
			int offset = (max - min) * svf / 100;
			double mx = current + offset;
			max = (mx > max) ? max : (int) Math.round(mx);

			double mn = current - offset;
			min = (mn < min) ? min : (int) Math.round(mn);
		}

		double rnd = Math.random() * (max - min) + min;
		return (int) Math.round(rnd);

	}

	public void addToPolicyLog(String policy) {
		agentUI.addToPolicyLog(policy);
	}

	/*------------------------------------------------------------------------------------------*/
	/* 		            Getter and Setter Methods for the private variables                 	*/
	/*------------------------------------------------------------------------------------------*/

	public String getRootPath() {
		return rootPath;
	}

	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}

	public void setUIReady(boolean UIReady) {
		this.UIReady = UIReady;
	}

	public String getInitialPolicy() {
		return initialPolicy;
	}

	public int getTemperature() {
		if (isTemperatureRandomized) {
			temperature = getRandom(temperatureMax, temperatureMin, temperature,
			                        isTemperatureSmoothed, temperatureSVF);
			agentUI.updateTemperature(temperature);
		}
		return temperature;
	}

	public void setTemperature(int temperature) {
		this.temperature = temperature;
	}

	public int getHumidity() {
		if (isHumidityRandomized) {
			humidity = getRandom(humidityMax, humidityMin, humidity, isHumiditySmoothed,
			                     humiditySVF);
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

	public String getDeviceIP() {
		return deviceIP;
	}

	public void setDeviceIP(String deviceIP) {
		this.deviceIP = deviceIP;
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

	public String getNetworkInterface() {
		return networkInterface;
	}

	public String getAgentStatus() {
		return agentStatus;
	}

	public int getPushInterval() {
		return pushInterval;
	}

	public List<String> getInterfaceList() {
		return interfaceList;
	}

	public List<String> getProtocolList() {
		return protocolList;
	}

	public void setTemperatureSVF(int temperatureSVF) {
		this.temperatureSVF = temperatureSVF;
	}

	public void setHumiditySVF(int humiditySVF) {
		this.humiditySVF = humiditySVF;
	}

	public void setIsTemperatureSmoothed(boolean isTemperatureSmoothed) {
		this.isTemperatureSmoothed = isTemperatureSmoothed;
	}

	public void setIsHumiditySmoothed(boolean isHumiditySmoothed) {
		this.isHumiditySmoothed = isHumiditySmoothed;
	}
}
