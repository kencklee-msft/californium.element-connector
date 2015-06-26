package org.eclipse.californium.elements.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.util.logging.Logger;

public class RawOutboundClientHandler extends ChannelOutboundHandlerAdapter{

	private static final Logger LOG = Logger.getLogger( RawOutboundClientHandler.class.getName() );


	@Override
	public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
		final byte[] message = (byte[])msg;
		LOG.finest("will allocate a buffer of " + message.length);
		final ByteBuf bb = ctx.alloc().buffer(message.length);
		bb.writeBytes(message);
		ctx.write(bb, promise);
	}

	@Override
	public void flush(final ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

}
