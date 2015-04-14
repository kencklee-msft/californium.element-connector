package org.eclipse.californium.elements.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;


public class RawClientHandler extends SimpleChannelInboundHandler<ByteBuf>{
	
	private ChannelHandlerContext ctx;
	
	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		System.out.println("Channel is active");
	}
	

	@Override
	protected void channelRead0(final ChannelHandlerContext ctx, final ByteBuf msg) throws Exception {
		System.out.println("Client received: " + msg.toString(CharsetUtil.UTF_8));
	}
	
	@Override
	 public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	 } 
}
