package de.wyraz.tibberpulse.sml;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.ArrayList;

import org.apache.commons.codec.binary.Hex;
import org.openmuc.jsml.structures.ASNObject;
import org.openmuc.jsml.structures.Integer16;
import org.openmuc.jsml.structures.Integer32;
import org.openmuc.jsml.structures.Integer8;
import org.openmuc.jsml.structures.OctetString;
import org.openmuc.jsml.structures.SmlList;
import org.openmuc.jsml.structures.SmlListEntry;
import org.openmuc.jsml.structures.SmlMessage;
import org.openmuc.jsml.structures.Unsigned16;
import org.openmuc.jsml.structures.Unsigned32;
import org.openmuc.jsml.structures.Unsigned64;
import org.openmuc.jsml.structures.Unsigned8;
import org.openmuc.jsml.structures.responses.SmlGetListRes;
import org.openmuc.jsml.structures.responses.SmlPublicCloseRes;
import org.openmuc.jsml.structures.responses.SmlPublicOpenRes;
import org.openmuc.jsml.transport.MessageExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.wyraz.tibberpulse.sml.SMLMeterData.Reading;

/**
 * SML Spec: https://www.bsi.bund.de/SharedDocs/Downloads/DE/BSI/Publikationen/TechnischeRichtlinien/TR03109/TR-03109-1_Anlage_Feinspezifikation_Drahtgebundene_LMN-Schnittstelle_Teilb.pdf?__blob=publicationFile&v=1
 * 
 * @author mwyraz
 *
 */
public class SMLDecoder {
	
	protected static final Logger log = LoggerFactory.getLogger(SMLDecoder.class);

	public static SMLMeterData decode(byte[] smlPayload) throws IOException {
		return decode(smlPayload, true);
	}
	
	public static SMLMeterData decode(byte[] smlPayload, boolean failOnCorruptMessagePart) throws IOException {
		
		if (log.isDebugEnabled()) {
			log.debug("Parsing SML: {}",Hex.encodeHexString(smlPayload));
		}
		
		byte[] messagePayload=extractMessage(smlPayload);
		
		DataInputStream din=new DataInputStream(new ByteArrayInputStream(messagePayload));

		SMLMeterData result=new SMLMeterData();
		
		SmlMessage sml=new SmlMessage();
		while (din.available()>0) {
			boolean crcOk=sml.decodeAndCheck(din);
			if (!crcOk) {
				if (failOnCorruptMessagePart) {
					throw new IOException("SML message crc error");
				} else {
					log.info("SML message crc error - proceeding with incomplete or corrupt message.");
				}
			}
			decodeASNObject(result, sml.getMessageBody().getChoice());
			if (!crcOk) {
				break;
			}
		}
		
		return result;
	}
	
	protected static void decodeASNObject(SMLMeterData result, ASNObject asn) {
		
		if (asn==null) {
			return; // may happen on incomplete SML message
		}
		
		if (asn instanceof SmlPublicCloseRes) {
			// no usable data
			return;
		}
		
		if (asn instanceof SmlPublicOpenRes) {
			if (result.meterId==null) {
				result.meterId=decodeMeterId(((SmlPublicOpenRes)asn).getServerId());
			}
			return;
		}
		
		if (asn instanceof SmlGetListRes) {
			SmlGetListRes res=(SmlGetListRes) asn;
			
			if (result.meterId==null) {
				result.meterId=decodeMeterId(res.getServerId());
			}
			// my meter has "ActSensorTime" as index of seconds - is this somehow useful?
			decodeASNObject(result, res.getValList());
			return;
		}
		
		if (asn instanceof SmlList) {
			for (ASNObject o: ((SmlList)asn).seqArray()) {
				decodeASNObject(result, o);
			}
			return;
		}
		
		if (asn instanceof SmlListEntry) {
			SmlListEntry e=(SmlListEntry) asn;

			String obisCode=decodeObisCode(e.getObjName());
			
			if (obisCode==null) {
				return; // may happen on incomplete SML message
			}
			
//			System.err.println(obisCode+" "+e.getValue());
			
			if ("129-129:199.130.3*255".equals(obisCode)) { // manufacturer id
				return;
			}
			if ("1-0:0.0.9*255".equals(obisCode)) { // meter serial
				return;
			}
			if ("1-0:96.1.0*255".equals(obisCode)) { // meter id
				return;
			}
			if ("1-0:96.50.1*1".equals(obisCode)) { // manufacturer id
				return;
			}
			if ("1-0:96.50.1*4".equals(obisCode)) { // manufacturer id
				return;
			}
			if ("1-0:96.50.1*4".equals(obisCode)) { // hardware version
				return;
			}
			if ("1-0:96.50.4*4".equals(obisCode)) { // parameter version
				return;
			}
			if ("1-0:96.90.2*1".equals(obisCode)) { // firmware checksum
				return;
			}
			if ("1-0:0.2.0*0".equals(obisCode)) { // firmware version
				return;
			}
			if ("1-0:97.97.0*0".equals(obisCode)) { // status register
				return;
			}
			
			Reading reading=new Reading();
			reading.obisCode=obisCode;
			reading.name=ObisNameMap.get(obisCode);
			reading.unit=e.getUnit().toString();
			if (e.getValue()!=null && e.getValue().getChoice()!=null) {
				reading.value=decodeNumber(e.getValue().getChoice(),e.getScaler());
			}

			if (result.readings==null) {
				result.readings=new ArrayList<>();
			}
			result.readings.add(reading);
			
			return;
		}
		
		log.warn("ASNObject not implemented: ",asn.getClass().getName());
		
	}
	
