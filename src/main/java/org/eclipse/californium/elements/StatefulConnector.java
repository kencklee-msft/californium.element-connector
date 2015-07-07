package org.eclipse.californium.elements;

import org.eclipse.californium.elements.tcp.ConnectionStateListener;

public interface StatefulConnector extends Connector{

	public void addConnectionStateListener(ConnectionStateListener listener);
}
