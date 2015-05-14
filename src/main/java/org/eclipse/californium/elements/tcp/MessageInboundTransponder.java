package org.eclipse.californium.elements.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.StringUtil;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.RawDataChannel;

@Sharable
public class MessageInboundTransponder extends ChannelInboundHandlerAdapter{
	
	private final ConcurrentHashMap<InetSocketAddress, RawDataChannel> boundedRawChannel = new ConcurrentHashMap<InetSocketAddress, RawDataChannel>();

	public MessageInboundTransponder() {
	}
	
	public void addRawDataChannel(final RawDataChannel rawDataChannel, final InetSocketAddress remote) {
		
		boundedRawChannel.put(remote, rawDataChannel);
	}
	
	@Override
	public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
		final InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
		final RawDataChannel rawDataChannel = boundedRawChannel.get(address);
		if(rawDataChannel != null) {
			final ByteBuf bb = (ByteBuf) msg;
			final byte[] message = new byte[bb.capacity()];
			bb.getBytes(0, message);
			final RawData raw = new RawData(message, address);
			System.out.println("RAW INBOUND: " + StringUtil.toHexString(message) + " from " + address.toString());
			rawDataChannel.receiveData(raw);
		}
	}

}
