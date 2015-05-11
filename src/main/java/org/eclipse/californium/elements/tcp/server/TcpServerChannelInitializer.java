package org.eclipse.californium.elements.tcp.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import org.eclipse.californium.elements.tcp.MessageInboundTransponder;
import org.eclipse.californium.elements.tcp.RawInboundClientHandler;
import org.eclipse.californium.elements.tcp.RawOutboundClientHandler;

public class TcpServerChannelInitializer extends ChannelInitializer<SocketChannel>{
	
private final MessageInboundTransponder transponder;
private final TcpServerConnectionMgr connMgr;

public TcpServerChannelInitializer(final MessageInboundTransponder transponder, final TcpServerConnectionMgr connMgr) {
	this.transponder = transponder;
	this.connMgr = connMgr;
}

@Override
protected void initChannel(final SocketChannel ch) throws Exception {
	ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4), new LengthFieldPrepender(4));
	ch.pipeline().addLast(new RawInboundClientHandler(), new RawOutboundClientHandler());
	ch.pipeline().addLast(connMgr);
	ch.pipeline().addLast(transponder);
}
}