	public static Number decodeNumber(ASNObject asn, Integer8 scaler) {
		if (asn==null) {
			return null;
		}
		
		int sc=(scaler==null)?0:scaler.getIntVal();

		if (asn instanceof Integer8) {
			byte val=((Integer8) asn).getVal();
			if (sc==0) {
				return val;
			}
			return new BigDecimal(val).scaleByPowerOfTen(sc);
		}

		if (asn instanceof Integer16) {
			short val=((Integer16) asn).getVal();
			if (sc==0) {
				return val;
			}
			return new BigDecimal(val).scaleByPowerOfTen(sc);
		}
		
		if (asn instanceof Integer32) {
			int val=((Integer32) asn).getVal();
			if (sc==0) {
				return val;
			}
			return new BigDecimal(val).scaleByPowerOfTen(sc);
		}

		if (asn instanceof Unsigned8) {
			int val=((Unsigned8) asn).getVal();
			if (sc==0) {
				return val;
			}
			return new BigDecimal(val).scaleByPowerOfTen(sc);
		}

		if (asn instanceof Unsigned16) {
			int val=((Unsigned16) asn).getVal();
			if (sc==0) {
				return val;
			}
			return new BigDecimal(val).scaleByPowerOfTen(sc);
		}
		
		if (asn instanceof Unsigned32) {
			int val=((Unsigned32) asn).getVal();
			if (sc==0) {
				return val;
			}
			return new BigDecimal(val).scaleByPowerOfTen(sc);
		}

		if (asn instanceof Unsigned64) {
			long val=((Unsigned64) asn).getVal();
			if (sc==0) {
				return val;
			}
			return new BigDecimal(val).scaleByPowerOfTen(sc);
		}
		
		log.warn("Number format not implemented: {}",asn.getClass().getName());
		return null;
	}
	
	public static String decodeObisCode(OctetString s) {
		return (s==null) ? null : decodeObisCode(s.getValue());
	}
	public static String decodeObisCode(byte[] bytes) {
		StringBuilder sb=new StringBuilder();
		sb.append(bytes[0] & 0xff);
		sb.append("-");
		sb.append(bytes[1] & 0xff);
		sb.append(":");
		sb.append(bytes[2] & 0xff);
		sb.append(".");
		sb.append(bytes[3] & 0xff);
		sb.append(".");
		sb.append(bytes[4] & 0xff);
		sb.append("*");
		sb.append(bytes[5] & 0xff);
		return sb.toString();
	}
	
	public static String decodeMeterId(OctetString s) {
		return (s==null) ? null : decodeMeterId(s.getValue());
	}
	
	/**
	 * https://netze.estw.de/erlangenGips/Erlangen/__attic__20210120_155237__estw1.de/Kopfnavigation/Netze/Messwesen/Messwesen/Herstelleruebergreifende-Identifikationsnummer-fuer-Messeinrichtungen.pdf
	 */
	public static String decodeMeterId(byte[] bytes) {
		StringBuilder sb=new StringBuilder();
		// 1st byte is 09 on my meter - no idea what the meaning is
		sb.append(Hex.encodeHex(bytes,1,1,false)[1]); // 2nd byte is "media"
		
		sb.append((char) bytes[2]); // 3 bytes manufacturer
		sb.append((char) bytes[3]);
		sb.append((char) bytes[4]);
		
		// following byte is "version" with 2 digits 
		NumberFormat nf=NumberFormat.getIntegerInstance();
		nf.setGroupingUsed(false);
		nf.setMinimumIntegerDigits(2);
		nf.setMaximumIntegerDigits(2);
		sb.append(nf.format(bytes[5]));

		// following 4 bytes are serial with 8 digits 
		
		nf.setMinimumIntegerDigits(8);
		nf.setMaximumIntegerDigits(8);
		sb.append(nf.format(new BigInteger(bytes, 6, 4)));
		
		return sb.toString();
	}
	
	protected static byte[] extractMessage(byte[] smlPayload) throws IOException {
		try {
			return new MessageExtractor(new DataInputStream(new ByteArrayInputStream(smlPayload)),1000).getSmlMessage();
		} catch (IOException ex) {
			if ("Timeout".equals(ex.getMessage())) {
				throw new IOException("Invalid SML payload: "+Hex.encodeHexString(smlPayload));
			}
			throw new IOException("Invalid SML payload: "+ex.getMessage());
		}
	}
	

}
