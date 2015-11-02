package org.eclipse.californium.elements.tcp.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.RawDataChannel;
import org.eclipse.californium.elements.tcp.MessageInboundTransponder;
import org.eclipse.californium.elements.tcp.framing.FourByteFieldPrepender;
import org.eclipse.californium.elements.tcp.framing.FourByteFrameDecoder;
import org.eclipse.californium.elements.utils.FutureAggregate;

public class TcpClientConnector extends ChannelInitializer<SocketChannel> implements Connector {

	private static final Logger LOG = Logger.getLogger( TcpClientConnector.class.getName() );

	protected final MessageInboundTransponder transponder;
	private final String remoteAddress;
	private final int remotePort;

	private InetSocketAddress netAddr;
	private NioEventLoopGroup workerPool;
	private ChannelFuture communicationChannel;

	public TcpClientConnector(final String remoteAddress, final int remotePort) {
		this(remoteAddress, remotePort, Executors.newCachedThreadPool());
	}

	public TcpClientConnector(final String remoteAddress, final int remotePort, final Executor callbackExecutor) {
		this.remoteAddress = remoteAddress;
		this.remotePort = remotePort;
		transponder = new MessageInboundTransponder(callbackExecutor);
	}

	@Override
	protected void initChannel(final SocketChannel ch) throws Exception {
		LOG.fine("initializing TCP Channel");
		ch.pipeline().addLast(new FourByteFrameDecoder(),
				new FourByteFieldPrepender(),
				new ByteArrayDecoder(),
				new ByteArrayEncoder(),
				transponder);
	}

	@Override
	public Future<?> start() throws IOException {
		LOG.info("Staring TCP CLIENT connector");
		workerPool = new NioEventLoopGroup();

		final InetSocketAddress inetSocketAddress = getAddress();
		final Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(workerPool).remoteAddress(inetSocketAddress).channel(NioSocketChannel.class).handler(this);

		communicationChannel = bootstrap.connect();
		communicationChannel.addListener(new ChannelActiveListener());
		return communicationChannel;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Future<?> stop() {
		netAddr = null;
		if(communicationChannel != null) {
			return new FutureAggregate(communicationChannel.channel().closeFuture(), workerPool.shutdownGracefully());
		}
		return new FutureAggregate();
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
		return netAddr == null ? new InetSocketAddress(remoteAddress, remotePort) : netAddr;
	}

	private class ChannelActiveListener implements ChannelFutureListener {

		@Override
		public void operationComplete(final ChannelFuture future) throws Exception {
			printOperationState(future);
		}
	}

	private static void printOperationState(final ChannelFuture future) {
		if (LOG.isLoggable(Level.FINEST)) {
			final StringBuilder sb = new StringBuilder();
			sb.append("Operation Complete:");
			if (future.isDone()) {
				if (future.isSuccess()) {
					sb.append("Operation was successful");
				} else if (!future.isSuccess() && !future.isCancelled()) {
					sb.append("Operation Failed: ").append(future.cause());
				} else {
					sb.append("Operation was cancelled");
				}
			} else {
				sb.append("Operation was not completed");
			}
			LOG.finest(sb.toString());
		}
	}

}
