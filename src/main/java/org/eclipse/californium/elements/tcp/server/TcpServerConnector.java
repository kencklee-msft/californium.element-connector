package org.eclipse.californium.elements.tcp.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
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
import org.eclipse.californium.elements.tcp.ConnectionInfo;
import org.eclipse.californium.elements.tcp.ConnectionInfo.ConnectionState;
import org.eclipse.californium.elements.tcp.ConnectionStateListener;
import org.eclipse.californium.elements.tcp.MessageInboundTransponder;
import org.eclipse.californium.elements.tcp.framing.FourByteFieldPrepender;
import org.eclipse.californium.elements.tcp.framing.FourByteFrameDecoder;
import org.eclipse.californium.elements.utils.FutureAggregate;

public class TcpServerConnector extends ChannelInitializer<SocketChannel> implements Connector, RemoteConnectionListener {

	private static final Logger LOG = Logger.getLogger( TcpServerConnector.class.getName() );

	private final InetSocketAddress address;
	private final MessageInboundTransponder transponder;
	private final TcpServerConnectionMgr connMgr;

	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private ChannelFuture communicationChannel;
	private ConnectionState state = ConnectionState.DISCONNECTED;

	private ConnectionStateListener csl;

	public TcpServerConnector(final String bindAddress, final int bindPort) {
		this(bindAddress, bindPort, Executors.newCachedThreadPool());
	}

	public TcpServerConnector(final String bindAddress, final int bindPort, final Executor callbackExecutor) {
		address = new InetSocketAddress(bindAddress, bindPort);
		transponder = new MessageInboundTransponder(callbackExecutor);
		connMgr = new TcpServerConnectionMgr(this, callbackExecutor);
	}

	@Override
	protected void initChannel(final SocketChannel ch) throws Exception {
		ch.pipeline().addLast(new FourByteFrameDecoder(),
				new FourByteFieldPrepender(),
				new ByteArrayDecoder(),
				new ByteArrayEncoder(),
				connMgr,
				transponder);
	}

	@Override
	public Future<?> start() throws IOException {
		LOG.info("Staring TCP SERVER connector with Xconn");
		bossGroup = new NioEventLoopGroup(50);
		workerGroup = new NioEventLoopGroup(50);

		final ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(bossGroup, workerGroup)
		.localAddress(address.getPort())
		.channel(NioServerSocketChannel.class)
		.childHandler(this)
		.option(ChannelOption.SO_BACKLOG, 128)
		.childOption(ChannelOption.SO_KEEPALIVE, true)
		.childOption(ChannelOption.TCP_NODELAY, true);

		communicationChannel = bootstrap.bind();
		communicationChannel.addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(final ChannelFuture future) throws Exception {
				printOperationState(future);
				incomingConnectionStateChange(new ConnectionInfo(ConnectionState.CONNECTED, (InetSocketAddress)future.channel().remoteAddress()));
			}
		});

		return communicationChannel;
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
			LOG.finest("Sending " + msg.getSize() + " byte" + " to " + ch.remoteAddress());
			return ch.writeAndFlush(msg.getBytes());
		}
		else {
			LOG.finest("No Channel available to message.  Unknown Connection " + msg.getAddress());
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

	public void setConnectionStateListener(final ConnectionStateListener csl) {
		this.csl = csl;
	}
}
