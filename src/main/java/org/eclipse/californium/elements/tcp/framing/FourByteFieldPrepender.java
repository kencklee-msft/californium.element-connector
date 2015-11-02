package org.eclipse.californium.elements.tcp.framing;

import io.netty.handler.codec.LengthFieldPrepender;

public class FourByteFieldPrepender extends LengthFieldPrepender{

	private static final int PREPENDED_BYTE_FIELD_LENGTH = 4;

	public FourByteFieldPrepender() {
		super(PREPENDED_BYTE_FIELD_LENGTH);
	}

}
