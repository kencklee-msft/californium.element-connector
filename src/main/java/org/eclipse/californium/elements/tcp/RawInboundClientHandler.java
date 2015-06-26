package org.eclipse.californium.elements.tcp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.logging.Logger;


public class RawInboundClientHandler extends ChannelInboundHandlerAdapter{

	private static final Logger LOG = Logger.getLogger( RawInboundClientHandler.class.getName() );

	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		LOG.finest("channel active in raw Inbound Client");
		super.channelActive(ctx);
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
