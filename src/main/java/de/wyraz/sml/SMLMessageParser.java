package de.wyraz.sml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openmuc.jsml.EUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.wyraz.sml.SMLTime.SMLTimeSecIndex;
import de.wyraz.sml.asn1.ASN1BERTokenizer;
import de.wyraz.sml.asn1.ASN1BERTokenizer.Type;
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

	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	public static Collection<SMLMessage> parse(byte[] payload) {
		return new SMLMessageParser(payload).parseSMLMessages();
	}

	protected final ASN1BERTokenizer tokenizer;
	
	protected SMLMessageParser(byte[] payload) {
		tokenizer=new ASN1BERTokenizer(payload);
	}

	protected Collection<SMLMessage> parseSMLMessages() {

		List<SMLMessage> results=new ArrayList<>();

		while (tokenizer.hasMoreData()) {
			
			tokenizer.readListOfElements(6, false);
	
			int crcStartOffset=tokenizer.getOffset();
			
			SMLMessage result = new SMLMessage();
			result.transactionId = tokenizer.readOctetString(false);
			result.groupNo = tokenizer.readUnsigned8(false);
			result.abortOnError = tokenizer.readUnsigned8(false);
			result.messageBody = parseSMLMessageBody();
			result.crc16Actual = CRC16.getCrc16(tokenizer.getMessage(), crcStartOffset, tokenizer.getOffset()-1);
			result.crc16Expected = tokenizer.readUnsigned16(false);
			result.crc16Ok = result.crc16Actual.equals(result.crc16Expected);
			
			tokenizer.readEndOfMessage(false);
			
			results.add(result);
		}
		
		return results;
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
		tokenizer.readListOfElements(6, false);

		SMLPublicOpenResponse result = new SMLPublicOpenResponse();
		result.codepage = tokenizer.readOctetString(true);
		result.clientId = tokenizer.readOctetString(true);
		result.reqFileId = tokenizer.readOctetString(true);
		result.serverId = tokenizer.readOctetString(true);
		result.refTime = parseSMLTime(true);
		result.smlVersion = tokenizer.readUnsigned8(true);

		return result;
	}

	protected SMLPublicCloseResponse parseSMLPublicCloseResponse() {
		tokenizer.readListOfElements(1, false);

		SMLPublicCloseResponse result = new SMLPublicCloseResponse();
		result.globalSignature = tokenizer.readOctetString(true);

		return result;
	}
	
	protected SMLGetListResponse parseSMLGetListResponse() {
		
		tokenizer.readListOfElements(7, false);

		SMLGetListResponse result = new SMLGetListResponse();
		result.clientId = tokenizer.readOctetString(true);
		result.serverId = tokenizer.readOctetString(true);
		result.listName = tokenizer.readOctetString(true);
		result.actSensorTime = parseSMLTime(true);
		
		int elementCount=tokenizer.readListOfElements(-1, false);
		result.valList = parseSMLList(elementCount);
		
		result.listSignature = tokenizer.readOctetString(true);
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
		tokenizer.readListOfElements(7, false);

		SMLListEntry result=new SMLListEntry();
		
		result.objName = tokenizer.readOctetString(false);
		{
			Number status=(Number) tokenizer.readNext(Type.UNSIGNED,-1,true);
			result.status = status==null?null:status.longValue();
		}
		result.valTime = parseSMLTime(true);
		Integer unit=tokenizer.readUnsigned8(true);
		result.valUnit = unit==null?null:EUnit.from(unit);
		result.scaler = tokenizer.readSigned8(true);
		result.value = parseSMLValue(false);
		result.valueSignature = tokenizer.readOctetString(true);
		
		return result;
	}
	
	protected Object parseSMLValue(boolean optional) {
		switch (tokenizer.readNext()) {
			case NULL:
			case OCTET_STRING:
			case SIGNED:
			case UNSIGNED:				
				return tokenizer.getObject();
			default:
				throw new RuntimeException("Unsuported type for SML entry value: "+tokenizer.getType());
		}
			
	}
	

	protected SMLTime parseSMLTime(boolean optional) {
		
		Type type=tokenizer.readNext();
		
		if (optional && tokenizer.getType()==Type.NULL) {
			return null;
		}
		
		if (type==Type.UNSIGNED && tokenizer.getDataLength()==4) {
			// spec requires a unsigned8 choice here but HYD uses unsigned32 (as value, not as choice) here
			// we just skip it and try to figure out what they mean with this
			return null;
		}
		
		// Expect a choice (list of 2 elements), maybe optional
		tokenizer.expect(Type.LIST, 2, false);
		
		Integer choice = tokenizer.readUnsigned8(false);
		if (choice == null) {
			return null;
		}
		
		if (choice==0x01) {
			SMLTimeSecIndex time=new SMLTimeSecIndex();
			time.secIndex=tokenizer.readUnsigned32(false);
			return time;
		}

		throw new RuntimeException("Unimplemented SML time: " + ByteUtil.int32ToHex(choice));
	}
	
	protected long readChoice32() {
		tokenizer.readListOfElements(2, false);
		
		Number value=(Number) tokenizer.readNext(Type.UNSIGNED, -1, false);
		
		return value.longValue();
	}

}
