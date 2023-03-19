package de.wyraz.sml;

import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.openmuc.jsml.EUnit;

import de.wyraz.sml.SMLTime.SMLTimeSecIndex;
import de.wyraz.tibberpulse.util.ByteUtil;

/**
 * SML message parser, based on
 * https://www.bsi.bund.de/SharedDocs/Downloads/DE/BSI/Publikationen/TechnischeRichtlinien/TR03109/TR-03109-1_Anlage_Feinspezifikation_Drahtgebundene_LMN-Schnittstelle_Teilb.pdf?__blob=publicationFile&v=1
 * 
 * References in comments are to the document above.
 * 
 * License: AGPLv3
 * 
 * @author mwyraz
 */
public class SMLMessageParser {

	protected final byte[] payload;
	protected int offset;

	public static Collection<SMLMessage> parse(byte[] payload) {
		return new SMLMessageParser(payload).parseSMLMessages();
	}

	public static void dumpRaw(byte[] payload, PrintStream out) {
		new SMLMessageParser(payload).dumpRaw(out);
	}

	protected SMLMessageParser(byte[] payload) {
		this.payload = payload;
		this.offset = 0;
	}

	protected Collection<SMLMessage> parseSMLMessages() {

		List<SMLMessage> results=new ArrayList<>();
		

		while (hasMoreData()) {
			int crcStart = offset;
			expectListOfElements(6); // (22) SML message is always a sequence of 6 elements
	
			SMLMessage result = new SMLMessage();
			result.transactionId = readOctetString(false);
			result.groupNo = readUnsigned8(false);
			result.abortOnError = readUnsigned8(false);
			result.messageBody = parseSMLMessageBody();
			result.crc16Actual = calculateCrc16(crcStart, offset-1);
			result.crc16Expected = readUnsigned16();
			result.crc16Ok = result.crc16Actual.equals(result.crc16Expected);
			
			expectEndOfSMLMessage();
			
			results.add(result);
		}
		
		return results;
	}

	protected boolean hasMoreData() {
		return offset < payload.length-1;
	}
	
	protected int calculateCrc16(int startOffset, int endOffset) {
		return CRC16.getCrc16(payload, startOffset, endOffset);
	}

	protected SMLMessageBody parseSMLMessageBody() {
		long choice = readChoice32();

		if (choice == 0x00000101) {
			return parseSMLPublicOpenResponse();
		}
		if (choice == 0x00000201) {
			return parseSMLPublicCloseResponse();
		}
		if (choice == 0x00000701) {
			return parseSMLGetListResponse();
		}

		throw new RuntimeException("Unimplemented SML message body: " + ByteUtil.int32ToHex(choice));
	}

	protected SMLPublicOpenResponse parseSMLPublicOpenResponse() {
		expectListOfElements(6);

		SMLPublicOpenResponse result = new SMLPublicOpenResponse();
		result.codepage = readOctetString(true);
		result.clientId = readOctetString(true);
		result.reqFileId = readOctetString(true);
		result.serverId = readOctetString(true);
		result.refTime = parseSMLTime(true);
		result.smlVersion = readUnsigned8(true);

		return result;
	}

	protected SMLPublicCloseResponse parseSMLPublicCloseResponse() {
		expectListOfElements(1);

		SMLPublicCloseResponse result = new SMLPublicCloseResponse();
		result.globalSignature = readOctetString(true);

		return result;
	}
	
	protected SMLGetListResponse parseSMLGetListResponse() {
		expectListOfElements(7);

		SMLGetListResponse result = new SMLGetListResponse();
		result.clientId = readOctetString(true);
		result.serverId = readOctetString(true);
		result.listName = readOctetString(true);
		result.actSensorTime = parseSMLTime(true);
		
		int elementCount=expectListOfElements(-1);
		result.valList = parseSMLList(elementCount);
		
		result.listSignature = readOctetString(true);
		result.actGatewayTime = parseSMLTime(true);
		
		return result;
		
	}

	protected List<SMLListEntry> parseSMLList(int elementCount) {
		List<SMLListEntry> result=new ArrayList<>(elementCount);
		for (int i=0;i<elementCount;i++) {
			result.add(parseSMLListEntry());
		}
		return result;
	}
	
	protected SMLListEntry parseSMLListEntry() {
		expectListOfElements(7);

		SMLListEntry result=new SMLListEntry();
		
		result.objName = readOctetString(false);
		result.status = parseSMLStatus(true);
		result.valTime = parseSMLTime(true);
		Integer unit=readUnsigned8(true);
		result.valUnit = unit==null?null:EUnit.from(unit);
		result.scaler = readSigned8(true);
		result.value = readSMLValue(false);
		result.valueSignature = readOctetString(true);
		
		return result;
		
	}
	

