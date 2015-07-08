package org.eclipse.californium.elements.tcp;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.internal.StringUtil;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.RawDataChannel;

@Sharable
public class MessageInboundTransponder extends SimpleChannelInboundHandler<byte[]> {

	private static final Logger LOG = Logger.getLogger( MessageInboundTransponder.class.getName() );


	//use to notify different event  without blocking netty's thread.
	//should be taken from a configurable pool
	private final  Executor notifyThread;
	private  RawDataChannel rawDataChannel;

	public MessageInboundTransponder(final Executor callBackExecutor) {
		this.notifyThread = callBackExecutor;
	}

	public void setRawDataChannel(final RawDataChannel rawDataChannel) {
		this.rawDataChannel = rawDataChannel;
	}

	@Override
	public void channelRead0(final ChannelHandlerContext ctx, final byte[] msg) throws Exception {
		final InetSocketAddress remote = (InetSocketAddress) ctx.channel().remoteAddress();
		if(rawDataChannel != null) {
			notify(msg, remote);
		}
	}

	private void notify(final byte[] message, final InetSocketAddress remote) {
		notifyThread.execute(new Runnable() {

			@Override
			public void run() {
				final RawData raw = new RawData(message, remote);
				if (LOG.isLoggable(Level.FINEST)) {
					LOG.finest("RAW INBOUND: " + StringUtil.toHexString(message) + " from " + remote);
				}
				rawDataChannel.receiveData(raw);
			}
		});
	}
}
