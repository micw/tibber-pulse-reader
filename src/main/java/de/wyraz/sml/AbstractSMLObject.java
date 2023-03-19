package de.wyraz.sml;

import java.lang.reflect.Field;
import java.util.List;

import de.wyraz.tibberpulse.util.ByteUtil;

public abstract class AbstractSMLObject {

	public String toString() {
		StringBuilder sb=new StringBuilder();
		appendToString(sb, 0, true);
		return sb.toString();
	}
	
	protected void appendToString(StringBuilder sb, int indent, boolean printClassName) {
		if (printClassName) {
			indentAppend(sb, indent, getClass().getSimpleName());
			indent++;
		}
		for (Field field: getClass().getDeclaredFields()) {
			field.setAccessible(true);
			Object value;
			try {
				value=field.get(this);
			} catch (Exception ex) {
				value=ex.toString();
			}
			indentAppend(sb, indent, field.getName(), value);
		}
	}

	protected void indentAppend(StringBuilder sb, int indent, String name, Object value) {
		
		if (value instanceof List) {
			List<?> list=(List<?>) value;
			indentAppend(sb, indent, name+" = List["+list.size()+"]");
			indent++;
			for (int i=0;i<list.size();i++) {
				indentAppend(sb, indent, "["+i+"]",list.get(i));
			}
			return;
		}
		
		if (value instanceof AbstractSMLObject) {
			indentAppend(sb, indent, name+" = "+value.getClass().getSimpleName());
			((AbstractSMLObject)value).appendToString(sb, indent+1, false);
			return;
		}
		if (value instanceof byte[]) {
			indentAppend(sb, indent, name+" = 0x"+ByteUtil.toHex((byte[])value));
			return;
		}
		
		indentAppend(sb, indent, name+" = "+String.valueOf(value));
	}
	
	protected void indentAppend(StringBuilder sb, int indent, String line) {
		if (sb.length()>0) {
			sb.append("\n");
		}
		for (int i=0;i<indent;i++) {
			sb.append("  ");
		}
		sb.append(line);
	}
	
}
