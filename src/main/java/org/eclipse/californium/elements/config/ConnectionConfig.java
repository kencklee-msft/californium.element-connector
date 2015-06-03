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
	
	public abstract LayerSemantic getTransportLayer();
	public abstract CommunicationRole getCommunicationRole();
	
	
	public abstract String getRemoteAddress();
	public abstract int getRemotePort();
}
