package de.wyraz.tibberpulse.util;

public class ByteUtil {
	
	public static String toBits(byte b) {
		StringBuilder sb=new StringBuilder(8);
		for (int i=7;i>=0;i--) {
			sb.append((b & (1<<i))==0?'0':'1');
		}
		return sb.toString();
	}

	public static char[] HEX_CHARS="0123456789ABCDEF".toCharArray();
	
	public static String toHex(byte[] bytes) {
		if (bytes==null) {
			return null;
		}
		StringBuilder sb=new StringBuilder(bytes.length*2);
		for (int i=0;i<bytes.length;i++) {
			sb.append(HEX_CHARS[(bytes[i] & 0xF0) >> 4]);
			sb.append(HEX_CHARS[(bytes[i] & 0x0F)]);
		}
		return sb.toString();
	}
	public static String int8ToHex(int i) {
		return toHex(new byte[] {
			(byte) (i & 0xFF)
		});
	}
	public static String int16ToHex(long l) {
		return toHex(new byte[] {
			(byte) ((l >> 8) & 0xFF),
			(byte) (l & 0xFF)
		});
	}
	public static String int24ToHex(long l) {
		return toHex(new byte[] {
			(byte) ((l >> 16) & 0xFF),
			(byte) ((l >> 8) & 0xFF),
			(byte) (l & 0xFF)
		});
	}
	public static String int32ToHex(long l) {
		return toHex(new byte[] {
			(byte) ((l >> 24) & 0xFF),
			(byte) ((l >> 16) & 0xFF),
			(byte) ((l >> 8) & 0xFF),
			(byte) (l & 0xFF)
		});
	}
	public static String int40ToHex(long l) {
		return toHex(new byte[] {
			(byte) ((l >> 32) & 0xFF),
			(byte) ((l >> 24) & 0xFF),
			(byte) ((l >> 16) & 0xFF),
			(byte) ((l >> 8) & 0xFF),
			(byte) (l & 0xFF)
		});
	}
	public static String int64ToHex(long l) {
		return toHex(new byte[] {
			(byte) ((l >> 56) & 0xFF),
			(byte) ((l >> 48) & 0xFF),
			(byte) ((l >> 40) & 0xFF),
			(byte) ((l >> 32) & 0xFF),
			(byte) ((l >> 24) & 0xFF),
			(byte) ((l >> 16) & 0xFF),
			(byte) ((l >> 8) & 0xFF),
			(byte) (l & 0xFF)
		});
	}
	
}
