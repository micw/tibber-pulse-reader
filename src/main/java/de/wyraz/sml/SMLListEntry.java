package de.wyraz.sml;

import org.openmuc.jsml.EUnit;

public class SMLListEntry extends AbstractSMLObject {
	
	protected byte[] objName;
	protected Long status;
	protected SMLTime valTime;
	protected EUnit valUnit;
	protected Number scaler;
	protected Object value;
	protected byte[] valueSignature;
	
	public byte[] getObjName() {
		return objName;
	}
	public Long getStatus() {
		return status;
	}
	public SMLTime getValTime() {
		return valTime;
	}
	public EUnit getValUnit() {
		return valUnit;
	}
	public Number getScaler() {
		return scaler;
	}
	public Object getValue() {
		return value;
	}
	public byte[] getValueSignature() {
		return valueSignature;
	}
	
	

}
