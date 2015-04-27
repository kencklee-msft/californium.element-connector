package org.eclipse.californium.elements.tcp;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import org.eclipse.californium.elements.tcp.client.MessageInboundTransponder;

public class TcpChannelInitializer extends ChannelInitializer<SocketChannel>{
		
	private final MessageInboundTransponder transponder;
	
	public TcpChannelInitializer(final MessageInboundTransponder transponder) {
		this.transponder = transponder;
	}

	@Override
	protected void initChannel(final SocketChannel ch) throws Exception {
		ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4), new LengthFieldPrepender(4));
		ch.pipeline().addLast(new RawInboundClientHandler(), new RawOutboundClientHandler());
		ch.pipeline().addLast(transponder);
	}
}
