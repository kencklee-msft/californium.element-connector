package org.eclipse.californium.elements.tcp.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;

import org.eclipse.californium.elements.tcp.MessageInboundTransponder;
import org.eclipse.californium.elements.tcp.RawInboundClientHandler;
import org.eclipse.californium.elements.tcp.RawOutboundClientHandler;

public class TcpClientChannelInitializer extends ChannelInitializer<SocketChannel>{
		
	private final MessageInboundTransponder transponder;
	private SslContext sslContext;
	
	public TcpClientChannelInitializer(final MessageInboundTransponder transponder) {
		this.transponder = transponder;
	}
	
	public void addTLS(final SslContext sslContext) {
		this.sslContext = sslContext;
	}

	@Override
	protected void initChannel(final SocketChannel ch) throws Exception {
		if(sslContext != null) {
			final SSLEngine engine = sslContext.newEngine(ch.alloc());
			ch.pipeline().addFirst("ssl", new SslHandler(engine));//init the TLS since we are the client
		}
		ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4), new LengthFieldPrepender(4));
		ch.pipeline().addLast(new RawInboundClientHandler(), new RawOutboundClientHandler());
		ch.pipeline().addLast(transponder);
	}
}
