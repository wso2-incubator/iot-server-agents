/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.device.mgt.iot.agent.firealarm.transport.http;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Server;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.transport.TransportHandler;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.transport.TransportUtils;

public abstract class HTTPTransportHandler implements TransportHandler {
	private static final Log log = LogFactory.getLog(HTTPTransportHandler.class);

	protected Server server;
	protected int port;
	protected int timeoutInterval;

	protected HTTPTransportHandler() {
		this.port = TransportUtils.getAvailablePort(10);
		this.server = new Server(port);
		timeoutInterval = DEFAULT_TIMEOUT_INTERVAL;
	}

	protected HTTPTransportHandler(int port) {
		this.port = port;
		this.server = new Server(this.port);
		timeoutInterval = DEFAULT_TIMEOUT_INTERVAL;
	}

	protected HTTPTransportHandler(int port, int timeoutInterval) {
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
