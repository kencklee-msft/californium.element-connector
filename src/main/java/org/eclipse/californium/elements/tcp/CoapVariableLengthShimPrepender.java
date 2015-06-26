package org.eclipse.californium.elements.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class CoapVariableLengthShimPrepender extends MessageToMessageEncoder<ByteBuf>{

	private static final byte NO_CODE_MAX_LENGTH = 0x17;
	private static final byte UINT_8_BIT_LENGTH_CODE = 0x18;
	private static final byte UINT_16_BIT_LENGTH_CODE = 0x019;
	private static final byte UINT_32_BIT_LENGTH_CODE = 0x1A;

	private final int MAX_FRAME_SIZE;

	public CoapVariableLengthShimPrepender(final int maxFrameSize) {
		MAX_FRAME_SIZE = maxFrameSize;
	}

	@Override
	protected void encode(final ChannelHandlerContext ctx, final ByteBuf msg, final List<Object> out) throws Exception {
		final int length = msg.readableBytes();

		if (length < 0) {
			throw new IllegalArgumentException("Frame length (" + length + ") is less than zero");
		}
		else if(length > MAX_FRAME_SIZE) {
			throw new IllegalArgumentException("Frame length (" + length + ") is more than the configured Max (" + MAX_FRAME_SIZE + ")");
		}
		if(length <= NO_CODE_MAX_LENGTH) {
			out.add(ctx.alloc().buffer(1).writeByte(length));
		} else if( length <= 255) {
			final byte[] lengthField = getByteLength(length);
			out.add(ctx.alloc().buffer(lengthField.length).writeBytes(lengthField));
		} else if(length <= 65535) {
			final byte[] lengthField = getShortLength(length);
			out.add(ctx.alloc().buffer(lengthField.length).writeBytes(lengthField));
		}else if(length <= Integer.MAX_VALUE) {
			final byte[] lengthField = getIntlength(length);
			out.add(ctx.alloc().buffer(lengthField.length).writeBytes(lengthField));
		}else {
			throw new IllegalArgumentException("Frame length (" + length + ") is larger than the allowed max (" + Integer.MAX_VALUE + ")");
		}
	}

	private byte[] getIntlength(final int value) {
		return new byte[] {
				UINT_32_BIT_LENGTH_CODE,
				(byte) (value >> 24),
				(byte) (value >> 16),
				(byte) (value >> 8),
				(byte) value};
	}

	private byte[] getShortLength(final int value) {
		return new byte[] {
				UINT_16_BIT_LENGTH_CODE,
				(byte) (value >> 8),
				(byte) value};
	}

	private byte[] getByteLength(final int value) {
		return new byte[] {
				UINT_8_BIT_LENGTH_CODE,
				(byte)value};
	}
}
