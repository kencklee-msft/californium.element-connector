package org.eclipse.californium.elements.tcp.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import org.eclipse.californium.elements.tcp.ConnectionInfo;
import org.eclipse.californium.elements.tcp.ConnectionInfo.ConnectionState;

@Sharable
public class TcpServerConnectionMgr extends ChannelInboundHandlerAdapter{	
	private static final Logger LOG = Logger.getLogger( TcpServerConnectionMgr.class.getName() );

	
	//use to notify different event  without blocking netty's thread.
	//should be taken from a configurable pool
	private final Executor notifyThread;
	
	/**
	 * this is not very efficient, but will suffice for POC
	 */
	private final ConcurrentHashMap<InetSocketAddress, Channel> connections = new ConcurrentHashMap<InetSocketAddress, Channel>();
	private final RemoteConnectionListener listener;
	
	public TcpServerConnectionMgr(final RemoteConnectionListener listener, final Executor notifyThread) {
		this.listener = listener;
		this.notifyThread = notifyThread;
	}

	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		final InetSocketAddress remote = (InetSocketAddress)ctx.channel().remoteAddress();
		connections.put(remote, ctx.channel());
		notify(new ConnectionInfo(ConnectionState.NEW_INCOMING_CONNECT, remote));
		super.channelActive(ctx);
	}
	
	@Override
	public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
		final InetSocketAddress remote = (InetSocketAddress)ctx.channel().remoteAddress();
		final Channel ch = connections.remove(remote);
		if(ch == null) {
			LOG.finest("Channel did not exist");
		}
		else {
			notify(new ConnectionInfo(ConnectionState.NEW_INCOMING_DISCONNECT, remote));
		}
		super.channelInactive(ctx);
	}
	
	public Channel getChannel(final InetSocketAddress address) {
		LOG.finest("request for Channel " + address.toString());
		return connections.get(address);
	}
	
	
	private void notify(final ConnectionInfo info) {
		notifyThread.execute(new Runnable() {
			@Override
			public void run() {
				listener.incomingConnectionStateChange(info);
			}
		});
	}
}
