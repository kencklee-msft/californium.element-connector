package org.eclipse.californium.elements.tcp.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.eclipse.californium.elements.tcp.MessageInboundTransponder;
import org.eclipse.californium.elements.tcp.RawInboundClientHandler;
import org.eclipse.californium.elements.tcp.RawOutboundClientHandler;

public class TcpClientChannelInitializer extends ChannelInitializer<SocketChannel>{
	private static final Logger LOG = Logger.getLogger( TcpClientChannelInitializer.class.getName() );
	private static final String SSL_HANDLER_ID = "ssl";

	private final MessageInboundTransponder transponder;

	private SSLContext sslContext;
	
	public TcpClientChannelInitializer(final MessageInboundTransponder transponder) {
		this.transponder = transponder;
	}
	
	public void addTLS(final SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	@Override
	protected void initChannel(final SocketChannel ch) throws Exception {
		if(sslContext != null) {
			final SSLEngine engine = sslContext.createSSLEngine();
			engine.setUseClientMode(true);
			ch.pipeline().addFirst(SSL_HANDLER_ID, new SslHandler(engine));//init the TLS since we are the client
		}
		ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4), new LengthFieldPrepender(4));
		ch.pipeline().addLast(new RawInboundClientHandler(), new RawOutboundClientHandler());
		ch.pipeline().addLast(transponder);
	}
	
	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		asychNotifyOnCompleteHandshake(((SslHandler)(ctx.pipeline().get(SSL_HANDLER_ID))).handshakeFuture());
		super.channelActive(ctx);
	}
	
	private void asychNotifyOnCompleteHandshake(final Future<Channel> handShakePromise) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					final Channel c = handShakePromise.get();
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