	protected SMLTime parseSMLTime(boolean optional) {
		
		byte tlField = payload[offset];
		if (isUnsigned32(tlField)) {
			// spec requires a unsigned8 choice here but HYD uses unsigned32 (as value, not as choice) here
			// we just skip it and try to figure out what they mean with this
			readUnsigned32();
			return null;
		}
		
		
		Integer choice = readChoice8(optional);
		if (choice == null) {
			return null;
		}
		
		if (choice==0x01) {
			SMLTimeSecIndex time=new SMLTimeSecIndex();
			time.secIndex=readUnsigned32();
			return time;
		}

		throw new RuntimeException("Unimplemented SML time: " + ByteUtil.int32ToHex(choice));
	}
	
	protected Long parseSMLStatus(boolean optional) {
		if (readOptionalNull(optional)) {
			return null;
		}
		byte tlField = payload[offset];
		if (isUnsigned8(tlField)) {
			return (long) readUnsigned8(false);
		}
		if (isUnsigned16(tlField)) {
			return (long) readUnsigned16();
		}
		if (isUnsigned24(tlField)) {
			return (long) readUnsigned24();
		}
		if (isUnsigned32(tlField)) {
			return (long) readUnsigned32();
		}
		
		throw unexpectedTlField(tlField, "unsigned8, unsigned16, unsigned24, unsigned32 or unsigned64");
	}
	

	protected void dumpRaw(PrintStream out) {
		dumpRaw(out, 0, Integer.MAX_VALUE);
	}

	protected boolean dumpRaw(PrintStream out, int indent, int returnAfter) {
		while (hasMoreData()) {
			if (returnAfter-- <= 0) {
				return true;
			}
			byte tlField = payload[offset];
			if (isEndOfSMLMessage(tlField)) {
				dumpIndent(out, indent, "end of SML message");
				offset++;
				continue;
			}
			if (isOptionalNull(tlField)) {
				dumpIndent(out, indent, "optional, null");
				offset++;
				continue;
			}
			if (isListOfElements(tlField)) {
				int elementCount = expectListOfElements(-1);
				dumpIndent(out, indent, "list of elements, length " + elementCount);
				if (!dumpRaw(out, indent + 1, elementCount)) {
					break;
				}
				continue;
			}
			if (isOctetString(tlField)) {
				dumpIndent(out, indent, "octet string, 0x" + ByteUtil.toHex(readOctetString(false)));
				continue;
			}
			if (isUnsigned8(tlField)) {
				dumpIndent(out, indent, "unsigned8, 0x" + ByteUtil.int8ToHex(readUnsigned8(false)));
				continue;
			}
			if (isUnsigned16(tlField)) {
				dumpIndent(out, indent, "unsigned16, 0x" + ByteUtil.int16ToHex(readUnsigned16()));
				continue;
			}
			if (isUnsigned24(tlField)) {
				dumpIndent(out, indent, "unsigned24, 0x" + ByteUtil.int24ToHex(readUnsigned24()));
				continue;
			}
			if (isUnsigned32(tlField)) {
				dumpIndent(out, indent, "unsigned32, 0x" + ByteUtil.int32ToHex(readUnsigned32()));
				continue;
			}
			if (isUnsigned64(tlField)) {
				dumpIndent(out, indent, "unsigned64, 0x" + ByteUtil.int64ToHex(readUnsigned64()));
				continue;
			}
			if (isSigned8(tlField)) {
				dumpIndent(out, indent, "signed8, 0x" + ByteUtil.int8ToHex(readSigned8(false)));
				continue;
			}
			if (isSigned16(tlField)) {
				dumpIndent(out, indent, "signed16, 0x" + ByteUtil.int8ToHex(readSigned16()));
				continue;
			}
			if (isSigned32(tlField)) {
				dumpIndent(out, indent, "signed32, 0x" + ByteUtil.int32ToHex(readSigned32()));
				continue;
			}
			if (isBoolean(tlField)) {
				dumpIndent(out, indent, "boolean, " + readBoolean());
				continue;
			}
			dumpIndent(out, indent, "unknown tl field: " + ByteUtil.toBits(tlField));
			dumpIndent(out, indent, "(stopped here)");
			break;
		}
		return false;
	}

	protected void dumpIndent(PrintStream out, int indent, String line) {
		for (int i = 0; i < indent; i++) {
			out.print("  ");
		}
		out.println(line);
	}

