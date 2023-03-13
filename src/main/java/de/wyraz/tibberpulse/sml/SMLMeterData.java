package de.wyraz.tibberpulse.sml;

import java.util.List;

public class SMLMeterData {
	
	public static class Reading {
		protected String obisCode;
		protected String name;
		protected Number value;
		protected String unit;
		
		public Reading() {
		}
		
		public Reading(String obisCode, String name, Number value, String unit) {
			super();
			this.obisCode = obisCode;
			this.name = name;
			this.value = value;
			this.unit = unit;
		}

		public String getName() {
			return name;
		}
		public String getObisCode() {
			return obisCode;
		}
		public String getUnit() {
			return unit;
		}
		public Number getValue() {
			return value;
		}
		
		@Override
		public String toString() {
			StringBuilder sb=new StringBuilder();
			sb.append(obisCode);
			if (name!=null) {
				sb.append(" / ").append(name);
			}
			sb.append(" = ").append(value);
			if (unit!=null) {
				sb.append(" ").append(unit);
			}
			return sb.toString();
		}
	}
	
	public SMLMeterData() {
	}
	
	public SMLMeterData(String meterId, List<Reading> readings) {
		this.meterId=meterId;
		this.readings=readings;
	}
	
	
	protected String meterId;
	protected List<Reading> readings;

	public String getMeterId() {
		return meterId;
	}
	public List<Reading> getReadings() {
		return readings;
	}
	
	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder();
		sb.append("Meter ").append(meterId);
		if (readings!=null) {
			for (Reading r: readings) {
				sb.append("\n ").append(r);
			}
		}
		return sb.toString();
	}

}
