package org.eclipse.californium.elements.tcp.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.RawDataChannel;
import org.eclipse.californium.elements.StatefulConnector;
import org.eclipse.californium.elements.config.TCPConnectionConfig;
import org.eclipse.californium.elements.tcp.ConnectionInfo;
import org.eclipse.californium.elements.tcp.ConnectionInfo.ConnectionState;
import org.eclipse.californium.elements.tcp.ConnectionStateListener;
import org.eclipse.californium.elements.tcp.MessageInboundTransponder;
import org.eclipse.californium.elements.utils.FutureAggregate;

public class TcpServerConnector implements StatefulConnector, RemoteConnectionListener {
	
	private static final Logger LOG = Logger.getLogger( TcpServerConnector.class.getName() );
	
	private final InetSocketAddress address;	
	private final MessageInboundTransponder transponder;
	private final TcpServerConnectionMgr connMgr;
	private final TCPConnectionConfig cfg;

	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private ChannelFuture communicationChannel;
	private ConnectionState state = ConnectionState.DISCONNECTED;


	private ConnectionStateListener csl;

	public TcpServerConnector(final TCPConnectionConfig cfg) {
		this.cfg = cfg;
		address = new InetSocketAddress(cfg.getRemoteAddress(), cfg.getRemotePort());
		transponder = new MessageInboundTransponder(cfg.getCallBackExecutor() != null ? 
				cfg.getCallBackExecutor() : Executors.newCachedThreadPool());
		connMgr = new TcpServerConnectionMgr(this, cfg.getCallBackExecutor() != null ? 
				cfg.getCallBackExecutor() : Executors.newCachedThreadPool());
		this.csl = cfg.getListener();
	}
	
	@Override
	public Future<?> start() throws IOException {
		LOG.info("Staring TCP SERVER connector with Xconn");
		bossGroup = new NioEventLoopGroup(50);
		workerGroup = new NioEventLoopGroup(50);
		final TcpServerChannelInitializer init = new TcpServerChannelInitializer(transponder, connMgr, this);
		if(cfg.isSecured()) {
			init.addTLS(cfg.getSSlContext(), cfg.getSslClientCertificateRequestLevel(), cfg.getTLSVersions());
		}
		final ServerBootstrap bootsrap = new ServerBootstrap();
		bootsrap.group(bossGroup, workerGroup)
				.localAddress(address.getPort())
				.channel(NioServerSocketChannel.class)
				.childHandler(init)
				.option(ChannelOption.SO_BACKLOG, 128)
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				.childOption(ChannelOption.TCP_NODELAY, true);

		communicationChannel = bootsrap.bind();
		communicationChannel.addListener(new ChannelFutureListener() {
			
			@Override
			public void operationComplete(final ChannelFuture future) throws Exception {
				printOperationState(future);
				incomingConnectionStateChange(new ConnectionInfo(ConnectionState.CONNECTED, (InetSocketAddress)future.channel().remoteAddress()));
			}
		});
				
		return communicationChannel;
	}

	@Override
	public Future<?> stop() {
		@SuppressWarnings({ "rawtypes", "unchecked" })
		final FutureAggregate aggregateFuture = new FutureAggregate(communicationChannel.channel().closeFuture(), 
															  bossGroup.shutdownGracefully(),
															  workerGroup.shutdownGracefully());
		incomingConnectionStateChange(new ConnectionInfo(ConnectionState.DISCONNECTED, getAddress()));
		return aggregateFuture;
	}

	@Override
	public void destroy() {
		communicationChannel = null;
		bossGroup = null;
		workerGroup = null;
	}

	@Override
	public Future<?> send(final RawData msg) {
		final Channel ch = connMgr.getChannel(msg.getInetSocketAddress());
		if(ch != null) {
			LOG.finest("Sending " + msg.getSize() + " byte" + " to " + ch.remoteAddress().toString());
			return ch.writeAndFlush(msg.getBytes());
		}
		else {
			LOG.finest("No Channel available to message.  Unknown Connection " + msg.getAddress().toString());
			return null;
		}
	}

	@Override
	public void setRawDataReceiver(final RawDataChannel messageHandler) {
		transponder.setRawDataChannel(messageHandler);
	}

	@Override
	public InetSocketAddress getAddress() {
		return address;
	}
	
	private static void printOperationState(final ChannelFuture future) {
		final StringBuilder sb = new StringBuilder();
		sb.append("Operation Complete:");
		if(future.isDone()) {
			if(future.isSuccess()) {
				sb.append("Operation is Succes");
			}
			else if (!future.isSuccess() && !future.isCancelled()){
				sb.append("Operation Failed: ").append(future.cause());
			}
			else {
				sb.append("Operation was cancelled");
			}
		}
		else {
			sb.append("Operation Uncompletd");
		}
		LOG.finest(sb.toString());
	}

	public ConnectionState getConnectionState() {
		return state;
	}

	@Override
	public void incomingConnectionStateChange(final ConnectionInfo info) {
		state = info.getConnectionState();
		if(csl != null) {
			csl.stateChange(info);
		}
	}

	@Override
	public void addConnectionStateListener(final ConnectionStateListener listener) {
		this.csl = listener;
		
	}
}