	protected String describeTlField(byte tlField) {
		if (isEndOfSMLMessage(tlField)) {
			return "end of SML message";
		}
		if (isOptionalNull(tlField)) {
			return "optional/null";
		}
		if (isListOfElements(tlField)) {
			return "list of elements";
		}
		if (isOctetString(tlField)) {
			return "octet string";
		}
		if (isUnsigned8(tlField)) {
			return "unsigned8";
		}
		if (isUnsigned16(tlField)) {
			return "unsigned16";
		}
		if (isUnsigned24(tlField)) {
			return "unsigned24";
		}
		if (isUnsigned32(tlField)) {
			return "unsigned32";
		}
		if (isSigned8(tlField)) {
			return "signed8";
		}
		if (isSigned16(tlField)) {
			return "signed16";
		}
		if (isSigned32(tlField)) {
			return "signed32";
		}
		if (isBoolean(tlField)) {
			return "boolean";
		}
		return "unknown tl field: " + ByteUtil.toBits(tlField);
	}

	protected Object readSMLValue(boolean optional) {
		if (readOptionalNull(optional)) {
			return null;
		}
		byte tlField = payload[offset];
		if (isBoolean(tlField)) {
			return readBoolean();
		}
		if (isOctetString(tlField)) {
			return readOctetString(false);
		}
		// TODO: missing singned/unsigned types
		if (isUnsigned8(tlField)) {
			return readUnsigned8(false);
		}
		if (isUnsigned16(tlField)) {
			return readUnsigned16();
		}
		if (isUnsigned24(tlField)) {
			return readUnsigned24();
		}
		if (isUnsigned32(tlField)) {
			return readUnsigned32();
		}
		if (isUnsigned64(tlField)) {
			return readUnsigned64();
		}
		if (isSigned8(tlField)) {
			return readSigned8(false);
		}
		if (isSigned16(tlField)) {
			return readSigned16();
		}
		if (isSigned32(tlField)) {
			return readSigned32();
		}
		// TODO: SML List
		throw unexpectedTlField(tlField, "a valid SML value");
	}
	
	protected RuntimeException unexpectedTlField(byte tlField, String expected) {
		throw new RuntimeException("Expected " + expected + " but got " + describeTlField(tlField) + " ("
				+ ByteUtil.toBits(tlField) + ")");
	}

	protected boolean isEndOfSMLMessage(byte tlField) {
		// 6.3.1
		return tlField == 0x00;
	}
	
	protected void expectEndOfSMLMessage() {
		byte tlField = payload[offset++];
		if (!isEndOfSMLMessage(tlField)) {
			throw unexpectedTlField(tlField, "end of SML message");
		}
	}
	
	
	protected boolean isOptionalNull(byte tlField) {
		// 6.3.4
		return tlField == 0x01;
	}

	protected boolean readOptionalNull(boolean allowOptional) {
		if (allowOptional && isOptionalNull(payload[offset])) {
			offset++;
			return true;
		}
		return false;
	}

	protected boolean isListOfElements(byte tlField) {
		// 6.2.5
		return (tlField & 0b01110000) == 0b01110000;
	}

	protected int expectListOfElements(int expectedLength) {
		byte tlField = payload[offset++];
		if (!isListOfElements(tlField)) {
			throw unexpectedTlField(tlField, "list of elements");
		}
		int length = tlField & 0b00001111;
		
		while ((tlField & 0b10000000) !=0) { // more length fields
			tlField = payload[offset++];
			
			if ((tlField & 0b01110000) == 0) { // use next 4 bits to extend length
				length = (length << 4 ) | (tlField & 0b0111111);
			} else {
				throw new RuntimeException("Unimplemented bit-shift for list length");
			}
		}
		
		if (expectedLength > -1 && length != expectedLength) {
			throw new RuntimeException(
					"Expected list of " + expectedLength + " elements but got list of " + length + " elements");
		}
		return length;
	}

	protected boolean isOctetString(byte tlField) {
		// 6.2.1
		return (tlField & 0b11110000) == 0b00000000;
	}

	protected byte[] readOctetString(boolean optional) {
		if (readOptionalNull(optional)) {
			return null;
		}
		byte tlField = payload[offset++];
		if (!isOctetString(tlField)) {
			throw unexpectedTlField(tlField, "octet string");
		}
		int length = (tlField & 0b00001111) - 1; // length includes TL
		byte[] bytes = Arrays.copyOfRange(payload, offset, offset + length);
		offset += length;
		return bytes;
	}

	protected boolean isUnsigned8(byte tlField) {
		// 6.2.3
		return ((tlField & 0xFF) == 0x62);
	}

