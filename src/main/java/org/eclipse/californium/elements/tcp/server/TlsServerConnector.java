package org.eclipse.californium.elements.tcp.server;

import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

public class TlsServerConnector extends TcpServerConnector {

	public enum SSLClientCertReq {
		NONE,
		WANT,
		NEED
	}

	private static final Logger LOG = Logger.getLogger( TlsServerConnector.class.getName() );

	private static final String SSL_HANDLER_ID = "ssl";

	private final SSLContext sslContext;
	private final SSLClientCertReq clientCertRequest;
	private final String[] supportedTLSVersions;

	public TlsServerConnector(final String remoteAddress, final int remotePort, final SSLContext sslContext, final SSLClientCertReq clientCertRequest, final String... supportedTLSVersions) {
		this(remoteAddress, remotePort, Executors.newCachedThreadPool(), sslContext, clientCertRequest, supportedTLSVersions);
	}

	public TlsServerConnector(final String remoteAddress, final int remotePort, final Executor callbackExecutor, final SSLContext sslContext, final SSLClientCertReq clientCertRequest, final String... supportedTLSVersions) {
		super(remoteAddress, remotePort, callbackExecutor);
		this.sslContext = sslContext;
		this.clientCertRequest = clientCertRequest;
		this.supportedTLSVersions = supportedTLSVersions;
	}

	@Override
	protected void initChannel(final SocketChannel ch) throws Exception {
		super.initChannel(ch);
		final SSLEngine engine = sslContext.createSSLEngine();
		switch(clientCertRequest) {
			case NONE:
				engine.setWantClientAuth(false);
				break;
			case WANT:
				engine.setWantClientAuth(true);
				break;
			case NEED:
				engine.setNeedClientAuth(true);
				break;
			default:
				throw new IllegalArgumentException("Impossible Client Certificate request strategy");
		}
		engine.setUseClientMode(false);
		if(supportedTLSVersions != null && supportedTLSVersions.length > 0) {
			engine.setEnabledProtocols(supportedTLSVersions);
		}
		engine.setEnableSessionCreation(true);
		ch.pipeline().addFirst(SSL_HANDLER_ID, new SslHandler(engine));
	}

}
