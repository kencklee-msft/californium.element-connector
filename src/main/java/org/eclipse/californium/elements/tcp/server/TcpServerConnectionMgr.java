package org.eclipse.californium.elements.tcp.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.californium.elements.tcp.ConnectionInfo;
import org.eclipse.californium.elements.tcp.ConnectionInfo.ConnectionState;

@Sharable
public class TcpServerConnectionMgr extends ChannelInboundHandlerAdapter{
	
	//use to notify different event  without blocking netty's thread.
	//should be taken from a configurable pool
	private final ExecutorService notifyThread = Executors.newCachedThreadPool();
	
	/**
	 * this is not very efficient, but will suffice for POC
	 */
	private final ConcurrentHashMap<InetSocketAddress, Channel> connections = new ConcurrentHashMap<InetSocketAddress, Channel>();
	private final RemoteConnectionListener listener;
	
	public TcpServerConnectionMgr(final RemoteConnectionListener listener) {
		this.listener = listener;
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
			System.out.println("Channel did not exist");
		}
		else {
			notify(new ConnectionInfo(ConnectionState.NEW_INCOMING_DISCONNECT, remote));
		}
		super.channelInactive(ctx);
	}
	
	public Channel getChannel(final InetSocketAddress address) {
		System.out.println("request for Channel " + address.toString());
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
