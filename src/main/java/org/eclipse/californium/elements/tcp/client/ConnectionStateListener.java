package org.eclipse.californium.elements.tcp.client;

import org.eclipse.californium.elements.StatefulConnector.ConnectionState;


public interface ConnectionStateListener {
	
	public void stateChange(ConnectionState state);

}
