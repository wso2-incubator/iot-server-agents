package org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.communication.mqtt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.communication
		.CommunicationHandlerException;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.core.AgentConstants;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.core.AgentManager;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MQTTCommunicationHandlerImpl extends MQTTCommunicationHandler {

	private static final Log log = LogFactory.getLog(MQTTCommunicationHandlerImpl.class);

	private static final AgentManager agentManager = AgentManager.getInstance();
	private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
	private int dataPublishInterval;

	public MQTTCommunicationHandlerImpl(String deviceOwner, String deviceType,
	                                    String mqttBrokerEndPoint, String subscribeTopic) {
		super(deviceOwner, deviceType, mqttBrokerEndPoint, subscribeTopic);
		dataPublishInterval = AgentConstants.DEFAULT_DATA_PUBLISH_INTERVAL;
	}

	public MQTTCommunicationHandlerImpl(String deviceOwner, String deviceType,
	                                    String mqttBrokerEndPoint, String subscribeTopic,
	                                    int intervalInMillis) {
		super(deviceOwner, deviceType, mqttBrokerEndPoint, subscribeTopic, intervalInMillis);
		dataPublishInterval = AgentConstants.DEFAULT_DATA_PUBLISH_INTERVAL;
	}

	public void setDataPublishInterval(int dataPublishInterval) {
		this.dataPublishInterval = dataPublishInterval;
	}

	@Override
	public void initializeConnection() {
		try {
			connectToQueue();
			subscribeToQueue();
		} catch (CommunicationHandlerException e) {
			log.warn(AgentConstants.LOG_APPENDER + "Connection/Subscription to MQTT Broker at: " +
					         mqttBrokerEndPoint + " failed");

			Runnable reconnect = new Runnable() {
				public void run() {
					attemptReconnection();
				}
			};
			Thread reconnectThread = new Thread(reconnect);
			reconnectThread.setDaemon(true);
			reconnectThread.start();
		}

		publishDeviceData();
	}

	@Override
	public void attemptReconnection() {
		while (!isConnected()) {
			if (log.isDebugEnabled()) {
				log.debug(
						AgentConstants.LOG_APPENDER + "Subscriber trying to reach MQTT queue....");
			}

			try {
				connectToQueue();
				subscribeToQueue();
			} catch (CommunicationHandlerException e1) {
				if (log.isDebugEnabled()) {
					log.debug(AgentConstants.LOG_APPENDER +
							          "Attempt to connect/subscribe to MQTT-Queue failed");
				}

				try {
					Thread.sleep(timeoutInterval);
				} catch (InterruptedException e) {
					log.error("MQTT-Subscriber: Thread Sleep Interrupt Exception");
				}
			}
		}
	}

	@Override
	public void processIncomingMessage(MqttMessage message) {
		log.info(AgentConstants.LOG_APPENDER + "Message " + message.toString() + " was received");

		String deviceOwner = agentManager.getAgentConfigs().getDeviceOwner();
		String deviceID = agentManager.getAgentConfigs().getDeviceId();
		String replyMessage;

		String[] controlSignal = message.toString().split(":");
		// message- "<SIGNAL_TYPE>:<SIGNAL_MODE>" format.(ex: "BULB:ON", "TEMPERATURE", "HUMIDITY")

		switch (controlSignal[0].toUpperCase()) {
			case AgentConstants.BULB_CONTROL:
				agentManager.changeBulbStatus(controlSignal[1].equals(AgentConstants.CONTROL_ON));
				log.info(AgentConstants.LOG_APPENDER + "Bulb was switched to state: '" +
						         controlSignal[1] + "'");
				break;

			case AgentConstants.TEMPERATURE_CONTROL:
				int currentTemperature = agentManager.getTemperature();

				String replyTemperature =
						"Current temperature was read as: '" + currentTemperature + "C'";
				log.info(AgentConstants.LOG_APPENDER + replyTemperature);

				String tempPublishTopic = String.format(
						AgentConstants.MQTT_PUBLISH_TOPIC, deviceOwner, deviceID);
				replyMessage = AgentConstants.TEMPERATURE_CONTROL + ":" + currentTemperature;

				try {
					publishToQueue(tempPublishTopic, replyMessage);
				} catch (CommunicationHandlerException e) {
//					TODO:: Handle Exception
				}
				break;

			case AgentConstants.HUMIDITY_CONTROL:
				int currentHumidity = agentManager.getHumidity();

				String replyHumidity =
						"Current humidity was read as: '" + currentHumidity + "%'";
				log.info(AgentConstants.LOG_APPENDER + replyHumidity);

				String humidPublishTopic = String.format(
						AgentConstants.MQTT_PUBLISH_TOPIC, deviceOwner, deviceID);
				replyMessage = AgentConstants.HUMIDITY_CONTROL + ":" + currentHumidity;

				try {
					publishToQueue(humidPublishTopic, replyMessage);
				} catch (CommunicationHandlerException e) {
//					TODO:: Handle Exception
				}
				break;

			default:
				log.warn(
						"'" + controlSignal[0] + "' is invalid and not-supported for " +
								"this device-type");
				break;
		}
	}


	@Override
	public void publishDeviceData() {
		Runnable pushDataRunnable = new Runnable() {
			@Override
			public void run() {
				int currentTemperature = agentManager.getTemperature();
				String payLoad =
						"PUBLISHER:" + AgentConstants.TEMPERATURE_CONTROL + ":" + currentTemperature;

				MqttMessage pushMessage = new MqttMessage();
				pushMessage.setPayload(payLoad.getBytes(StandardCharsets.UTF_8));
				pushMessage.setQos(DEFAULT_MQTT_QUALITY_OF_SERVICE);
				pushMessage.setRetained(true);

				try {
					publishToQueue(AgentConstants.MQTT_PUBLISH_TOPIC, pushMessage);
				} catch (CommunicationHandlerException e) {
					log.warn("Data Publish attempt to topic - [" +
							         AgentConstants.MQTT_PUBLISH_TOPIC + "] failed for payload [" +
							         payLoad + "]");
				}
			}
		};

		service.scheduleAtFixedRate(pushDataRunnable, 0, dataPublishInterval, TimeUnit.SECONDS);
	}


	@Override
	public void terminateConnection() {
		Runnable stopConnection = new Runnable() {
			public void run() {
				while (isConnected()) {
					try {
						service.shutdown();
						closeConnection();
					} catch (MqttException e) {
						if (log.isDebugEnabled()) {
							log.warn("Unable to 'STOP' MQTT connection at broker at: " +
									         mqttBrokerEndPoint);
						}

						try {
							Thread.sleep(timeoutInterval);
						} catch (InterruptedException e1) {
							log.error("MQTT-Terminator: Thread Sleep Interrupt Exception");
						}
					}
				}
			}
		};

		Thread terminatorThread = new Thread(stopConnection);
		terminatorThread.setDaemon(true);
		terminatorThread.start();
	}

	@Override
	public void processIncomingMessage() {

	}

}
