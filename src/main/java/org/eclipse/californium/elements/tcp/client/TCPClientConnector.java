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
import org.eclipse.californium.elements.tcp.TcpChannelInitializer;

public class TCPClientConnector implements StatefulConnector {
	
	private static final Logger LOG = Logger.getLogger( TCPClientConnector.class.getName() );
	
	private final MessageInboundTransponder transponder = new MessageInboundTransponder();
	private final String addr;
	private final int port;
	
	private InetSocketAddress netAddr;
	private NioEventLoopGroup workerPool;
	private ChannelFuture communicationChannel;
	private ConnectionState state = ConnectionState.DISCONNECTED;

	private final ConnectionStateListener csl;
	
	public TCPClientConnector(final String addr, final int port) {
		this(addr, port, null);
	}
	
	public TCPClientConnector(final String addr, final int port, final ConnectionStateListener csl) {
		this.addr = addr;
		this.port = port;
		this.csl = csl;
	}

	@Override
	public void start() throws IOException {
		start(false);
	}
	
	@Override
	public void start(final boolean wait) throws IOException {
		LOG.info("Staring TCP connector");
		netAddr = new InetSocketAddress(addr, port);
		workerPool = new NioEventLoopGroup();
		
		final TcpChannelInitializer init = new TcpChannelInitializer(transponder);
		
		final Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(workerPool)
			   	 .remoteAddress(netAddr)
				 .channel(NioSocketChannel.class)
				 .handler(init);
			
		updateConnectionState(ConnectionState.CONNECTING);
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
				updateConnectionState(ConnectionState.DISCONNECTING);
				communicationChannel.channel().closeFuture().sync();
				workerPool.shutdownGracefully().sync();
			} catch (final InterruptedException e) {
				System.err.println("error in stop: " + e.getMessage());
				e.printStackTrace();
			}
			finally {
				updateConnectionState(ConnectionState.DISCONNECTED);
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
	
	private void updateConnectionState(final ConnectionState connState) {
		state = connState;
		if(csl != null) {
			csl.stateChange(connState);
		}
	}
	
	private class ChannelActiveListener implements ChannelFutureListener {

		@Override
		public void operationComplete(final ChannelFuture future) throws Exception {
			printOperationState(future);
			updateConnectionState(ConnectionState.CONNECTED);
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

}
