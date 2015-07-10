package org.eclipse.californium.elements.tcp.framing;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class FourByteFrameDecoder extends LengthFieldBasedFrameDecoder{
	
	private static final int MAX_FRAME_LENGTH = Integer.MAX_VALUE;
	private static final int FIELD_OFFSET = 0;
	private static final int FIELD_LENGTH = 4;
	private static final int LENGTH_ADJUSTMENT = 0;
	private static final int INITIAL_BYTE_TO_STRIP = 4;

	public FourByteFrameDecoder() {
		super(MAX_FRAME_LENGTH, 
			  FIELD_OFFSET, 
			  FIELD_LENGTH, 
			  LENGTH_ADJUSTMENT, 
			  INITIAL_BYTE_TO_STRIP);
	}

}
