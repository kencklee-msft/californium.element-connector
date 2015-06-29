package org.eclipse.californium.elements.tcp.framing;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.TooLongFrameException;

import java.util.List;

/**
 * The 'Message Length' field is a 8-bit unsigned integer in network
 * byte order.  Length sematics follow the same mechanisme as "Major
 * type 0" from CBOR specification ([RFC7049]) The length field is
 * dicated by the 5 less significant bit of the byte.  Values are used
 * as such:
 *
 * o  between 0b000_00001 and 0b000_10111 (1 to 23) dictates the actual
 *    lenght of the following message
 *
 * o  0b000_11000 (24) means an additional 8-bit unsigned Integer is
 *    appended to the length field dictating the total length
 *
 * o  0b000_11001 (25) means an additional 16-bit unsigned Integer is
 *    appended to the length field dictating the total length
 *
 * o  0b000_11010 (26) means an additional 32-bit unsigned Integer is
 *    appended to the length field dictating the total length
 *
 * The 3 most significante bit in the initial length field are reserved
 * for future use.  If a recipient get a message larger than it can
 * handle, it SHOULD if possible send back a 4.13 in accordance with
 * [RFC7252] section on error code
 * 
 *
 */
public class CoapVariableLengthShimDecoder extends ByteToMessageDecoder{

	private static final int NO_CODE_MAX_LENGTH = 0x17;
	private static final int UINT_8_BIT_LENGTH_CODE = 0x18;
	private static final int UINT_16_BIT_LENGTH_CODE = 0x019;
	private static final int UINT_32_BIT_LENGTH_CODE = 0x1A;
	private static final int RESERVED_LENGTH_INDICATOR = 0xE0;

	private final int MAX_FRAME_SIZE;

	public CoapVariableLengthShimDecoder() {
		this(Integer.MAX_VALUE);
	}


	public CoapVariableLengthShimDecoder(final int maxValue) {
		MAX_FRAME_SIZE = maxValue;
	}


	@Override
	protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws Exception {
		int size = 0;
		int lengthFieldSize = 1;
		final byte sizeIdicator = in.getByte(0);
		switch (getLengthCode(sizeIdicator)) {
		case NO_CODE_MAX_LENGTH:
			size = getMinimalSizeLenght(sizeIdicator);
			break;
		case UINT_8_BIT_LENGTH_CODE:
			size = getExpandedSize(1, in);
			lengthFieldSize += 1;
			break;
		case UINT_16_BIT_LENGTH_CODE:
			size = getExpandedSize(2, in);
			lengthFieldSize += 2;
			break;
		case UINT_32_BIT_LENGTH_CODE:
			size = getExpandedSize(4, in);
			lengthFieldSize += 4;
			break;
		case RESERVED_LENGTH_INDICATOR:
		default:
			throw new CorruptedFrameException("Length of the Frame is Invalid");
		}

		if(size < MAX_FRAME_SIZE) {		
			final Object decoded = decode(ctx, in, size, lengthFieldSize);
			if(decoded != null) {
				out.add(decoded);
			}
		} else {
			throw new TooLongFrameException("Frame is larger than the max supported frame size of " + MAX_FRAME_SIZE);
		}
	}

	/**
	 * decode the rest of the length
	 * @param ctx
	 * @param in
	 * @param frameSize
	 * @param lengthFieldAdjust
	 * @return
	 */
	private Object decode(final ChannelHandlerContext ctx, final ByteBuf in, final  int frameSize, final int lengthFieldAdjust) {
		if(in.readableBytes() < frameSize + lengthFieldAdjust) {
			//message not fully in yet
			return null;
		}
		//skip the length bytes
		final int readerIndex = in.skipBytes(lengthFieldAdjust).readerIndex();
		final ByteBuf frame = extractFrame(ctx, in, readerIndex, frameSize);
		in.readerIndex(readerIndex + frameSize);

		return frame;

	}

	/**
	 * get the frame for the specified length 
	 * @param ctx
	 * @param buffer
	 * @param index
	 * @param length
	 * @return
	 */
	protected ByteBuf extractFrame(final ChannelHandlerContext ctx, final ByteBuf buffer, final int index, final int length) {
		final ByteBuf frame = ctx.alloc().buffer(length);
		frame.writeBytes(buffer, index, length);
		return frame;
	}

	/**
	 * get the size from the following bytes
	 * @param i - the number of byte to read in order to get the message length 
	 * @param in - the byte buffer containing the message
	 * @return the total size
	 */
	private int getExpandedSize(final int numOfByte, final ByteBuf in) {
		final byte[] size = new byte[numOfByte];
		in.getBytes(1, size);//skip the first byte
		return getIntFromByteArray(size);
	}

	/**
	 * get int value from a byte array
	 * @param size
	 * @return
	 */
	private int getIntFromByteArray(final byte[] size) {
		int shift = (size.length * 8) - 8;//know the amount of shift we need to to, max shift is 24 (0b00000000 00000000 00000000 00000000)
		int sizeValue = 0;
		for(final byte value : size) {
			sizeValue = sizeValue | (value & 0xFF) << shift;
			shift -= 8;
		}
		return sizeValue;
	}

	/**
	 * leave like this for now, mask is not that useful
	 * we need to assess if it is performant enough to let autoboxing deal with it.
	 * 
	 * @param sizeIdicator
	 * @return
	 */
	private int getMinimalSizeLenght(final byte sizeIdicator) {
		return sizeIdicator & 0xFF;
	}

	/**
	 * get the Length code from the first byte
	 * @param in
	 * @return
	 */
	private int getLengthCode(final byte in) {

		//specific size
		if(in == UINT_8_BIT_LENGTH_CODE) {
			return UINT_8_BIT_LENGTH_CODE;
		} else if(in == UINT_16_BIT_LENGTH_CODE) {
			return UINT_16_BIT_LENGTH_CODE;
		}else if(in == UINT_32_BIT_LENGTH_CODE) {
			return UINT_32_BIT_LENGTH_CODE;
		}

		//dynamic minimal size (evalute mask at 5 least sig bit)
		final byte dynSize = (byte) (in >>> 3);
		if((dynSize & (byte)0xFF) == 0x0 ||
				(dynSize & (byte)0xFF) == 0x1 ||
				(dynSize & (byte)0xFF) == 0x2 ) {
			return NO_CODE_MAX_LENGTH;
		}
		//error: using reserved bits (evaluate the 3 reserved byte)
		else if(((in >>> 5) & (byte)0xFF) != 0x0) {
			return RESERVED_LENGTH_INDICATOR;
		}
		//error: invalid length
		else {
			return RESERVED_LENGTH_INDICATOR;
		}
	}
}
