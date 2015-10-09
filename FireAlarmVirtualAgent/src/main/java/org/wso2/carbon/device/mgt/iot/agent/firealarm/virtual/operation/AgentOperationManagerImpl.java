/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 */

package org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.operation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.core.AgentConstants;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.core.AgentManager;

public class AgentOperationManagerImpl implements AgentOperationManager {

	private static final Log log = LogFactory.getLog(AgentOperationManagerImpl.class);

	public void changeBulbStatus(boolean status) {
		log.info("Bulb status: " +
				         (status ? AgentConstants.CONTROL_ON : AgentConstants.CONTROL_OFF));
	}

	public double getTemperature() {
		double temp = Math.random() * 100;
		log.info("Temperature: " + temp);
		return temp;
	}

	public double getHumidity() {
		double hum = Math.random() * (80 - 10) + 10;
		log.info("Humidity: " + hum);
		return hum;
	}
}