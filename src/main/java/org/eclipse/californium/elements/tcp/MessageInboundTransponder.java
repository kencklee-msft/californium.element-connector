package org.eclipse.californium.elements.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.StringUtil;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.RawDataChannel;

@Sharable
public class MessageInboundTransponder extends ChannelInboundHandlerAdapter{
	
	//use to notify different event  without blocking netty's thread.
	//should be taken from a configurable pool
	private final ExecutorService notifyThread = Executors.newCachedThreadPool();
	private  RawDataChannel rawDataChannel;

	public MessageInboundTransponder() {
	}
	
	public void setRawDataChannel(final RawDataChannel rawDataChannel) {
		this.rawDataChannel = rawDataChannel;
	}
	
	@Override
	public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
		final InetSocketAddress remote = (InetSocketAddress) ctx.channel().remoteAddress();
		if(rawDataChannel != null) {
			final ByteBuf bb = (ByteBuf) msg;
			final byte[] message = new byte[bb.capacity()];
			bb.getBytes(0, message);
			notify(message, remote);
		}
	}
	
	private void notify(final byte[] message, final InetSocketAddress remote) {
		notifyThread.execute(new Runnable() {
			
			@Override
			public void run() {
				final RawData raw = new RawData(message, remote);
				System.out.println("RAW INBOUND: " + StringUtil.toHexString(message) + " from " + remote.toString());
				rawDataChannel.receiveData(raw);
			}
		});
	}
}
