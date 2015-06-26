package org.eclipse.californium.elements.tcp.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.RawDataChannel;
import org.eclipse.californium.elements.StatefulConnector;
import org.eclipse.californium.elements.config.TCPConnectionConfig;
import org.eclipse.californium.elements.tcp.ConnectionInfo;
import org.eclipse.californium.elements.tcp.ConnectionInfo.ConnectionState;
import org.eclipse.californium.elements.tcp.ConnectionStateListener;
import org.eclipse.californium.elements.tcp.MessageInboundTransponder;
import org.eclipse.californium.elements.tcp.server.RemoteConnectionListener;

public class TcpClientConnector implements StatefulConnector, RemoteConnectionListener {

	private static final Logger LOG = Logger.getLogger( TcpClientConnector.class.getName() );

	private final MessageInboundTransponder transponder;
	private final TCPConnectionConfig cfg;

	private InetSocketAddress netAddr;
	private NioEventLoopGroup workerPool;
	private ChannelFuture communicationChannel;
	private ConnectionState state = ConnectionState.DISCONNECTED;

	private ConnectionStateListener csl;


	public TcpClientConnector(final TCPConnectionConfig cfg) {
		this.cfg = cfg;
		transponder = new MessageInboundTransponder(cfg.getCallBackExecutor() != null ? 
				cfg.getCallBackExecutor() : Executors.newCachedThreadPool());
		this.csl = cfg.getListener();
	}



	@Override
	public void start() throws IOException {
		start(false);
	}

	@Override
	public void start(final boolean wait) throws IOException {
		LOG.info("Staring TCP CLIENT connector");
		netAddr = new InetSocketAddress(cfg.getRemoteAddress(), cfg.getRemotePort());
		workerPool = new NioEventLoopGroup();

		final TcpClientChannelInitializer init = new TcpClientChannelInitializer(transponder, this);
		if(cfg.isSecured()) {
			init.addTLS(cfg.getSSlContext());
		}

		final Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(workerPool)
				 .remoteAddress(netAddr)
				 .channel(NioSocketChannel.class)
				 .handler(init);

		incomingConnectionStateChange(new ConnectionInfo(ConnectionState.CONNECTING, netAddr));
		communicationChannel = bootstrap.connect();
		if(wait) {
			try {
				communicationChannel.sync();
			} catch (final InterruptedException e) {
				LOG.log(Level.SEVERE, "Waiting for connection was interupted", e);
			}
		}
		communicationChannel.addListener(new ChannelActiveListener());
	}

	@Override
	public void stop() {
		if(communicationChannel != null) {
			try {
				incomingConnectionStateChange(new ConnectionInfo(ConnectionState.DISCONNECTING, getAddress()));
				final ChannelFuture closeFuture = communicationChannel.channel().closeFuture();
				final Future<?> workerPoolShutdown = workerPool.shutdownGracefully();
				boolean result = closeFuture.await(1000);
				LOG.finest("Stopping Communication Channel succes?: " + result);
				result = workerPoolShutdown.await(1000);
				LOG.finest("Stopping WorkerPool succes?: " + result);

			} catch (final InterruptedException e) {
				LOG.log(Level.SEVERE, "error in stop: ", e);
			}
			finally {
				incomingConnectionStateChange(new ConnectionInfo(ConnectionState.DISCONNECTING, getAddress()));
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
		LOG.finest("Sending " + msg.getSize() + " byte");
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
	/*
	 * returns the address resolved on start, if the netAddr is null,
	 * the server is not stared, resolve the address and send the new object
	 * the netAddr will stil be resolved once it is ask to start
	 * (non-Javadoc)
	 * @see org.eclipse.californium.elements.Connector#getAddress()
	 */
	public InetSocketAddress getAddress() {
		return netAddr == null ? new InetSocketAddress(cfg.getRemoteAddress(), cfg.getRemotePort()) : netAddr;
	}

	@Override
	public ConnectionState getConnectionState() {
		return state;
	}


	private class ChannelActiveListener implements ChannelFutureListener {

		@Override
		public void operationComplete(final ChannelFuture future) throws Exception {
			printOperationState(future);
			incomingConnectionStateChange(new ConnectionInfo(ConnectionState.CONNECTED, getAddress()));
		}
	}

	@Override
	public void incomingConnectionStateChange(final ConnectionInfo info) {
		state = info.getConnectionState();
		if(csl != null) {
			csl.stateChange(info);
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
		LOG.finest(sb.toString());
	}

	@Override
	public void addConnectionStateListener(final ConnectionStateListener listener) {
		csl = listener;
	}




}
