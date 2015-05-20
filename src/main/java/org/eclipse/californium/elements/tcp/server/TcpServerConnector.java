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
import java.util.logging.Logger;

import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.RawDataChannel;
import org.eclipse.californium.elements.StatefulConnector;
import org.eclipse.californium.elements.tcp.ConnectionInfo;
import org.eclipse.californium.elements.tcp.ConnectionInfo.ConnectionState;
import org.eclipse.californium.elements.tcp.ConnectionStateListener;
import org.eclipse.californium.elements.tcp.MessageInboundTransponder;

public class TcpServerConnector implements StatefulConnector, RemoteConnectionListener {
	
	private static final Logger LOG = Logger.getLogger( TcpServerConnector.class.getName() );

	
	private final InetSocketAddress address;	
	private final MessageInboundTransponder transponder;
	private final TcpServerConnectionMgr connMgr;

	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private ChannelFuture communicationChannel;
	private ConnectionState state = ConnectionState.DISCONNECTED;


	private ConnectionStateListener csl;
	
	public TcpServerConnector(final String address, final int port, final ConnectionStateListener csl) {
		this(new InetSocketAddress(address, port), csl);
	}

	public TcpServerConnector(final InetSocketAddress address, final ConnectionStateListener csl) {
		this.address = address;
		transponder = new MessageInboundTransponder();
		connMgr = new TcpServerConnectionMgr(this);
		this.csl = csl;
	}
	
	@Override
	public void start(final boolean wait) throws IOException {
		LOG.info("Staring TCP SERVER connector with Xconn");
		bossGroup = new NioEventLoopGroup(50);
		workerGroup = new NioEventLoopGroup(50);
		final TcpServerChannelInitializer init = new TcpServerChannelInitializer(transponder, connMgr);
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
				
		if(wait) {
			try {
				communicationChannel.sync();
			} catch (final InterruptedException e) {
				System.err.println("Waiting for connection was interupted");
			}
		}
	}

	@Override
	public void start() throws IOException {
		start(false);
	}

	@Override
	public void stop() {
		communicationChannel.channel().close();
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
		incomingConnectionStateChange(new ConnectionInfo(ConnectionState.DISCONNECTED, getAddress()));
	}

	@Override
	public void destroy() {
		communicationChannel = null;
		bossGroup = null;
		workerGroup = null;
	}

	@Override
	public void send(final RawData msg) {
		final Channel ch = connMgr.getChannel(msg.getInetSocketAddress());
		if(ch != null) {
			System.out.println("Sending " + msg.getSize() + " byte" + " to " + ch.remoteAddress().toString());
			
			ch.writeAndFlush(msg.getBytes()).addListener(new ChannelFutureListener() {
				
				@Override
				public void operationComplete(final ChannelFuture future) throws Exception {
					printOperationState(future);
				}
			});
		}
		else {
			System.out.println("No Channel available to message.  Unknown Connection " + msg.getAddress().toString());
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
		System.out.println(sb.toString());
	}


	@Override
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
