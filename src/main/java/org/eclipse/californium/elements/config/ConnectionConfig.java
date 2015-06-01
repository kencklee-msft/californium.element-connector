package org.eclipse.californium.elements.config;

public abstract class ConnectionConfig {
	
	public enum LayerSemantic {
		TCP,
		UDP
	}
	
	public enum ConnectionSemantic {
		NIO,
		OIO,
		LOCAL
	}
	
	public enum SecuritySemantic {
		TLS,
		DTLS
	}
	
	public enum CommunicationRole {
		NODE,
		CLIENT,
		SERVER;
	}

	private String remoteAddress;
	private int remotePort;
	
	public abstract LayerSemantic getTransportLayer();
	public abstract CommunicationRole getCommunicationRole();
	
	
	public String getRemoteAddress() {
		return remoteAddress;
	}
	
	public void setRemoteAddress(final String remoteAddress) {
		this.remoteAddress = remoteAddress;
	}
	
	public int getRemotePort() {
		return remotePort;
	}
	
	public void setRemotePort(final int remotePort) {
		this.remotePort = remotePort;
	}
}
