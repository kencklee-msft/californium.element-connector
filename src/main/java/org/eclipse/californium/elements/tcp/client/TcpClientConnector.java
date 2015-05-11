package org.eclipse.californium.elements.tcp.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.RawDataChannel;
import org.eclipse.californium.elements.StatefulConnector;
import org.eclipse.californium.elements.tcp.ConnectionInfo;
import org.eclipse.californium.elements.tcp.MessageInboundTransponder;
import org.eclipse.californium.elements.tcp.ConnectionInfo.ConnectionState;
import org.eclipse.californium.elements.tcp.ConnectionStateListener;

public class TcpClientConnector implements StatefulConnector {
	
	private static final Logger LOG = Logger.getLogger( TcpClientConnector.class.getName() );
	
	private final MessageInboundTransponder transponder;
	private final String addr;
	private final int port;
	
	private InetSocketAddress netAddr;
	private NioEventLoopGroup workerPool;
	private ChannelFuture communicationChannel;
	private ConnectionState state = ConnectionState.DISCONNECTED;

	private ConnectionStateListener csl;
	
	public TcpClientConnector(final String addr, final int port) {
		this(addr, port, null);
	}
	
	public TcpClientConnector(final String addr, final int port, final ConnectionStateListener csl) {
		this.addr = addr;
		this.port = port;
		transponder = new MessageInboundTransponder(addr, port);
		this.csl = csl;
	}

	@Override
	public void start() throws IOException {
		start(false);
	}
	
	@Override
	public void start(final boolean wait) throws IOException {
		LOG.info("Staring TCP CLIENT connector");
		netAddr = new InetSocketAddress(addr, port);
		workerPool = new NioEventLoopGroup();
		
		final TcpClientChannelInitializer init = new TcpClientChannelInitializer(transponder);
		
		final Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(workerPool)
			   	 .remoteAddress(netAddr)
				 .channel(NioSocketChannel.class)
				 .handler(init);
			
		updateConnectionState(new ConnectionInfo(ConnectionState.CONNECTING, netAddr));
		communicationChannel = bootstrap.connect();
		if(wait) {
			try {
				communicationChannel.sync();
			} catch (final InterruptedException e) {
				System.err.println("Waiting for connection was interupted");
			}
		}
		communicationChannel.addListener(new ChannelActiveListener());
	}

	@Override
	public void stop() {
		if(communicationChannel != null) {
			try {
				updateConnectionState(new ConnectionInfo(ConnectionState.DISCONNECTING, getAddress()));
				communicationChannel.channel().closeFuture().sync();
				workerPool.shutdownGracefully().sync();
			} catch (final InterruptedException e) {
				System.err.println("error in stop: " + e.getMessage());
				e.printStackTrace();
			}
			finally {
				updateConnectionState(new ConnectionInfo(ConnectionState.DISCONNECTING, getAddress()));
			}
		}
		netAddr = null;
	}

	@Override
	public void destroy() {
		netAddr = null;
		workerPool = null;
		communicationChannel = null;
	}

	@Override
	public void send(final RawData msg) {
		System.out.println("Sending " + msg.getSize() + " byte");
		communicationChannel.channel().writeAndFlush(msg.getBytes()).addListener(new ChannelFutureListener() {
			
			@Override
			public void operationComplete(final ChannelFuture future) throws Exception {
				printOperationState(future);
			}
		});
	}

	@Override
	public void setRawDataReceiver(final RawDataChannel messageHandler) {
		transponder.setRawDataChannel(messageHandler);
	}

	@Override
	public InetSocketAddress getAddress() {
		return netAddr;
	}

	@Override
	public ConnectionState getConnectionState() {
		return state;
	}
	
	private void updateConnectionState(final ConnectionInfo connInfo) {
		state = connInfo.getConnectionState();
		if(csl != null) {
			csl.stateChange(connInfo);
		}
	}
	
	private class ChannelActiveListener implements ChannelFutureListener {

		@Override
		public void operationComplete(final ChannelFuture future) throws Exception {
			printOperationState(future);
			updateConnectionState(new ConnectionInfo(ConnectionState.CONNECTED, getAddress()));
		}
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
	public void addConnectionStateListener(final ConnectionStateListener listener) {
		csl = listener;
	}

}
