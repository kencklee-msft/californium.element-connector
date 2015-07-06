package org.eclipse.californium.elements.tcp.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.eclipse.californium.elements.config.TCPConnectionConfig.SSLCLientCertReq;
import org.eclipse.californium.elements.tcp.ConnectionInfo;
import org.eclipse.californium.elements.tcp.ConnectionInfo.ConnectionState;
import org.eclipse.californium.elements.tcp.MessageInboundTransponder;
import org.eclipse.californium.elements.tcp.framing.FourByteFieldPrepender;
import org.eclipse.californium.elements.tcp.framing.FourByteFrameDecoder;

public class TcpServerChannelInitializer extends ChannelInitializer<SocketChannel> {
	private static final Logger LOG = Logger.getLogger( TcpServerChannelInitializer.class.getName() );
	private static final String SSL_HANDLER_ID = "ssl";

	private final MessageInboundTransponder transponder;
	private final TcpServerConnectionMgr connMgr;

	private SSLContext sslContext;
	private SSLCLientCertReq req;
	private String[] supportedTLSVerions;
	private final RemoteConnectionListener remoteConnectionListener;

	public TcpServerChannelInitializer(
			final MessageInboundTransponder transponder, final TcpServerConnectionMgr connMgr, final RemoteConnectionListener listener) {
		this.transponder = transponder;
		this.connMgr = connMgr;
		this.remoteConnectionListener = listener;
	}

	public void addTLS(final SSLContext sslContext, final SSLCLientCertReq req, final String[] supportedTLSVerions) {
		this.sslContext = sslContext;
		this.req = req;
		this.supportedTLSVerions = supportedTLSVerions;
	}

	@Override
	protected void initChannel(final SocketChannel ch) throws Exception {
		if(sslContext != null) {
			final SSLEngine engine = sslContext.createSSLEngine();
			switch(req) {
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
			if(supportedTLSVerions != null && supportedTLSVerions.length > 0) {
				engine.setEnabledProtocols(supportedTLSVerions);
			}
			engine.setEnableSessionCreation(true);
			ch.pipeline().addFirst(SSL_HANDLER_ID, new SslHandler(engine));//init the TLS since we are the client
		}
		ch.pipeline().addLast(new FourByteFrameDecoder(), new FourByteFieldPrepender());
		ch.pipeline().addLast(new ByteArrayDecoder(), new ByteArrayEncoder());
		ch.pipeline().addLast(connMgr);
		ch.pipeline().addLast(transponder);
	}

	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		final InetSocketAddress remote = (InetSocketAddress)ctx.channel().remoteAddress();

		((SslHandler)(ctx.pipeline().get(SSL_HANDLER_ID))).handshakeFuture().addListener(new TLSHandshakeListener());
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
	}

	private class TLSHandshakeListener implements GenericFutureListener<Future<Channel>> {

		@Override
		public void operationComplete(final Future<Channel> future) throws Exception {
			remoteConnectionListener.incomingConnectionStateChange(
					new ConnectionInfo(ConnectionState.CONNECTED_SECURE, (InetSocketAddress)future.get().remoteAddress()));
			LOG.info("TLS Handshake was completed ");			
		}
	}
}
