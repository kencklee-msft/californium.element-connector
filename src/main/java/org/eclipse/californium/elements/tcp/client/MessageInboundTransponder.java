package org.eclipse.californium.elements.tcp.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.net.InetSocketAddress;

import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.RawDataChannel;

@Sharable
public class MessageInboundTransponder extends ChannelInboundHandlerAdapter{
	
	private final String host;
	private final int port;

	public MessageInboundTransponder(final String host, final int port) {
		this.host = host;
		this.port = port;
	}
	
	private RawDataChannel rawDataChannel;

	public void setRawDataChannel(final RawDataChannel rawDataChannel) {
		this.rawDataChannel = rawDataChannel;
	}
	
@Override
	public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
		if(rawDataChannel != null) {
			final ByteBuf bb = (ByteBuf) msg;
			final byte[] message = new byte[bb.capacity()];
			bb.getBytes(0, message);
			final InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
			rawDataChannel.receiveData(new RawData(message, address));
		}
	}

}
