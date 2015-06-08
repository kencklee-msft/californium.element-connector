package org.eclipse.californium.elements.tcp.server;

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

import org.eclipse.californium.elements.config.TCPConnectionConfig.SSLCLientCertReq;
import org.eclipse.californium.elements.tcp.MessageInboundTransponder;
import org.eclipse.californium.elements.tcp.RawInboundClientHandler;
import org.eclipse.californium.elements.tcp.RawOutboundClientHandler;

public class TcpServerChannelInitializer extends ChannelInitializer<SocketChannel> {
	private static final Logger LOG = Logger.getLogger( TcpServerChannelInitializer.class.getName() );

	private final MessageInboundTransponder transponder;
	private final TcpServerConnectionMgr connMgr;

	private SslHandler sslHandler;

	public TcpServerChannelInitializer(
			final MessageInboundTransponder transponder, final TcpServerConnectionMgr connMgr) {
		this.transponder = transponder;
		this.connMgr = connMgr;
	}
	
	public void addTLS(final SSLContext sslContext, final SSLCLientCertReq req, final String[] supportedTLSVerions) {
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
			System.out.println("printing cypher");
			for(final String c : engine.getEnabledCipherSuites()) {
				System.out.println(c);
			}
			for(final String p : engine.getSupportedProtocols()) {
				System.out.println(p);
			}
			if(supportedTLSVerions != null && supportedTLSVerions.length > 0) {
				engine.setEnabledProtocols(supportedTLSVerions);
			}
			this.sslHandler =  new SslHandler(engine);
			sslHandler.setHandshakeTimeoutMillis(5000);
			sslHandler.setCloseNotifyTimeoutMillis(2000);
		}
	}

	@Override
	protected void initChannel(final SocketChannel ch) throws Exception {
		if(sslHandler != null) {
			ch.pipeline().addFirst("ssl", sslHandler);//init the TLS since we are the client
		}
		ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4), new LengthFieldPrepender(4));
		ch.pipeline().addLast(new RawInboundClientHandler(), new RawOutboundClientHandler());
		ch.pipeline().addLast(connMgr);
		ch.pipeline().addLast(transponder);
	}
	
	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		asychNotifyOnCompleteHandshake(sslHandler.handshakeFuture());
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
