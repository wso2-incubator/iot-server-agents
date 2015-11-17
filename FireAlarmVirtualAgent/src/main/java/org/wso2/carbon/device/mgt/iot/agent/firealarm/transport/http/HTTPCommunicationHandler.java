package org.wso2.carbon.device.mgt.iot.agent.firealarm.transport.http;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Server;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.transport.CommunicationHandler;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.transport.CommunicationHandlerException;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.transport.CommunicationUtils;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.core.AgentConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public abstract class HTTPCommunicationHandler implements CommunicationHandler {
	private static final Log log = LogFactory.getLog(HTTPCommunicationHandler.class);

	protected Server server;
	protected int port;
	protected int timeoutInterval;

	protected HTTPCommunicationHandler() {
		this.port = CommunicationUtils.getAvailablePort(10);
		this.server = new Server(port);
		timeoutInterval = DEFAULT_TIMEOUT_INTERVAL;
	}

	protected HTTPCommunicationHandler(int port) {
		this.port = port;
		this.server = new Server(this.port);
		timeoutInterval = DEFAULT_TIMEOUT_INTERVAL;
	}

	protected HTTPCommunicationHandler(int port, int timeoutInterval) {
		this.port = port;
		this.server = new Server(this.port);
		this.timeoutInterval = timeoutInterval;
	}

	public void setTimeoutInterval(int timeoutInterval) {
		this.timeoutInterval = timeoutInterval;
	}

	/**
	 * Checks whether the HTTP server is up and listening for incoming requests.
	 *
	 * @return true if the server is up & listening for requests, else false.
	 */
	public boolean isConnected() {
		return server.isStarted();
	}


	protected void incrementPort() {
		this.port = this.port + 1;
		server = new Server(port);
	}

	/**
	 * Shuts-down the HTTP Server.
	 */
	public void closeConnection() throws Exception {
		if (server != null && isConnected()) {
			server.stop();
		}
	}



}
