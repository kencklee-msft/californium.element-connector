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
import org.eclipse.californium.elements.tcp.client.MessageInboundTransponder;

public class TcpServerConnector implements StatefulConnector {
	
	private static final Logger LOG = Logger.getLogger( TcpServerConnector.class.getName() );

	
	private final InetSocketAddress address;	
	private final MessageInboundTransponder transponder;
	private final TcpServerConnectionMgr connMgr;

	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private ChannelFuture communicationChannel;


	public TcpServerConnector() {
		this(null);
	}
	
	public TcpServerConnector(final int port) {
		this(new InetSocketAddress(port));
	}
	
	public TcpServerConnector(final String address, final int port) {
		this(new InetSocketAddress(address, port));
	}

	public TcpServerConnector(final InetSocketAddress address) {
		this.address = address;
		transponder = new MessageInboundTransponder(address.getHostName(), address.getPort());
		connMgr = new TcpServerConnectionMgr();
	}
	
	@Override
	public void start(final boolean wait) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start() throws IOException {
		LOG.info("Staring TCP SERVER connector with Xconn");
		bossGroup = new NioEventLoopGroup();
		workerGroup = new NioEventLoopGroup();
		final TcpServerChannelInitializer init = new TcpServerChannelInitializer(transponder, connMgr);
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
		// TODO Auto-generated method stub
		return null;
	}

}
