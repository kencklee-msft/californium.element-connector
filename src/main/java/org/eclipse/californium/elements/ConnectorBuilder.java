package org.eclipse.californium.elements;

import java.net.InetSocketAddress;

import org.eclipse.californium.elements.tcp.client.TCPClientConnector;
import org.eclipse.californium.elements.tcp.server.TCPServerConnector;

public abstract class ConnectorBuilder {
	
	protected ConnectionSemantic connectionSematic;
	protected SecuritySemantic securitySemantic;
	protected CommunicationRole communicationRole;
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
		public StatefulConnector buildStatfulConnector() {
			if(communicationRole.equals(CommunicationRole.CLIENT)) {
				return new TCPClientConnector(address, port);
				
			} else if(communicationRole.equals(CommunicationRole.SERVER)) {
				return new TCPServerConnector(address, port);
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
		public StatefulConnector buildStatfulConnector() {
			throw new UnsupportedOperationException("Cannot Builder Statful Connector Using UDP");
		}
		
		@Override
		public Connector buildConnector() {
			return new UDPConnector(getSocketObj());
		}
	}
	
}
