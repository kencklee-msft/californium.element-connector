package org.eclipse.californium.elements.tcp.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.RawDataChannel;
import org.eclipse.californium.elements.StatefulConnector;
import org.eclipse.californium.elements.config.TCPConnectionConfig;
import org.eclipse.californium.elements.tcp.ConnectionStateListener;
import org.eclipse.californium.elements.tcp.MessageInboundTransponder;
import org.eclipse.californium.elements.utils.FutureAggregate;
import org.eclipse.californium.elements.utils.TransitiveFuture;

public class TcpClientConnector implements StatefulConnector {

	private static final Logger LOG = Logger.getLogger( TcpClientConnector.class.getName() );

	private final MessageInboundTransponder transponder;
	private final TCPConnectionConfig cfg;

	private InetSocketAddress netAddr;
	private NioEventLoopGroup workerPool;
	private ChannelFuture communicationChannel;

	private ConnectionStateListener csl;


	public TcpClientConnector(final TCPConnectionConfig cfg) {
		this.cfg = cfg;
		transponder = new MessageInboundTransponder(cfg.getCallBackExecutor() != null ? 
				cfg.getCallBackExecutor() : Executors.newCachedThreadPool());
		this.csl = cfg.getListener();
	}



	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Future<?> start() throws IOException {
		final LinkedList<Future<?>> connectionStep = new LinkedList<Future<?>>();
		LOG.info("Staring TCP CLIENT connector");
		netAddr = new InetSocketAddress(cfg.getRemoteAddress(), cfg.getRemotePort());
		workerPool = new NioEventLoopGroup();

		final TcpClientChannelInitializer init = new TcpClientChannelInitializer(transponder);
		if(cfg.isSecured()) {
			final TransitiveFuture<Channel> tlsSecuredFuture = new TransitiveFuture<Channel>();
			init.addTLS(cfg.getSSlContext(), tlsSecuredFuture);
			connectionStep.add(tlsSecuredFuture);
		}

		final Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(workerPool)
				 .remoteAddress(netAddr)
				 .channel(NioSocketChannel.class)
				 .handler(init);

		communicationChannel = bootstrap.connect();		
		communicationChannel.addListener(new ChannelActiveListener());
		connectionStep.addFirst(communicationChannel);
		return new FutureAggregate(connectionStep.toArray(new Future<?>[connectionStep.size()]));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Future<?> stop() {
		FutureAggregate aggregateFuture = null;
		if(communicationChannel != null) {
			aggregateFuture = new FutureAggregate(communicationChannel.channel().closeFuture(),
													  workerPool.shutdownGracefully());
		}
		netAddr = null;
		return aggregateFuture != null ? aggregateFuture : new FutureAggregate();
	}

	@Override
	public void destroy() {
		netAddr = null;
		workerPool = null;
		communicationChannel = null;
	}

	@Override
	public Future<?> send(final RawData msg) {
		LOG.finest("Sending " + msg.getSize() + " byte");
		return communicationChannel.channel().writeAndFlush(msg.getBytes());
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

	private class ChannelActiveListener implements ChannelFutureListener {

		@Override
		public void operationComplete(final ChannelFuture future) throws Exception {
			printOperationState(future);
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
