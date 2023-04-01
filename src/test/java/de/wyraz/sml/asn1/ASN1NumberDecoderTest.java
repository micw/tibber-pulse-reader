package de.wyraz.sml.asn1;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;

import org.junit.Test;

public class ASN1NumberDecoderTest {
	
	protected static Number decodeUnsigned(int... byteArray) {
		
		byte[] bytes=new byte[byteArray.length];
		for (int i=0;i<byteArray.length;i++) {
			bytes[i]=(byte) (byteArray[i] & 0xFF);
		}
		
		return ASN1BERTokenizer.decodeUnsigned(bytes, 0, bytes.length);
	}
	protected static Number decodeSigned(int... byteArray) {
		
		byte[] bytes=new byte[byteArray.length];
		for (int i=0;i<byteArray.length;i++) {
			bytes[i]=(byte) (byteArray[i] & 0xFF);
		}
		
		return ASN1BERTokenizer.decodeSigned(bytes, 0, bytes.length);
	}

	@Test
	public void testParseSigned8() {
		assertThat(decodeSigned(0x01))
			.hasToString("1")
			.isInstanceOf(Byte.class)
			;
		assertThat(decodeSigned(0xFF))
			.hasToString("-1")
			.isInstanceOf(Byte.class)
			;
		assertThat(decodeSigned(0x7F))
			.hasToString("127")
			.isInstanceOf(Byte.class)
			;
		assertThat(decodeSigned(0x80))
			.hasToString("-128")
			.isInstanceOf(Byte.class)
			;
	}

	@Test
	public void testParseSigned16() {
		assertThat(decodeSigned(0x00,0x01))
			.hasToString("1")
			.isInstanceOf(Integer.class)
			;
		assertThat(decodeSigned(0xFF,0xFF))
			.hasToString("-1")
			.isInstanceOf(Integer.class)
			;
		assertThat(decodeSigned(0x7F,0xFF))
			.hasToString("32767")
			.isInstanceOf(Integer.class)
			;
		assertThat(decodeSigned(0x80,0x00))
			.hasToString("-32768")
			.isInstanceOf(Integer.class)
			;
	}
	
	@Test
	public void testParseUnsigned8() {
		assertThat(decodeUnsigned(0x01))
			.hasToString("1")
			.isInstanceOf(Integer.class)
			;
		assertThat(decodeUnsigned(0xFF))
			.hasToString("255")
			.isInstanceOf(Integer.class)
			;
	}

	@Test
	public void testParseUnsigned16() {
		assertThat(decodeUnsigned(0x00,0x01))
			.hasToString("1")
			.isInstanceOf(Integer.class)
			;
		assertThat(decodeUnsigned(0xFF,0xFF))
			.hasToString(new BigInteger("00FFFF",16).toString())
			.isInstanceOf(Integer.class)
			;
	}

	@Test
	public void testParseUnsigned24() {
		assertThat(decodeUnsigned(0x00,0x00,0x01))
			.hasToString("1")
			.isInstanceOf(Integer.class)
			;
		assertThat(decodeUnsigned(0xFF,0xFF,0xFF))
			.hasToString(new BigInteger("00FFFFFF",16).toString())
			.isInstanceOf(Integer.class)
			;
	}

	@Test
	public void testParseUnsigned32() {
		assertThat(decodeUnsigned(0x00,0x00,0x00,0x01))
			.hasToString("1")
			.isInstanceOf(Long.class)
			;
		assertThat(decodeUnsigned(0xFF,0xFF,0xFF,0xFF))
			.hasToString(new BigInteger("00FFFFFFFF",16).toString())
			.isInstanceOf(Long.class)
			;
	}

	@Test
	public void testParseUnsigned40() {
		assertThat(decodeUnsigned(0x00,0x00,0x00,0x00,0x01))
			.hasToString("1")
			.isInstanceOf(Long.class)
			;
		assertThat(decodeUnsigned(0xFF,0xFF,0xFF,0xFF,0xFF))
			.hasToString(new BigInteger("00FFFFFFFFFF",16).toString())
			.isInstanceOf(Long.class)
			;
	}

	@Test
	public void testParseUnsigned48() {
		assertThat(decodeUnsigned(0x00,0x00,0x00,0x00,0x00,0x01))
			.hasToString("1")
			.isInstanceOf(Long.class)
			;
		assertThat(decodeUnsigned(0xFF,0xFF,0xFF,0xFF,0xFF,0xFF))
			.hasToString(new BigInteger("00FFFFFFFFFFFF",16).toString())
			.isInstanceOf(Long.class)
			;
	}

	@Test
	public void testParseUnsigned56() {
		assertThat(decodeUnsigned(0x00,0x00,0x00,0x00,0x00,0x00,0x01))
			.hasToString("1")
			.isInstanceOf(Long.class)
			;
		assertThat(decodeUnsigned(0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF))
			.hasToString(new BigInteger("00FFFFFFFFFFFFFF",16).toString())
			.isInstanceOf(Long.class)
			;
	}

	@Test
	public void testParseUnsigned64() {
		assertThat(decodeUnsigned(0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x01))
			.hasToString("1")
			.isInstanceOf(BigInteger.class)
			;
		assertThat(decodeUnsigned(0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF))
			.hasToString(new BigInteger("00FFFFFFFFFFFFFFFF",16).toString())
			.isInstanceOf(BigInteger.class)
			;
	}

	@Test
	public void testParseUnsigned72() {
		assertThat(decodeUnsigned(0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x01))
			.hasToString("1")
			.isInstanceOf(BigInteger.class)
			;
		assertThat(decodeUnsigned(0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF))
			.hasToString(new BigInteger("00FFFFFFFFFFFFFFFFFF",16).toString())
			.isInstanceOf(BigInteger.class)
			;
	}
	
}
