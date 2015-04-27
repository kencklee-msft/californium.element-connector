package org.eclipse.californium.elements.tcp.server;

import io.netty.bootstrap.ServerBootstrap;
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
import org.eclipse.californium.elements.tcp.TcpChannelInitializer;
import org.eclipse.californium.elements.tcp.client.MessageInboundTransponder;

public class TCPServerConnector implements StatefulConnector {
	
	private static final Logger LOG = Logger.getLogger( TCPServerConnector.class.getName() );

	
	private final InetSocketAddress address;	
	private final MessageInboundTransponder transponder;

	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private ChannelFuture communicationChannel;


	public TCPServerConnector() {
		this(null);
	}
	
	public TCPServerConnector(final int port) {
		this(new InetSocketAddress(port));
	}
	
	public TCPServerConnector(final String address, final int port) {
		this(new InetSocketAddress(address, port));
	}

	public TCPServerConnector(final InetSocketAddress address) {
		this.address = address;
		transponder = new MessageInboundTransponder(address.getHostName(), address.getPort());
	}
	
	@Override
	public void start(final boolean wait) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start() throws IOException {
		LOG.info("Staring TCP SERVER connector");
		bossGroup = new NioEventLoopGroup();
		workerGroup = new NioEventLoopGroup();
		final TcpChannelInitializer init = new TcpChannelInitializer(transponder);
		final ServerBootstrap bootsrap = new ServerBootstrap();
		bootsrap.group(bossGroup, workerGroup)
				.localAddress(address.getPort())
				.channel(NioServerSocketChannel.class)
				.childHandler(init)
				.option(ChannelOption.SO_BACKLOG, 128)
				.childOption(ChannelOption.SO_KEEPALIVE, true);

		communicationChannel = bootsrap.bind();
	}

	@Override
	public void stop() {
		communicationChannel.channel().close();
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
	}

	@Override
	public void destroy() {
		communicationChannel = null;
		bossGroup = null;
		workerGroup = null;
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
		// TODO Auto-generated method stub
		return null;
	}

}
