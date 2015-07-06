package org.eclipse.californium.elements.tcp.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.ssl.SslHandler;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.eclipse.californium.elements.tcp.MessageInboundTransponder;
import org.eclipse.californium.elements.tcp.framing.FourByteFieldPrepender;
import org.eclipse.californium.elements.tcp.framing.FourByteFrameDecoder;
import org.eclipse.californium.elements.utils.TransitiveFuture;

public class TcpClientChannelInitializer extends ChannelInitializer<SocketChannel>{
	private static final Logger LOG = Logger.getLogger( TcpClientChannelInitializer.class.getName() );
	private static final String SSL_HANDLER_ID = "ssl";

	private final MessageInboundTransponder transponder;

	private SSLContext sslContext;
	private TransitiveFuture<Channel> connectionSecured;

	public TcpClientChannelInitializer(final MessageInboundTransponder transponder) {
		this.transponder = transponder;
	}

	public void addTLS(final SSLContext sslContext, final TransitiveFuture<Channel> connectionSecured) {
		this.sslContext = sslContext;
		this.connectionSecured = connectionSecured;
	}

	@Override
	protected void initChannel(final SocketChannel ch) throws Exception {
		LOG.log(Level.FINE, "initializing TCP Channel");
		if(sslContext != null) {
			LOG.log(Level.FINE, "initializing TLS Comonents");
			final SSLEngine engine = sslContext.createSSLEngine();
			engine.setUseClientMode(true);
			final SslHandler sslHandler = new SslHandler(engine);
			ch.pipeline().addFirst(SSL_HANDLER_ID, sslHandler);//init the TLS since we are the client
			connectionSecured.setTransitiveFuture(sslHandler.handshakeFuture());
		}
		ch.pipeline().addLast(new FourByteFrameDecoder(), new FourByteFieldPrepender());
		ch.pipeline().addLast(new ByteArrayDecoder(), new ByteArrayEncoder());
		ch.pipeline().addLast(transponder);
	}
}
