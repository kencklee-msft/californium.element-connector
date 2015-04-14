package org.eclipse.californium.elements;

import java.io.IOException;

public interface StatefulConnector extends Connector{
	
	public enum ConnectionState {
		CONNECTING,
		CONNECTED,
		DISCONNECTING,
		DISCONNECTED;
	}
	
	public void start(boolean wait) throws IOException;
	
	public ConnectionState getConnectionState();

}
