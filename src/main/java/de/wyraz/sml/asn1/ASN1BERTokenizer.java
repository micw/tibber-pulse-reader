package de.wyraz.sml.asn1;

import java.io.PrintStream;
import java.math.BigInteger;

import org.bouncycastle.util.Arrays;

import de.wyraz.tibberpulse.util.ByteUtil;

/**
 * parses a BER formated message into ASN.1 tokens
 * 
 * License: AGPLv3
 * 
 * @author mwyraz
 *
 */
public class ASN1BERTokenizer {
	
	public enum Type {
		BEGIN_OF_FILE,
		END_OF_FILE,
		END_OF_MESSAGE,
		UNKNOWN {
			@Override
			public String describe(byte typeValue, int dataLength, Object object) {
				return name()+"(0x"+ByteUtil.toBits(typeValue)+"?, length="+dataLength+")";			
			}
		},
		NULL,
		LIST {
			@Override
			public String describe(byte typeValue, int dataLength, Object object) {
				return name()+" length="+dataLength;
			}
		},
		OCTET_STRING {
			@Override
			public String describe(byte typeValue, int dataLength, Object object) {
				return name()+" length="+dataLength+": 0x"+ByteUtil.toHex((byte[])object);
			}
		},
		UNSIGNED {
			@Override
			public String describe(byte typeValue, int dataLength, Object object) {
				return name()+"_"+(8*dataLength)+": "+object;
			}
		},
		SIGNED {
			@Override
			public String describe(byte typeValue, int dataLength, Object object) {
				return name()+"_"+(8*dataLength)+": "+object;
			}
		},
		;
		
		public String describe(byte typeValue, int dataLength, Object object) {
			return name();
		}
	}
	
	protected final byte[] message;
	protected int offset;
	
	protected Type type = Type.BEGIN_OF_FILE;
	protected byte typeValue = 0;
	protected int dataLength;
	protected Object object = null;
	
	public ASN1BERTokenizer(byte[] message) {
		this.message = message;
		this.offset = 0;
	}
	
	public Type getType() {
		return type;
	}
	public int getDataLength() {
		return dataLength;
	}
	public Object getObject() {
		return object;
	}
	
	public static Number decodeUnsigned(byte[] data, int offset, int length) {
		
		if (length==1) {
			return data[offset] & 0xFF; 
		}

		if (length==2) {
			return
				(data[offset++] & 0xFF) << 8 | 
				data[offset++] & 0xFF;
		}

		if (length==3) {
			return
				(data[offset++] & 0xFF) << 16 | 
				(data[offset++] & 0xFF) << 8 | 
				data[offset++] & 0xFF;
		}

		if (length==4) {
			return
				((long) data[offset++] & 0xFF) << 24 | 
				(data[offset++] & 0xFF) << 16 | 
				(data[offset++] & 0xFF) << 8 | 
				data[offset++] & 0xFF;
		}

		if (length==5) {
			return
				((long) data[offset++] & 0xFF) << 32 | 
				((long) data[offset++] & 0xFF) << 24 | 
				(data[offset++] & 0xFF) << 16 | 
				(data[offset++] & 0xFF) << 8 | 
				data[offset++] & 0xFF;
		}

		if (length==6) {
			return
				((long) data[offset++] & 0xFF) << 40 | 
				((long) data[offset++] & 0xFF) << 32 | 
				((long) data[offset++] & 0xFF) << 24 | 
				(data[offset++] & 0xFF) << 16 | 
				(data[offset++] & 0xFF) << 8 | 
				data[offset++] & 0xFF;
		}

		if (length==7) {
			return
				((long) data[offset++] & 0xFF) << 48 | 
				((long) data[offset++] & 0xFF) << 40 | 
				((long) data[offset++] & 0xFF) << 32 | 
				((long) data[offset++] & 0xFF) << 24 | 
				(data[offset++] & 0xFF) << 16 | 
				(data[offset++] & 0xFF) << 8 | 
				data[offset++] & 0xFF;
		}
		
		if ((data[offset] & 0x80) != 0) { // most significant bit set -> negative number
			// add a "00" byte so that BigInteger sees a larger positive number
			byte[] bytes=new byte[length + 1];
			bytes[0] = 0;
			System.arraycopy(data, offset, bytes, 1, length);
			return new BigInteger(bytes);
		}
		
		return new BigInteger(data, offset, length);
	}

