package org.eclipse.californium.elements.tcp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;


public class RawInboundClientHandler extends ChannelInboundHandlerAdapter{
		
	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		System.out.println("channel active");
	}

	@Override
	public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
		super.channelRead(ctx, msg);
	}
	
	@Override
	 public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	 } 
	
	
}
