package org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.communication.http;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.communication
		.CommunicationHandlerException;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.communication.CommunicationUtils;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.core.AgentConstants;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.core.AgentManager;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.exception.AgentCoreOperationException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HTTPCommunicationHandlerImpl extends HTTPCommunicationHandler {
	private static final Log log = LogFactory.getLog(HTTPCommunicationHandlerImpl.class);

	private static final AgentManager agentManager = AgentManager.getInstance();
	private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

	private int dataPublishInterval;

	public HTTPCommunicationHandlerImpl() {
		super();
		dataPublishInterval = AgentConstants.DEFAULT_DATA_PUBLISH_INTERVAL;
	}

	public HTTPCommunicationHandlerImpl(int port) {
		super(port);
		dataPublishInterval = AgentConstants.DEFAULT_DATA_PUBLISH_INTERVAL;
	}

	public HTTPCommunicationHandlerImpl(int port, int reconnectionInterval) {
		super(port, reconnectionInterval);
		dataPublishInterval = AgentConstants.DEFAULT_DATA_PUBLISH_INTERVAL;
	}

	public void setDataPublishInterval(int dataPublishInterval) {
		this.dataPublishInterval = dataPublishInterval;
	}


	public void initializeConnection() {
		processIncomingMessage();

		try {
			server.start();
			log.info("HTTP Server started at port: " + port);
		} catch (Exception ex) {
			log.warn("Unable to start HTTP server at port: " + port);

			Runnable reconnect = new Runnable() {
				public void run() {
					attemptReconnection();
				}
			};
			Thread reconnectThread = new Thread(reconnect);
			reconnectThread.setDaemon(true);
			reconnectThread.start();
		}

		registerThisDevice();
		publishDeviceData();
	}

	@Override
	public void attemptReconnection() {
		while (!isConnected()) {
			incrementPort();
			try {
				server.start();
			} catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.warn("Unable to 'START' HTTP server at port: " + port);
				}

				try {
					Thread.sleep(timeoutInterval);
				} catch (InterruptedException e1) {
					log.error("HTTP-Connector: Thread Sleep Interrupt Exception");
				}
			}
		}
	}


	@Override
	public void processIncomingMessage() {
		server.setHandler(new AbstractHandler() {
			public void handle(String s, Request request, HttpServletRequest
					httpServletRequest,
			                   HttpServletResponse httpServletResponse)
					throws IOException, ServletException {
				httpServletResponse.setContentType("text/html;charset=utf-8");
				httpServletResponse.setStatus(HttpServletResponse.SC_OK);
				request.setHandled(true);

				AgentManager agentManager = AgentManager.getInstance();
				String pathContext = request.getPathInfo();
				String separator = File.separator;

				if (pathContext.toUpperCase().contains(
						separator + AgentConstants.TEMPERATURE_CONTROL)) {
					httpServletResponse.getWriter().println(
							agentManager.getTemperature());

				} else if (pathContext.toUpperCase().contains(
						separator + AgentConstants.HUMIDITY_CONTROL)) {
					httpServletResponse.getWriter().println(
							agentManager.getHumidity());

				} else if (pathContext.toUpperCase().contains(
						separator + AgentConstants.BULB_CONTROL)) {
					String[] pathVariables = pathContext.split(separator);

					if (pathVariables.length != 3) {
						httpServletResponse.getWriter().println(
								"Invalid BULB-control received by the device. Need to be in " +
										"'/BULB/<ON|OFF>' format.");
						return;
					}

					String switchState = pathVariables[2];

					if (switchState == null) {
						httpServletResponse.getWriter().println(
								"Please specify switch-status of the BULB.");
					} else {
						boolean status = switchState.toUpperCase().equals(
								AgentConstants.CONTROL_ON);
						agentManager.changeBulbStatus(status);
						httpServletResponse.getWriter().println("Bulb is " + (status ?
								AgentConstants.CONTROL_ON : AgentConstants.CONTROL_OFF));
					}
				} else {
					httpServletResponse.getWriter().println(
							"Invalid control command received by the device.");
				}
			}
		});
	}

	@Override
	public void publishDeviceData() {
		Runnable pushDataRunnable = new Runnable() {
			@Override
			public void run() {
				int responseCode = -1;
				String deviceOwner = agentManager.getAgentConfigs().getDeviceOwner();
				String deviceID = agentManager.getAgentConfigs().getDeviceId();
				String pushDataEndPointURL = agentManager.getPushDataAPIEP();
				String pushDataPayload = null;
				HttpURLConnection httpConnection = null;

				try {
					httpConnection = getHttpConnection(agentManager.getPushDataAPIEP());
					httpConnection.setRequestMethod(AgentConstants.HTTP_POST);
					httpConnection.setRequestProperty("Authorization", "Bearer " +
							agentManager.getAgentConfigs().getAuthToken());
					httpConnection.setRequestProperty("Content-Type",
					                                  AgentConstants.APPLICATION_JSON_TYPE);

					int currentTemperature = agentManager.getTemperature();
					pushDataPayload = String.format(AgentConstants.PUSH_DATA_PAYLOAD, deviceOwner,
					                                deviceID,
					                                agentManager.getDeviceIP(),
					                                currentTemperature);

					if (log.isDebugEnabled()) {
						log.debug(AgentConstants.LOG_APPENDER + "Push Data Payload is: " +
								          pushDataPayload);
					}

					httpConnection.setDoOutput(true);
					DataOutputStream dataOutPutWriter = new DataOutputStream(
							httpConnection.getOutputStream());
					dataOutPutWriter.writeBytes(pushDataPayload);
					dataOutPutWriter.flush();
					dataOutPutWriter.close();

					responseCode = httpConnection.getResponseCode();
					httpConnection.disconnect();

				} catch (ProtocolException exception) {
					String errorMsg = AgentConstants.LOG_APPENDER +
							"Protocol specific error occurred when trying to set method to " +
							AgentConstants.HTTP_POST + " for:" + pushDataEndPointURL;
					log.error(errorMsg);

				} catch (IOException exception) {
					String errorMsg = AgentConstants.LOG_APPENDER +
							"An IO error occurred whilst trying to get the response code from: " +
							pushDataEndPointURL + " for a " + AgentConstants.HTTP_POST + " " +
							"method.";
					log.error(errorMsg);

				} catch (CommunicationHandlerException exception) {
					log.error(AgentConstants.LOG_APPENDER +
							          "Error encountered whilst trying to create HTTP-Connection" +
							          " to IoT-Server EP at: " + pushDataEndPointURL);
				}

				if (responseCode == HttpStatus.CONFLICT_409 ||
						responseCode == HttpStatus.PRECONDITION_FAILED_412) {
					log.warn(AgentConstants.LOG_APPENDER +
							         "DeviceIP is being Re-Registered due to Push-Data failure " +
							         "with response code: " + responseCode);
					registerThisDevice();

				} else if (responseCode != HttpStatus.NO_CONTENT_204) {
					if (log.isDebugEnabled()) {
						log.error(AgentConstants.LOG_APPENDER + "Status Code: " + responseCode +
								          " encountered whilst trying to Push-Device-Data to IoT" +
								          " Server at: " +
								          agentManager.getPushDataAPIEP());
					}
					agentManager.updateAgentStatus(AgentConstants.SERVER_NOT_RESPONDING);
				}

				if (log.isDebugEnabled()) {
					log.debug(AgentConstants.LOG_APPENDER + "Push-Data call with payload - " +
							          pushDataPayload + ", to IoT Server returned status " +
							          responseCode);
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
					} catch (Exception e) {
						if (log.isDebugEnabled()) {
							log.warn("Unable to 'STOP' HTTP server at port: " + port);
						}

						try {
							Thread.sleep(timeoutInterval);
						} catch (InterruptedException e1) {
							log.error("HTTP-Termination: Thread Sleep Interrupt Exception");
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
	public void processIncomingMessage(Object message) {

	}


	public void registerThisDevice() {
		agentManager.updateAgentStatus("Registering...");

		final Runnable ipRegistration = new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						int responseCode = registerDeviceIP(
								agentManager.getAgentConfigs().getDeviceOwner(),
								agentManager.getAgentConfigs().getDeviceId());

						if (responseCode == HttpStatus.OK_200) {
							agentManager.updateAgentStatus(AgentConstants.REGISTERED);
							break;
						} else {
							log.error(AgentConstants.LOG_APPENDER +
									          "Device Registration with IoT Server at:" + " " +
									          agentManager.getIpRegistrationEP() +
									          " failed with response - '" + responseCode + ":" +
									          HttpStatus.getMessage(responseCode) + "'");
							agentManager.updateAgentStatus(AgentConstants.RETRYING_TO_REGISTER);
						}
					} catch (AgentCoreOperationException exception) {
						log.error(AgentConstants.LOG_APPENDER +
								          "Error encountered whilst trying to register the " +
								          "Device's " +
								          "IP at: " + agentManager.getIpRegistrationEP() +
								          ".\nCheck whether the network-interface provided is " +
								          "accurate");
						agentManager.updateAgentStatus(AgentConstants.REGISTRATION_FAILED);
					}

					try {
						Thread.sleep(timeoutInterval);
					} catch (InterruptedException e1) {
						log.error("Device Registration: Thread Sleep Interrupt Exception");
					}
				}
			}
		};

		Thread ipRegisterThread = new Thread(ipRegistration);
		ipRegisterThread.setDaemon(true);
		ipRegisterThread.start();
	}


	/**
	 * This method calls the "Register-API" of the IoT Server in order to register the device's IP
	 * against its ID.
	 *
	 * @param deviceOwner the owner of the device by whose name the agent was downloaded.
	 *                    (Read from configuration file)
	 * @param deviceID    the deviceId that is auto-generated whilst downloading the agent.
	 *                    (Read from configuration file)
	 * @return the status code of the HTTP-Post call to the Register-API of the IoT-Server
	 * @throws AgentCoreOperationException if any errors occur when an HTTPConnection session is
	 *                                     created
	 */
	private int registerDeviceIP(String deviceOwner, String deviceID)
			throws AgentCoreOperationException {
		int responseCode = -1;

		String networkInterface = agentManager.getNetworkInterface();
		String deviceIPAddress = getDeviceIP(networkInterface);

		if (deviceIPAddress == null) {
			throw new AgentCoreOperationException(
					"An IP address could not be retrieved for the selected network interface - '" +
							networkInterface + ".");
		}

		agentManager.setDeviceIP(deviceIPAddress);
		log.info(AgentConstants.LOG_APPENDER + "Device IP Address: " + deviceIPAddress);

		String deviceIPRegistrationEP = agentManager.getIpRegistrationEP();
		String registerEndpointURLString =
				deviceIPRegistrationEP + File.separator + deviceOwner + File.separator + deviceID +
						File.separator + deviceIPAddress;

		if (log.isDebugEnabled()) {
			log.debug(AgentConstants.LOG_APPENDER + "DeviceIP Registration EndPoint: " +
					          registerEndpointURLString);
		}

		HttpURLConnection httpConnection;
		try {
			httpConnection = getHttpConnection(registerEndpointURLString);
		} catch (CommunicationHandlerException e) {
			String errorMsg = AgentConstants.LOG_APPENDER +
					"Protocol specific error occurred when trying to fetch an HTTPConnection to:" +
					" " + registerEndpointURLString;
			log.error(errorMsg);
			throw new AgentCoreOperationException();
		}

		try {
			httpConnection.setRequestMethod(AgentConstants.HTTP_POST);
			httpConnection.setRequestProperty("Authorization", "Bearer " +
					agentManager.getAgentConfigs().getAuthToken());
			httpConnection.setDoOutput(true);
			responseCode = httpConnection.getResponseCode();

		} catch (ProtocolException exception) {
			String errorMsg = AgentConstants.LOG_APPENDER +
					"Protocol specific error occurred when trying to set method to " +
					AgentConstants.HTTP_POST + " for:" + registerEndpointURLString;
			log.error(errorMsg);
			throw new AgentCoreOperationException(errorMsg, exception);

		} catch (IOException exception) {
			String errorMsg = AgentConstants.LOG_APPENDER +
					"An IO error occurred whilst trying to get the response code from: " +
					registerEndpointURLString + " for a " + AgentConstants.HTTP_POST + " method.";
			log.error(errorMsg);
			throw new AgentCoreOperationException(errorMsg, exception);
		}

		log.info(AgentConstants.LOG_APPENDER + "DeviceIP - " + deviceIPAddress +
				         ", registration with IoT Server at : " +
				         agentManager.getAgentConfigs().getHTTPS_ServerEndpoint() +
				         " returned status " +
				         responseCode);

		return responseCode;
	}

	/*------------------------------------------------------------------------------------------*/
	/* 		Utility methods relevant to creating and sending HTTP requests to the Iot-Server 	*/
	/*------------------------------------------------------------------------------------------*/

	/**
	 * This method is used to get the IP of the device in which the agent is run on.
	 *
	 * @return the IP Address of the device
	 * @throws AgentCoreOperationException if any errors occur whilst trying to get the IP address
	 */
	private String getDeviceIP() throws AgentCoreOperationException {
		try {
			return Inet4Address.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			String errorMsg = AgentConstants.LOG_APPENDER +
					"Error encountered whilst trying to get the device IP address.";
			log.error(errorMsg);
			throw new AgentCoreOperationException(errorMsg, e);
		}
	}

	/**
	 * This is an overloaded method that fetches the public IPv4 address of the given network
	 * interface
	 *
	 * @param networkInterfaceName the network-interface of whose IPv4 address is to be retrieved
	 * @return the IP Address iof the device
	 * @throws AgentCoreOperationException if any errors occur whilst trying to get details of the
	 *                                     given network interface
	 */
	private String getDeviceIP(String networkInterfaceName) throws
	                                                        AgentCoreOperationException {
		String ipAddress = null;
		try {
			Enumeration<InetAddress> interfaceIPAddresses = NetworkInterface.getByName(
					networkInterfaceName).getInetAddresses();
			for (; interfaceIPAddresses.hasMoreElements(); ) {
				InetAddress ip = interfaceIPAddresses.nextElement();
				ipAddress = ip.getHostAddress();
				if (log.isDebugEnabled()) {
					log.debug(AgentConstants.LOG_APPENDER + "IP Address: " + ipAddress);
				}

				if (CommunicationUtils.validateIPv4(ipAddress)) {
					return ipAddress;
				}
			}
		} catch (SocketException | NullPointerException exception) {
			String errorMsg = AgentConstants.LOG_APPENDER +
					"Error encountered whilst trying to get IP Addresses of the network " +
					"interface: " + networkInterfaceName +
					".\nPlease check whether the name of the network interface used is correct";
			log.error(errorMsg);
			throw new AgentCoreOperationException(errorMsg, exception);
		}

		return ipAddress;
	}


	private Map<String, String> getDeviceIPList() throws AgentCoreOperationException {

		Map<String, String> ipAddressList = new HashMap<String, String>();
		Enumeration<NetworkInterface> networkInterfaces;
		String networkInterfaceName = "";
		String ipAddress;

		try {
			networkInterfaces = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException exception) {
			String errorMsg = AgentConstants.LOG_APPENDER +
					"Error encountered whilst trying to get the list of network-interfaces of " +
					"the" +
					" " +
					"device.";
			log.error(errorMsg);
			throw new AgentCoreOperationException(errorMsg, exception);
		}

		try {
			for (; networkInterfaces.hasMoreElements(); ) {
				networkInterfaceName = networkInterfaces.nextElement().getName();

				if (log.isDebugEnabled()) {
					log.debug(AgentConstants.LOG_APPENDER + "Network Interface: " +
							          networkInterfaceName);
					log.debug(AgentConstants.LOG_APPENDER +
							          "------------------------------------------");
				}

				Enumeration<InetAddress> interfaceIPAddresses = NetworkInterface.getByName(
						networkInterfaceName).getInetAddresses();

				for (; interfaceIPAddresses.hasMoreElements(); ) {
					ipAddress = interfaceIPAddresses.nextElement().getHostAddress();

					if (log.isDebugEnabled()) {
						log.debug(AgentConstants.LOG_APPENDER + "IP Address: " + ipAddress);
					}

					if (CommunicationUtils.validateIPv4(ipAddress)) {
						ipAddressList.put(networkInterfaceName, ipAddress);
					}
				}

				if (log.isDebugEnabled()) {
					log.debug(AgentConstants.LOG_APPENDER +
							          "------------------------------------------");
				}
			}
		} catch (SocketException exception) {
			String errorMsg = AgentConstants.LOG_APPENDER +
					"Error encountered whilst trying to get the IP Addresses of the network " +
					"interface: " + networkInterfaceName;
			log.error(errorMsg);
			throw new AgentCoreOperationException(errorMsg, exception);
		}

		return ipAddressList;
	}

}
