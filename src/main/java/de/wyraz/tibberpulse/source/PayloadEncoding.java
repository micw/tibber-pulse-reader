package de.wyraz.tibberpulse.source;

import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum PayloadEncoding {
	
	HEX {
		@Override
		byte[] decode(byte[] payload) {
			try {
				return Hex.decodeHex(new String(payload, StandardCharsets.UTF_8));
			} catch (DecoderException ex) {
				log.warn("Unable to decode SML as HEX",ex);
				return null;
			}
		}
	},
	BINARY {
		@Override
		byte[] decode(byte[] payload) {
			return payload;
		}
	},
	;
	
	protected static final Logger log = LoggerFactory.getLogger(PayloadEncoding.class);
	
	
	abstract byte[] decode(byte[] payload);

}
