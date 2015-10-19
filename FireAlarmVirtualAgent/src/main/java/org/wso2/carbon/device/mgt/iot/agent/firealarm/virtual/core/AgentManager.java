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
import org.eclipse.jetty.http.HttpStatus;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.exception.AgentCoreOperationException;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.ui.AgentUI;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.utils.http.SimpleServer;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.utils.mqtt.MQTTClient;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.utils.xmpp.XMPPClient;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class AgentManager {

	private static final Log log = LogFactory.getLog(AgentManager.class);

	private static AgentManager agentManager = new AgentManager();
	private AgentUI agentUI;
	private int pushInterval;
	private int temperature = 30, humidity = 30;
	private int temperatureMin = 20, temperatureMax = 50, humidityMin = 20, humidityMax = 50;
    private int temperatureSVF = 50, humiditySVF = 50;
	private boolean isTemperatureRandomized, isHumidityRandomized;
    private boolean isTemperatureSmoothed, isHumiditySmoothed;
	private String deviceMgtControlUrl, deviceMgtAnalyticUrl, deviceName, agentStatus;

	private AgentConfiguration agentConfigs;

	private SimpleServer simpleServer;
	private MQTTClient agentMQTTClient;
	private XMPPClient agentXMPPClient;
	private String xmppAdminJID;

	private String deviceIP;
	private String controllerAPIEP;
	private String ipRegistrationEP;
	private String pushDataAPIEP;

    private List<String> interfaceList, protocolList;

	Runnable ipRegister = new Runnable() {
		@Override
		public void run() {
			while (true) {
				try {
					int responseCode = AgentCoreOperations.registerDeviceIP(
							agentConfigs.getDeviceOwner(), agentConfigs.getDeviceId());

					if (responseCode == HttpStatus.OK_200) {
						updateAgentStatus("Registered");
						break;
					} else {
						log.error(AgentConstants.LOG_APPENDER +
								          "Device Registration with IoT Server at:" +
								          " " + ipRegistrationEP + " failed with response - '" +
								          responseCode + ":" + HttpStatus.getMessage(responseCode) +
								          "'");
						updateAgentStatus("Registration failed. Re-trying..");
					}
				} catch (AgentCoreOperationException exception) {
					log.error(AgentConstants.LOG_APPENDER +
							          "Error encountered whilst trying to register the Device's " +
							          "IP at: " + ipRegistrationEP +
							          ".\nCheck whether the network-interface provided is " +
							          "accurate");
					updateAgentStatus("Registration failed");
				}

				try {
					Thread.sleep(AgentConstants.DEFAULT_RETRY_THREAD_INTERVAL);
				} catch (InterruptedException e1) {
					log.error("Device Registration: Thread Sleep Interrupt Exception");
				}
			}
		}
	};

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

		String deviceControlPageContext = String.format(AgentConstants.AGENT_CONTROL_APP_EP,
		                                                AgentConstants.DEVICE_TYPE,
		                                                agentConfigs.getDeviceId());

		this.deviceMgtAnalyticUrl = agentConfigs.getHTTPS_ServerEndpoint() + analyticsPageContext;
		this.deviceMgtControlUrl =
				agentConfigs.getHTTPS_ServerEndpoint() + deviceControlPageContext;

		try {
			// Set System L&F
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException e) {
			// handle exception
		} catch (ClassNotFoundException e) {
			// handle exception
		} catch (InstantiationException e) {
			// handle exception
		} catch (IllegalAccessException e) {
			// handle exception
		}

		this.agentStatus = "Not Connected";
		this.deviceName = this.agentConfigs.getDeviceName();

        //TODO: Get agent name from configs
        //this.agentName = this.agentConfigs.getAgentName();
        //TODO: Remove this line after getting agent name from configs
        this.deviceName = "WSO2 Virtual Agent";
        //TODO: Set ip interfaces
        interfaceList = new ArrayList<>();
        interfaceList.add("eth0");

        protocolList = new ArrayList<>();
        protocolList.add("MQTT");
        protocolList.add("XMPP");
        protocolList.add("HTTP");

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                agentUI = new AgentUI();
                agentUI.setVisible(true);
            }
        });

		// Initialise IoT-Server URL endpoints from the configuration read from file
		AgentCoreOperations.initializeHTTPEndPoints();

		// Register this current device's IP with the IoT-Server
		this.registerThisDevice();

		// Initiate the thread for continuous pushing of device data to the IoT-Server
		AgentCoreOperations.initiateDeviceDataPush(agentConfigs.getDeviceOwner(),
		                                           agentConfigs.getDeviceId(),
		                                           agentConfigs.getDataPushInterval());

		// Subscribe to the platform's MQTT Queue for receiving Control Signals via MQTT
		try {
			AgentCoreOperations.subscribeToMQTT(this.agentConfigs.getDeviceOwner(),
			                                    this.agentConfigs.getDeviceId(),
			                                    this.agentConfigs.getMqttBrokerEndpoint());
		} catch (AgentCoreOperationException e) {
			log.error(AgentConstants.LOG_APPENDER + "Subscription to MQTT Broker at: " +
					          this.agentConfigs.getMqttBrokerEndpoint() + " failed");
			retryMQTTSubscription();
		}

		// Connect to the platform's XMPP Server for receiving Control Signals via XMPP
		try {
			AgentCoreOperations.connectToXMPPServer(this.agentConfigs.getDeviceId(),
			                                        this.agentConfigs.getAuthToken(),
			                                        this.agentConfigs.getDeviceOwner(),
			                                        this.agentConfigs.getXmppServerEndpoint());
		} catch (AgentCoreOperationException e) {
			log.error(AgentConstants.LOG_APPENDER + "Connect/Login attempt to XMPP Server at: " +
					          this.agentConfigs.getXmppServerEndpoint() + " failed");
			retryXMPPConnection();
		}

		// Start a simple HTTP Server to receive Control Signals via HTTP
		try {
			simpleServer = new SimpleServer();
		} catch (AgentCoreOperationException e) {
			log.error(AgentConstants.LOG_APPENDER + "Failed to start HTTP Server");
			retryHTTPServerInit();
		}

	}

	public void registerThisDevice() {
		this.agentStatus = "Registering";
		Thread ipRegisterThread = new Thread(ipRegister);
		ipRegisterThread.setDaemon(true);
		ipRegisterThread.start();
	}

	private void retryMQTTSubscription() {
		Thread retryToSubscribe = new Thread() {
			@Override
			public void run() {
				while (true) {
					if (!agentMQTTClient.isConnected()) {
						if (log.isDebugEnabled()) {
							log.debug(AgentConstants.LOG_APPENDER +
									          "Subscriber re-trying to reach MQTT queue....");
						}

						try {
							agentMQTTClient.connectAndSubscribe();
						} catch (AgentCoreOperationException e1) {
							if (log.isDebugEnabled()) {
								log.debug(AgentConstants.LOG_APPENDER +
										          "Attempt to re-connect to MQTT-Queue " +
										          "failed");
							}
						}
					} else {
						break;
					}

					try {
						Thread.sleep(AgentConstants.DEFAULT_RETRY_THREAD_INTERVAL);
					} catch (InterruptedException e1) {
						log.error("MQTT: Thread S;eep Interrupt Exception");
					}
				}
			}
		};

		retryToSubscribe.setDaemon(true);
		retryToSubscribe.start();
	}

	private void retryXMPPConnection() {
		Thread retryToConnect = new Thread() {
			@Override
			public void run() {
				while (true) {
					if (!agentXMPPClient.isConnected()) {
						if (log.isDebugEnabled()) {
							log.debug(AgentConstants.LOG_APPENDER +
									          "Re-trying to reach XMPP Server....");
						}

						try {
							agentXMPPClient.connectAndLogin(agentConfigs.getDeviceId(),
							                                agentConfigs.getAuthToken(),
							                                agentConfigs.getDeviceOwner());
							agentXMPPClient.setMessageFilterAndListener(xmppAdminJID);
						} catch (AgentCoreOperationException e1) {
							if (log.isDebugEnabled()) {
								log.debug(AgentConstants.LOG_APPENDER +
										          "Attempt to re-connect to XMPP-Server " +
										          "failed");
							}
						}
					} else {
						break;
					}

					try {
						Thread.sleep(AgentConstants.DEFAULT_RETRY_THREAD_INTERVAL);
					} catch (InterruptedException e1) {
						log.error("XMPP: Thread Sleep Interrupt Exception");
					}
				}
			}
		};

		retryToConnect.setDaemon(true);
		retryToConnect.start();
	}

	private void retryHTTPServerInit() {
		Thread restartServer = new Thread() {
			@Override
			public void run() {
				while (true) {
					if (!simpleServer.getServer().isStarted()) {
						if (log.isDebugEnabled()) {
							log.debug(AgentConstants.LOG_APPENDER +
									          "Re-trying to start HTTP Server....");
						}

						try {
							simpleServer.getServer().start();
						} catch (Exception e) {
							if (log.isDebugEnabled()) {
								log.debug(AgentConstants.LOG_APPENDER +
										          "Attempt to restart HTTP-Server failed");
							}
						}
					} else {
						break;
					}

					try {
						Thread.sleep(AgentConstants.DEFAULT_RETRY_THREAD_INTERVAL);
					} catch (InterruptedException e1) {
						log.error("HTTP: Thread Sleep Interrupt Exception");
					}
				}
			}
		};

		restartServer.setDaemon(true);
		restartServer.start();
	}


	public void changeBulbStatus(boolean isOn) {
		agentUI.setBulbStatus(isOn);
	}

	public void updateAgentStatus(String status) {
		this.agentStatus = status;
	}

	public int getTemperature() {
		if (isTemperatureRandomized) {
			temperature = getRandom(temperatureMax, temperatureMin, temperature, isTemperatureSmoothed, temperatureSVF);
			agentUI.updateTemperature(temperature);
		}
		return temperature;
	}

	public void setTemperature(int temperature) {
		this.temperature = temperature;
	}

	public int getHumidity() {
		if (isHumidityRandomized) {
			humidity = getRandom(humidityMax, humidityMin, humidity, isHumiditySmoothed, humiditySVF);
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

	public void setDeviceMgtAnalyticUrl(String deviceMgtAnalyticUrl) {
		this.deviceMgtAnalyticUrl = deviceMgtAnalyticUrl;
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

	/*------------------------------------------------------------------------------------------*/
	/* 		            Getter and Setter Methods for the private variables                 	*/
	/*------------------------------------------------------------------------------------------*/

	public AgentConfiguration getAgentConfigs() {
		return agentConfigs;
	}

	public void setAgentConfigs(AgentConfiguration agentConfigs) {
		this.agentConfigs = agentConfigs;
	}

	public SimpleServer getSimpleServer() {
		return simpleServer;
	}

	public MQTTClient getAgentMQTTClient() {
		return agentMQTTClient;
	}

	public void setAgentMQTTClient(MQTTClient mqttClient) {
		this.agentMQTTClient = mqttClient;
	}

	public XMPPClient getAgentXMPPClient() {
		return agentXMPPClient;
	}

	public void setAgentXMPPClient(XMPPClient xmppClient) {
		this.agentXMPPClient = xmppClient;
	}

	public String getXmppAdminJID() {
		return xmppAdminJID;
	}

	public void setXmppAdminJID(String xmppAdminJID) {
		this.xmppAdminJID = xmppAdminJID;
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

    public String getAgentStatus() {
        return agentStatus;
    }

	public int getPushInterval() {
		return pushInterval;
	}

	public void setPushInterval(int pushInterval) {
		this.pushInterval = pushInterval;
	}

    public List<String> getInterfaceList() {
        return interfaceList;
    }

    public List<String> getProtocolList() {
        return protocolList;
    }

    public void setInterface(int interfaceId) {
        //TODO: Set selected interface using id in list
    }

    public void setProtocol(int protocolId) {
        //TODO: Set selected protocol using id in list
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
