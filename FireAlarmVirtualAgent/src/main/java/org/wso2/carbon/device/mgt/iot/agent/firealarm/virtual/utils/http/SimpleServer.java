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

package org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.utils.http;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.core.AgentConstants;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.core.AgentManager;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.exception.AgentCoreOperationException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

public class SimpleServer {

	private static final Log log = LogFactory.getLog(SimpleServer.class);
	private Server server;

	public SimpleServer() throws AgentCoreOperationException {
		new SimpleServer(AgentConstants.DEVICE_SERVER_PORT);
	}

	public SimpleServer(int serverPort) throws AgentCoreOperationException {
		this.server = new Server(serverPort);
		try {
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
			server.start();
			log.info(AgentConstants.LOG_APPENDER + "HTTP Server started at port: " + serverPort);
		} catch (Exception e) {
			String errorMsg = "Unable to start HTTP server at port: " + serverPort;
			log.error(AgentConstants.LOG_APPENDER + errorMsg);
			throw new AgentCoreOperationException(errorMsg, e);
		}
	}

	public Server getServer() {
		return server;
	}
}

