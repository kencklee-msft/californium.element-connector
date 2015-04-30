package org.eclipse.californium.elements.tcp.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

@Sharable
public class TcpServerConnectionMgr extends ChannelInboundHandlerAdapter{
	
	private final ConcurrentHashMap<InetSocketAddress, Channel> connections = new ConcurrentHashMap<InetSocketAddress, Channel>();
	
	public TcpServerConnectionMgr() {
		
	}

	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		System.out.println("Channel Avtive " + ctx.channel().remoteAddress().toString() + " " + Math.random());
		connections.put((InetSocketAddress)ctx.channel().remoteAddress(), ctx.channel());
		super.channelActive(ctx);
	}
	
	@Override
	public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
		final Channel ch = connections.remove(ctx.channel().remoteAddress());
		if(ch == null) {
			System.out.println("Channel did not exist");
		}
		super.channelInactive(ctx);
	}
	
	public Channel getChannel(final InetSocketAddress address) {
		return connections.get(address);
		
	}
}
