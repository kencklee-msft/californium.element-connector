package org.eclipse.californium.elements.tcp.server;

import org.eclipse.californium.elements.tcp.ConnectionInfo;


public interface RemoteConnectionListener {

	public void incomingConnectionStateChange(ConnectionInfo info);

}
