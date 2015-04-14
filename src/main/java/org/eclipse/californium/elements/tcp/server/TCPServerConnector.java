package org.eclipse.californium.elements.tcp.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.RawDataChannel;

public class TCPServerConnector implements Connector {
	
	private final InetSocketAddress address;

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
	}

	@Override
	public void start() throws IOException {
		final EventLoopGroup bossGroup = new NioEventLoopGroup();
		final EventLoopGroup workerGroup = new NioEventLoopGroup();
		
		try {
			final ServerBootstrap bootsrap = new ServerBootstrap();
			bootsrap.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<Channel>() {

						@Override
						protected void initChannel(final Channel arg0)
								throws Exception {
							// TODO Auto-generated method stub
							
						}
					})
					.option(ChannelOption.SO_BACKLOG, 128)
					.childOption(ChannelOption.SO_KEEPALIVE, true);
			
			final ChannelFuture future = bootsrap.bind(address.getPort()).sync();
			future.channel().closeFuture().sync();
		} catch (final InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void send(final RawData msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setRawDataReceiver(final RawDataChannel messageHandler) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public InetSocketAddress getAddress() {
		// TODO Auto-generated method stub
		return null;
	}

}
