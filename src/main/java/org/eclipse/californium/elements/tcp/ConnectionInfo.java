package org.eclipse.californium.elements.tcp;

import java.net.InetSocketAddress;

public class ConnectionInfo {
	
	public enum ConnectionState {
		CONNECTING,
		CONNECTED,
		DISCONNECTING,
		DISCONNECTED,
		NEW_INCOMING_CONNECT,
		NEW_INCOMING_DISCONNECT,
		TLS_HANDSHAKE_STARTED,
		CONNECTED_SECURE,
		TLS_HANDSHAKE_FAILED;
	}

	private final InetSocketAddress remote;
	private final ConnectionState state;
	
	public ConnectionInfo(final ConnectionState state, final InetSocketAddress remote) {
		this.state = state;
		this.remote = remote;
	}
	
	public ConnectionState getConnectionState() {
		return state;
	}
	
	public InetSocketAddress getRemote() {
		return remote;
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("Connection: ")
		  .append(remote != null ? remote.toString():"NULL")
		  .append(" --> State: ")
		  .append(state != null ? state.toString():"NULL");
		return sb.toString();
	}

}
