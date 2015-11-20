package org.wso2.carbon.device.mgt.iot.agent.firealarm.transport;

public class TransportHandlerException extends Exception {
	private static final long serialVersionUID = 2736466230451105440L;

	private String errorMessage;

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public TransportHandlerException(String msg, Exception nestedEx) {
		super(msg, nestedEx);
		setErrorMessage(msg);
	}

	public TransportHandlerException(String message, Throwable cause) {
		super(message, cause);
		setErrorMessage(message);
	}

	public TransportHandlerException(String msg) {
		super(msg);
		setErrorMessage(msg);
	}

	public TransportHandlerException() {
		super();
	}

	public TransportHandlerException(Throwable cause) {
		super(cause);
	}
}
