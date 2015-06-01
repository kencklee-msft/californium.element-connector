package org.eclipse.californium.elements;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;

import org.eclipse.californium.elements.config.ConnectionConfig;
import org.eclipse.californium.elements.config.ConnectionConfig.CommunicationRole;
import org.eclipse.californium.elements.config.TCPConnectionConfig;
import org.eclipse.californium.elements.config.UDPConnectionConfig;
import org.eclipse.californium.elements.tcp.client.TcpClientConnector;
import org.eclipse.californium.elements.tcp.server.TcpServerConnector;
import org.eclipse.californium.elements.tcp.server.ThreadSafeTcpServerConnector;

public abstract class ConnectorBuilder {
	
	protected int nSelectorThread;
	protected Executor selectorExecutor;
	protected Executor callBackExecutor;
	
	public static ConnectorBuilder createTransportLayerBuilder(final ConnectionConfig cfg){
		switch(cfg.getTransportLayer()) {
		case TCP:
			return  new TCPConnectorBuilder((TCPConnectionConfig)cfg);
		case UDP:
			return new UDPConnectorBuilder((UDPConnectionConfig)cfg);
		default:
			throw new UnsupportedOperationException("Not a Supported communication layer semantic");
		}
	}
	
	public abstract Connector buildConnector();
	public abstract StatefulConnector buildStatefulConnector();
	
	protected InetSocketAddress getSocketObj(final String address, final int port) {
		if(address == null) {
			return new InetSocketAddress(port);
		}
		else {
			return new InetSocketAddress(address, port);
		}
	}
		
	private static class TCPConnectorBuilder extends ConnectorBuilder {
		
		private final TCPConnectionConfig cfg;

		public TCPConnectorBuilder(final TCPConnectionConfig cfg) {
			this.cfg = cfg;
		}
				
		@Override
		public StatefulConnector buildStatefulConnector() {
			if(cfg.getCommunicationRole().equals(CommunicationRole.CLIENT)) {
				return new TcpClientConnector(cfg);
				
			} else if(cfg.getCommunicationRole().equals(CommunicationRole.SERVER)) {
				if(cfg.isSharable()) {
					return new ThreadSafeTcpServerConnector(cfg);
				} else {
					return new TcpServerConnector(cfg);
				}
			}
			else {
				throw new UnsupportedOperationException("No Valide Communication role was specified");
			}
		}

		@Override
		public Connector buildConnector() {
			return buildStatefulConnector();
		}
	}
	
	private static class UDPConnectorBuilder extends ConnectorBuilder {
		private final UDPConnectionConfig cfg;
		
		public UDPConnectorBuilder(final UDPConnectionConfig cfg) {
			this.cfg = cfg;
		}
		
		@Override
		public Connector buildConnector() {
			return new UDPConnector(getSocketObj(cfg.getRemoteAddress(), cfg.getRemotePort()));
		}

		@Override
		public StatefulConnector buildStatefulConnector() {
			throw new UnsupportedOperationException("Cannot build a connector that track state for UDP");
		}
	}
	
}
