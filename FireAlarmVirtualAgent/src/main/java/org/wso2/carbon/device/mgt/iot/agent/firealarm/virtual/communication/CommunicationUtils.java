package org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.communication;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.core.AgentConstants;
import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.exception.AgentCoreOperationException;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class CommunicationUtils {
	private static final Log log = LogFactory.getLog(CommunicationUtils.class);


	/**
	 * Given a server endpoint as a String, this method splits it into Protocol, Host and Port
	 *
	 * @param ipString a network endpoint in the format - '<PROTOCOL>://<HOST>:<PORT>'
	 * @return a map with keys "Protocol", "Host" & "Port" for the related values from the ipString
	 * @throws AgentCoreOperationException
	 */
	public static Map<String, String> getHostAndPort(String ipString)
			throws AgentCoreOperationException {
		Map<String, String> ipPortMap = new HashMap<String, String>();
		String[] ipPortArray = ipString.split(":");

		if (ipPortArray.length != 3) {
			String errorMsg =
					"The IP String - '" + ipString +
							"' is invalid. It needs to be in format '<PROTOCOL>://<HOST>:<PORT>'.";
			log.info(AgentConstants.LOG_APPENDER + errorMsg);
			throw new AgentCoreOperationException(errorMsg);
		}

		ipPortMap.put("Protocol", ipPortArray[0]);
		ipPortMap.put("Host", ipPortArray[1].replace(File.separator, ""));
		ipPortMap.put("Port", ipPortArray[2]);
		return ipPortMap;
	}


	/**
	 * This method validates whether a specific IP Address is of IPv4 type
	 *
	 * @param ipAddress the IP Address which needs to be validated
	 * @return true if it is of IPv4 type and false otherwise
	 */
	public static boolean validateIPv4(String ipAddress) {
		try {
			if (ipAddress == null || ipAddress.isEmpty()) {
				return false;
			}

			String[] parts = ipAddress.split("\\.");
			if (parts.length != 4) {
				return false;
			}

			for (String s : parts) {
				int i = Integer.parseInt(s);
				if ((i < 0) || (i > 255)) {
					return false;
				}
			}
			return !ipAddress.endsWith(".");

		} catch (NumberFormatException nfe) {
			log.warn(
					AgentConstants.LOG_APPENDER + "The IP Address: " + ipAddress + " could not " +
							"be validated against IPv4-style");
			return false;
		}
	}

}
