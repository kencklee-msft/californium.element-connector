package org.eclipse.californium.elements;

import java.net.InetSocketAddress;

import org.eclipse.californium.elements.tcp.ConnectionStateListener;
import org.eclipse.californium.elements.tcp.client.TcpClientConnector;
import org.eclipse.californium.elements.tcp.server.TcpServerConnector;
import org.eclipse.californium.elements.tcp.server.ThreadSafeTcpServerConnector;

public abstract class ConnectorBuilder {
	
	protected ConnectionSemantic connectionSematic;
	protected SecuritySemantic securitySemantic;
	protected CommunicationRole communicationRole;
	protected ConnectionStateListener listener;
	protected boolean isThreadSafe = false;
	protected String address;
	protected int port = 0;
		
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
		
	public static ConnectorBuilder createTransportLayerBuilder(final LayerSemantic layerstrat){
		switch(layerstrat) {
		case TCP:
			return  new TCPConnectorBuilder();
		case UDP:
			return new UDPConnectorBuilder();
		default:
			throw new UnsupportedOperationException("Not Supported communication layer strategy");
		}
	}
	
	public abstract ConnectorBuilder setConnectionSemantics(final ConnectionSemantic connSemantic);
	public abstract ConnectorBuilder setSecuritySemantic(final SecuritySemantic secSemantic);
	public abstract ConnectorBuilder setCommunicationRole(CommunicationRole commRole);
	public abstract ConnectorBuilder setConnectionStateListener(ConnectionStateListener listener);
	public abstract StatefulConnector buildStatfulConnector();
	public abstract Connector buildConnector();
	
	public ConnectorBuilder setAddress(final String address) {
		this.address = address;
		return this;
	}
	
	public ConnectorBuilder setPort(final int port) {
		this.port = port;
		return this;
	}
	
	public ConnectorBuilder addThreadSafty() {
		this.isThreadSafe = true;
		return this;
	}
	
	protected InetSocketAddress getSocketObj() {
		if(address == null) {
			return new InetSocketAddress(port);
		}
		else {
			return new InetSocketAddress(address, port);
		}
	}
	
	private static class TCPConnectorBuilder extends ConnectorBuilder {
		private final LayerSemantic layerSemantic = LayerSemantic.TCP;
		
		public TCPConnectorBuilder() {}
		
		@Override
		public ConnectorBuilder setConnectionSemantics(final ConnectionSemantic connSemantic) {
			this.connectionSematic = connSemantic;
			return this;
		}
		
		@Override
		public ConnectorBuilder setSecuritySemantic(final SecuritySemantic secSemantic) {
			if(secSemantic.equals(SecuritySemantic.DTLS)) {
				throw new UnsupportedOperationException("TCP cannot be secured with DTLS");
			}
			
			this.securitySemantic = secSemantic;
			return this;
		}
		
		@Override
		public ConnectorBuilder setCommunicationRole(final CommunicationRole commRole) {
			if(commRole.equals(CommunicationRole.NODE)) {
				throw new UnsupportedOperationException("TCP cannot be set as Node");
			}
			this.communicationRole = commRole;
			return this;
		}
		
		@Override
		public ConnectorBuilder setConnectionStateListener(final ConnectionStateListener listener) {
			this.listener = listener;
			return this;
		}
				
		@Override
		public StatefulConnector buildStatfulConnector() {
			if(communicationRole.equals(CommunicationRole.CLIENT)) {
				return new TcpClientConnector(address, port, listener);
				
			} else if(communicationRole.equals(CommunicationRole.SERVER)) {
				if(isThreadSafe) {
					return new ThreadSafeTcpServerConnector(address, port, listener);
				} else {
					return new TcpServerConnector(address, port, listener);
				}
			}
			else {
				throw new UnsupportedOperationException("No Valide Communication role was specified");
			}
		}
		
		@Override
		public Connector buildConnector() {
			return this.buildStatfulConnector();
		}
	}
	
	private static class UDPConnectorBuilder extends ConnectorBuilder {
		private final LayerSemantic layerSemantic = LayerSemantic.UDP;
		private final CommunicationRole commRole = CommunicationRole.NODE;
		
		public UDPConnectorBuilder() {}
		
		@Override
		public ConnectorBuilder setConnectionSemantics(final ConnectionSemantic connSemantic) {
			this.connectionSematic = connSemantic;
			return this;
		}
		
		@Override
		public ConnectorBuilder setSecuritySemantic(final SecuritySemantic secSemantic) {
			if(secSemantic.equals(SecuritySemantic.TLS)) {
				throw new UnsupportedOperationException("UDP cannot be secured with TLS");
			}
			
			this.securitySemantic = secSemantic;
			return this;
		}
		
		@Override
		public ConnectorBuilder setCommunicationRole(final CommunicationRole commRole) {
			if(commRole.equals(CommunicationRole.NODE)) {
				this.communicationRole = commRole;
				return this;
			}
			
			throw new UnsupportedOperationException("UDP cannot be set as " + commRole.toString());			
		}
		
		@Override
		public ConnectorBuilder addThreadSafty() {
			throw new UnsupportedOperationException("Thread safe operation not supported for UDP");
		}
		
		@Override
		public ConnectorBuilder setConnectionStateListener(final ConnectionStateListener listener) {
			throw new UnsupportedOperationException("Cannot set a State Listener on a UDP Connection");
		}
		
		@Override
		public StatefulConnector buildStatfulConnector() {
			throw new UnsupportedOperationException("Cannot Builder Statful Connector Using UDP");
		}
		
		@Override
		public Connector buildConnector() {
			return new UDPConnector(getSocketObj());
		}
	}
	
}
