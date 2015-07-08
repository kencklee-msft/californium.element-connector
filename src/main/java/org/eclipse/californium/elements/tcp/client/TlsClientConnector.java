package org.eclipse.californium.elements.tcp.client;

import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.eclipse.californium.elements.utils.FutureAggregate;

public class TlsClientConnector extends TcpClientConnector {

	private static final Logger LOG = Logger.getLogger( TlsClientConnector.class.getName() );
	private static final String SSL_HANDLER_ID = "ssl";

	private final SSLContext sslContext;
	private SslHandler sslHandler;

	public TlsClientConnector(final String remoteAddress, final int remotePort, final SSLContext sslContext) {
		this(remoteAddress, remotePort, sslContext, Executors.newCachedThreadPool());
	}

	public TlsClientConnector(final String remoteAddress, final int remotePort, final SSLContext sslContext, final Executor callbackExecutor) {
		super(remoteAddress, remotePort, callbackExecutor);
		this.sslContext = sslContext;
	}

	@Override
	protected void initChannel(final SocketChannel ch) throws Exception {
		super.initChannel(ch);
		LOG.log(Level.FINE, "initializing TLS Components");
		final SSLEngine engine = sslContext.createSSLEngine();
		engine.setUseClientMode(true);
		sslHandler = new SslHandler(engine);
		ch.pipeline().addFirst(SSL_HANDLER_ID, sslHandler);//init the TLS since we are the client
	}


	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Future<?> start() throws IOException {
		return new FutureAggregate(super.start(), sslHandler.handshakeFuture());
	}

	@Override
	public void destroy() {
		super.destroy();
		sslHandler = null;
	}

}
