package org.eclipse.californium.elements.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class RawOutboundClientHandler extends ChannelOutboundHandlerAdapter{
	
	
	@Override
	public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
		final byte[] message = (byte[])msg;
		
		final ByteBuf bb = ctx.alloc().buffer(message.length);
		bb.writeBytes(message);
		ctx.write(bb, promise);
	}
	
	@Override
	public void flush(final ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}
	
}
