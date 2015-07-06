package org.eclipse.californium.elements;

import org.eclipse.californium.elements.tcp.ConnectionInfo.ConnectionState;
import org.eclipse.californium.elements.tcp.ConnectionStateListener;

public interface StatefulConnector extends Connector{

	public ConnectionState getConnectionState();

	public void addConnectionStateListener(ConnectionStateListener listener);
}
