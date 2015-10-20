package org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.utils.xmpp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.packet.Message;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.communication
		.CommunicationHandlerException;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.communication.xmpp
		.XMPPCommunicationHandler;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.core.AgentConstants;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.core.AgentManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class XMPPCommunicationHandlerImpl extends XMPPCommunicationHandler {

	private static final Log log = LogFactory.getLog(XMPPCommunicationHandlerImpl.class);

	private static final AgentManager agentManager = AgentManager.getInstance();
	private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
	private ScheduledFuture<?> serviceHandler;

	private String username;
	private String password;
	private String resource;
	private String xmppAdminJID;
	private String xmppDeviceJID;

	public XMPPCommunicationHandlerImpl(String server) {
		super(server);
	}

	public XMPPCommunicationHandlerImpl(String server, int port) {
		super(server, port);
	}

	public XMPPCommunicationHandlerImpl(String server, int port, int timeout) {
		super(server, port, timeout);
	}

	public ScheduledFuture<?> getServiceHandler() {
		return serviceHandler;
	}

	@Override
	public void initializeConnection() {
		username = agentManager.getAgentConfigs().getDeviceId();
		password = agentManager.getAgentConfigs().getAuthToken();
		resource = agentManager.getAgentConfigs().getDeviceOwner();

		xmppDeviceJID = username + "@" + server;
		xmppAdminJID = AgentConstants.XMPP_ADMIN_ACCOUNT_UNAME + "@" + server;

		try {
			connectToServer();
			loginToServer(username, password, resource);
			agentManager.updateAgentStatus("Connected to XMPP Server");

		} catch (CommunicationHandlerException e) {
			log.warn(AgentConstants.LOG_APPENDER + "Connection/Login to XMPP server at: " +
					         server + " failed");

			Runnable reconnect = new Runnable() {
				public void run() {
					attemptReconnection();
				}
			};
			Thread reconnectThread = new Thread(reconnect);
			reconnectThread.setDaemon(true);
			reconnectThread.start();
		}

		setMessageFilterAndListener(xmppAdminJID, xmppDeviceJID, true);
		publishDeviceData(agentManager.getPushInterval());
	}

	@Override
	public void attemptReconnection() {
		while (!isConnected()) {
			if (log.isDebugEnabled()) {
				log.debug(
						AgentConstants.LOG_APPENDER + "Subscriber trying to reach MQTT queue....");
			}

			try {
				connectToServer();
				loginToServer(username, password, resource);
				agentManager.updateAgentStatus("Connected to XMPP Server");

			} catch (CommunicationHandlerException e1) {
				if (log.isDebugEnabled()) {
					log.debug(AgentConstants.LOG_APPENDER +
							          "Attempt to connect/subscribe to MQTT-Queue failed");
				}

				try {
					Thread.sleep(timeoutInterval);
				} catch (InterruptedException e) {
					log.error("XMPP-Login: Thread Sleep Interrupt Exception");
				}
			}
		}
	}

	@Override
	public void processIncomingMessage(Message xmppMessage) {
		String from = xmppMessage.getFrom();
		String message = xmppMessage.getBody();
		log.info(AgentConstants.LOG_APPENDER + "Received XMPP message '" + message +
				         "' from " + from);

		String[] controlSignal = message.toString().split(":");
		//message- "<SIGNAL_TYPE>:<SIGNAL_MODE>" format. (ex: "BULB:ON", "TEMPERATURE", "HUMIDITY")

		switch (controlSignal[0].toUpperCase()) {
			case AgentConstants.BULB_CONTROL:
				agentManager.changeBulbStatus(
						controlSignal[1].equals(AgentConstants.CONTROL_ON) ? true : false);
				log.info(AgentConstants.LOG_APPENDER + "Bulb was switched to state: '" +
						         controlSignal[1] + "'");
				break;

			case AgentConstants.TEMPERATURE_CONTROL:
				int currentTemperature = agentManager.getTemperature();

				String replyTemperature =
						"The current temperature was read to be: '" + currentTemperature +
								"C'";
				log.info(AgentConstants.LOG_APPENDER + replyTemperature);

				sendXMPPMessage(xmppAdminJID, replyTemperature, AgentConstants
						.TEMPERATURE_CONTROL);
				break;

			case AgentConstants.HUMIDITY_CONTROL:
				int currentHumidity = agentManager.getHumidity();

				String replyHumidity =
						"The current humidity was read to be: '" + currentHumidity + "%'";
				log.info(AgentConstants.LOG_APPENDER + replyHumidity);

				sendXMPPMessage(xmppAdminJID, replyHumidity, AgentConstants.HUMIDITY_CONTROL);
				break;

			default:
				log.warn("'" + controlSignal[0] +
						         "' is invalid and not-supported for this device-type");
				break;
		}
	}

	@Override
	public void publishDeviceData(int publishInterval) {
		Runnable pushDataRunnable = new Runnable() {
			@Override
			public void run() {
				int currentTemperature = agentManager.getTemperature();
				String payLoad = AgentConstants.TEMPERATURE_CONTROL + ":" + currentTemperature;

				Message pushMessage = new Message();
				pushMessage.setTo(xmppAdminJID);
				pushMessage.setSubject("PUBLISHER");
				pushMessage.setBody(payLoad);
				pushMessage.setType(Message.Type.chat);

				sendXMPPMessage(xmppAdminJID, pushMessage);
			}
		};

		serviceHandler = service.scheduleAtFixedRate(pushDataRunnable, publishInterval,
		                                             publishInterval, TimeUnit.SECONDS);
	}


	@Override
	public void terminateConnection() {
		Runnable stopConnection = new Runnable() {
			public void run() {
				while (isConnected()) {
					serviceHandler.cancel(true);
					closeConnection();

					if (log.isDebugEnabled()) {
						log.warn("Unable to 'STOP' connection to XMPP server at: " + server);
					}

					try {
						Thread.sleep(timeoutInterval);
					} catch (InterruptedException e1) {
						log.error("XMPP-Terminator: Thread Sleep Interrupt Exception");
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
