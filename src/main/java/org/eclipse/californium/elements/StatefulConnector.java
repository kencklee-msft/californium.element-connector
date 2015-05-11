package org.eclipse.californium.elements;

import java.io.IOException;

import org.eclipse.californium.elements.tcp.ConnectionInfo.ConnectionState;
import org.eclipse.californium.elements.tcp.ConnectionStateListener;

public interface StatefulConnector extends Connector{
	
	public void start(boolean wait) throws IOException;
	
	public ConnectionState getConnectionState();
	
	public void addConnectionStateListener(ConnectionStateListener listener);

}