	protected Integer readUnsigned8(boolean optional) {
		if (readOptionalNull(optional)) {
			return null;
		}
		byte tlField = payload[offset++];
		if (!isUnsigned8(tlField)) {
			throw unexpectedTlField(tlField, "unsigned8");
		}
		return payload[offset++] & 0xFF;
	}

	protected boolean isUnsigned16(byte tlField) {
		// 6.2.3
		return ((tlField & 0xFF) == 0x63);
	}

	protected int readUnsigned16() {
		byte tlField = payload[offset++];
		if (!isUnsigned16(tlField)) {
			throw unexpectedTlField(tlField, "unsigned16");
		}
		return ((payload[offset++] & 0xFF) << 8) | (payload[offset++] & 0xFF);
	}

	protected boolean isUnsigned24(byte tlField) {
		// 6 -> unsigned, 4-> byte length (minus tl field) = 3 byte
		return (tlField == 0x64);
	}

	protected int readUnsigned24() {
		byte tlField = payload[offset++];
		if (!isUnsigned24(tlField)) {
			throw unexpectedTlField(tlField, "unsigned24");
		}
		return ((payload[offset++] & 0xFF) << 16)
				| ((payload[offset++] & 0xFF) << 8) | (payload[offset++] & 0xFF);
	}
	
	protected boolean isUnsigned32(byte tlField) {
		// 6.2.3
		return (tlField == 0x65);
	}

	protected long readUnsigned32() {
		byte tlField = payload[offset++];
		if (!isUnsigned32(tlField)) {
			throw unexpectedTlField(tlField, "unsigned32");
		}
		return
			((long) (payload[offset++] & 0xFF) << 24) |
			((payload[offset++] & 0xFF) << 16) |
			((payload[offset++] & 0xFF) << 8) |
			(payload[offset++] & 0xFF);
	}

	protected boolean isUnsigned64(byte tlField) {
		// 6.2.3
		return (tlField == 0x69);
	}

	protected long readUnsigned64() {
		byte tlField = payload[offset++];
		if (!isUnsigned64(tlField)) {
			throw unexpectedTlField(tlField, "unsigned64");
		}
		return
				((long) (payload[offset++] & 0xFF) << 56) |
				((long) (payload[offset++] & 0xFF) << 48) |
				((long) (payload[offset++] & 0xFF) << 40) |
				((long) (payload[offset++] & 0xFF) << 32) |
				((long) (payload[offset++] & 0xFF) << 24) |
				((payload[offset++] & 0xFF) << 16) |
				((payload[offset++] & 0xFF) << 8) |
				(payload[offset++] & 0xFF);
	}
	
	
	protected boolean isSigned8(byte tlField) {
		// 6.2.2
		return (tlField == 0x52);
	}

	protected Integer readSigned8(boolean optional) {
		if (readOptionalNull(optional)) {
			return null;
		}
		byte tlField = payload[offset++];
		if (!isSigned8(tlField)) {
			throw unexpectedTlField(tlField, "signed8");
		}
		return (int) payload[offset++];
	}
	
	protected boolean isSigned16(byte tlField) {
		// 6.2.3
		return tlField == 0x53;
	}

	protected int readSigned16() {
		byte tlField = payload[offset++];
		if (!isSigned16(tlField)) {
			throw unexpectedTlField(tlField, "signed16");
		}
		return ((payload[offset++] & 0xFF) << 8) | (payload[offset++] & 0xFF);
	}
	
	protected boolean isSigned32(byte tlField) {
		// 6.2.3
		return (tlField == 0x55);
	}

	protected long readSigned32() {
		byte tlField = payload[offset++];
		if (!isSigned32(tlField)) {
			throw unexpectedTlField(tlField, "signed32");
		}
		return
			((payload[offset++] & 0xFF) << 24) |
			((payload[offset++] & 0xFF) << 16) |
			((payload[offset++] & 0xFF) << 8) |
			(payload[offset++] & 0xFF);
	}

	protected boolean isBoolean(byte tlField) {
		// 6.2.4
		return (tlField == 0x42);
	}

	protected boolean readBoolean() {
		byte tlField = payload[offset++];
		if (!isBoolean(tlField)) {
			throw unexpectedTlField(tlField, "boolean");
		}
		return payload[offset++] != 0x00;
	}

	protected long readChoice32() {
		expectListOfElements(2);
		
		byte tlField = payload[offset];
		if (isUnsigned16(tlField)) {
			// Out of spec, but used by EFR electricity meter
			return readUnsigned16();
		}
		
		return readUnsigned32();
	}

	protected Integer readChoice8(boolean optional) {
		if (readOptionalNull(optional)) {
			return null;
		}
		expectListOfElements(2);
		return readUnsigned8(false);
	}

}
