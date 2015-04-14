package org.eclipse.californium.elements.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;


public class RawInboundClientHandler extends ChannelInboundHandlerAdapter{
		
	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		System.out.println("channel active");
		ctx.writeAndFlush(ctx.alloc().buffer(0).writeBytes("Loggedin".getBytes()));
	}

	@Override
	public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
		final ByteBuf bb = (ByteBuf)msg;
		bb.setIndex(0, 0);
		bb.writeBytes("MOD".getBytes());
		super.channelRead(ctx, bb);
	}
	
	@Override
	 public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	 } 
	
	
}