	public static Number decodeSigned(byte[] data, int offset, int length) {
		
		if (length==1) {
			return data[offset]; 
		}
		
		if (length>1 && length<=4) {
			return new BigInteger(data, offset, length).intValue();
		}
		
		if (length>4 && length<=8) {
			return new BigInteger(data, offset, length).longValue();
		}
		
		
		return new BigInteger(data, offset, length);
	}
	
	public boolean hasMoreData() {
		return offset<message.length;
	}
	
	public Integer readListOfElements(int expectedSize, boolean optional) {
		readNext(Type.LIST, expectedSize, optional);
		if (type==Type.NULL) {
			return null;
		}
		return dataLength;
	}

	public void readEndOfMessage(boolean optional) {
		readNext(Type.END_OF_MESSAGE, 0, optional);
	}
	
	public byte[] readOctetString(boolean optional) {
		return (byte[]) readNext(Type.OCTET_STRING,-1,optional);
	}

	public Integer readUnsigned8(boolean optional) {
		return (Integer) readNext(Type.UNSIGNED,1,optional);
	}
	public Integer readUnsigned16(boolean optional) {
		return (Integer) readNext(Type.UNSIGNED,2,optional);
	}
	public Long readUnsigned32(boolean optional) {
		return (Long) readNext(Type.UNSIGNED,4,optional);
	}

	public Byte readSigned8(boolean optional) {
		return (Byte) readNext(Type.SIGNED,1,optional);
	}
	
	public byte[] getMessage() {
		return message;
	}
	
	public int getOffset() {
		return offset;
	}
	
	public Object readNext(Type expectedType, int expectedSize, boolean optional) {
		readNext();
		return expect(expectedType, expectedSize, optional);
	}
	
	public Object expect(Type expectedType, int expectedSize, boolean optional) {
		if (optional && type==Type.NULL) {
			return null;
		}
		if (type!=expectedType) {
			throw new RuntimeException("Expected "+expectedType+" but found "+type.describe(typeValue, dataLength, object));
		}
		if (expectedSize>-1 && expectedSize!=dataLength) {
			throw new RuntimeException("Expected "+expectedType+" of length "+expectedSize+" but found "+type.describe(typeValue, dataLength, object));
		}
		return object;
	}
	
	
	
	public Type readNext() {
		
		this.typeValue = 0;
		this.type = Type.UNKNOWN;
		this.dataLength = 0;
		this.object = null;
		
		if (!hasMoreData()) {
			if (this.type == Type.END_OF_FILE) {
				throw new RuntimeException("Read after END_OF_FILE");
			}
			this.type = Type.END_OF_FILE;
			return this.type;
		}
		
		byte tlField = message[offset++];
		this.typeValue =(byte) (tlField & 0b01110000);
		
		if (tlField == 0x0) {
			this.type = Type.END_OF_MESSAGE;
			return this.type;
		}
		
		int tlLength=1;
		int tlAndDataLength=(tlField & 0b1111);
		while ((tlField & 0b10000000)!=0) {
			tlField = message[offset++];
			tlAndDataLength = (((tlAndDataLength & 0xffffffff ) << 4) | (tlField & 0b00001111));
			tlLength++;
		}
		
		this.dataLength=tlAndDataLength-tlLength;
		
		switch (this.typeValue) {
			case 0b01110000:
				this.type = Type.LIST;
				this.dataLength = tlAndDataLength; // since length is not in bytes, tlLength is not substracted
				break;
			case 0b01100000:
				this.type = Type.UNSIGNED;
				this.object = decodeUnsigned(message,offset,this.dataLength);
				offset+=this.dataLength;
				break;
			case 0b01010000:
				this.type = Type.SIGNED;
				this.object = decodeSigned(message,offset,this.dataLength);
				offset+=this.dataLength;
				break;
			case 0b00000000:
				if (this.dataLength==0) {
					this.type = Type.NULL;
					break;
				}
				this.type = Type.OCTET_STRING;
				// no "break", same 
			default:
				this.object = Arrays.copyOfRange(message, offset, offset+this.dataLength);
				offset+=this.dataLength;
				break;
		}
		
		return this.type;
	}

	public void dump(PrintStream out) {
		dump(out, 0, Integer.MAX_VALUE);
	}
	
	protected void dump(PrintStream out, int depth, int maxElementCount) {
		while ((maxElementCount--)>0 && readNext()!=Type.END_OF_FILE) {
			
			for (int i=0;i<depth;i++) {
				out.print("  ");
			}
			out.println(type.describe(typeValue, dataLength, object));
			if (type==Type.LIST) { // nested list
				dump(out,depth+1, dataLength);
			}
		}
	}
	
}
