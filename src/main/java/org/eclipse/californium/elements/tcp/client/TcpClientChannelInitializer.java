package org.eclipse.californium.elements.tcp.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.eclipse.californium.elements.tcp.ConnectionInfo;
import org.eclipse.californium.elements.tcp.ConnectionInfo.ConnectionState;
import org.eclipse.californium.elements.tcp.MessageInboundTransponder;
import org.eclipse.californium.elements.tcp.RawInboundClientHandler;
import org.eclipse.californium.elements.tcp.RawOutboundClientHandler;
import org.eclipse.californium.elements.tcp.server.RemoteConnectionListener;

public class TcpClientChannelInitializer extends ChannelInitializer<SocketChannel>{
	private static final Logger LOG = Logger.getLogger( TcpClientChannelInitializer.class.getName() );
	private static final String SSL_HANDLER_ID = "ssl";

	private final MessageInboundTransponder transponder;

	private SSLContext sslContext;
	private final RemoteConnectionListener remoteConnectionListner;
	
	public TcpClientChannelInitializer(final MessageInboundTransponder transponder, final RemoteConnectionListener listener) {
		this.transponder = transponder;
		this.remoteConnectionListner = listener;
	}
	
	public void addTLS(final SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	@Override
	protected void initChannel(final SocketChannel ch) throws Exception {
		if(sslContext != null) {
			final SSLEngine engine = sslContext.createSSLEngine();
			engine.setUseClientMode(true);
			final SslHandler sslHandler = new SslHandler(engine);
			ch.pipeline().addFirst(SSL_HANDLER_ID, sslHandler);//init the TLS since we are the client
			remoteConnectionListner.incomingConnectionStateChange(new ConnectionInfo(ConnectionState.TLS_HANDSHAKE_STARTED, ch.remoteAddress()));
			asychNotifyOnCompleteHandshake(sslHandler.handshakeFuture());
		}
		ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4), new LengthFieldPrepender(4));
		ch.pipeline().addLast(new RawInboundClientHandler(), new RawOutboundClientHandler());
		ch.pipeline().addLast(transponder);
	}
	
	private void asychNotifyOnCompleteHandshake(final Future<Channel> handShakePromise) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					final Channel c = handShakePromise.get();
					remoteConnectionListner.incomingConnectionStateChange(new ConnectionInfo(ConnectionState.CONNECTED_SECURE, (InetSocketAddress)c.remoteAddress()));
					LOG.info("TLS Handshake was completed ");
				} catch (final InterruptedException e) {
					LOG.log(Level.SEVERE, "Could not wait for TLS Handshake to be completed, waiting thread was interupted ", e);
				} catch (final ExecutionException e) {
					LOG.log(Level.SEVERE, "Could not wait for TLS Handshake to be completed, waiting thread Encountered and Error ", e);
				}
				
			}
		}).start();
	}
}
