package org.eclipse.californium.elements.tcp.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;

import org.eclipse.californium.elements.config.TCPConnectionConfig.SSLCLientCertReq;
import org.eclipse.californium.elements.tcp.MessageInboundTransponder;
import org.eclipse.californium.elements.tcp.RawInboundClientHandler;
import org.eclipse.californium.elements.tcp.RawOutboundClientHandler;

public class TcpServerChannelInitializer extends ChannelInitializer<SocketChannel> {

	private final MessageInboundTransponder transponder;
	private final TcpServerConnectionMgr connMgr;
	private SslContext sslContext;
	private String[] tlsVersions;
	private SSLCLientCertReq req;

	public TcpServerChannelInitializer(
			final MessageInboundTransponder transponder, final TcpServerConnectionMgr connMgr) {
		this.transponder = transponder;
		this.connMgr = connMgr;
	}
	
	public void addTLS(final SslContext sslContext, final SSLCLientCertReq req, final String[] supportedTLSVerions) {
		this.sslContext = sslContext;
		this.req = req;
		this.tlsVersions = supportedTLSVerions;
	}

	@Override
	protected void initChannel(final SocketChannel ch) throws Exception {
		if(sslContext != null) {
			final SSLEngine engine = sslContext.newEngine(ch.alloc());
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
			if(tlsVersions != null && tlsVersions.length > 0) {
				engine.setEnabledProtocols(tlsVersions);
			}
			ch.pipeline().addFirst("ssl", new SslHandler(engine));//init the TLS since we are the client
		}
		ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4), new LengthFieldPrepender(4));
		ch.pipeline().addLast(new RawInboundClientHandler(), new RawOutboundClientHandler());
		ch.pipeline().addLast(connMgr);
		ch.pipeline().addLast(transponder);
	}
}
