package de.wyraz.tibberpulse.sink;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.util.Strings;

import de.wyraz.tibberpulse.sml.SMLMeterData;
import de.wyraz.tibberpulse.sml.SMLMeterData.Reading;

/**
 * Filters and modifies readings
 * 
 * "Spec" is a list of rules, separated by whitespace and newline in the fomr of
 *   key=rule key=rule key=rule
 * 
 * "key" can be
 * - an obis code (1-0:1.8.0*255)
 * - friendly name of a reading (energyImportTotal)
 * 
 * "rule" can be
 * - IGNORE - the reading will be ignored
 * - KILOWATT - converts WATT to KILOWATT
 * - KILOWATT_HOURS - converts WATT_HOURS to KILOWATT_HOURS
 * 
 * @author mwyraz
 */
public class MeterReadingFilter {
	
	public enum Rule {
		
		IGNORE {
			@Override
			Reading apply(Reading input) {
				return null;
			}
		},
		
		KILOWATT("kW") {
			@Override
			Reading apply(Reading input) {
				return scaleUnit(input, "WATT", "KILOWATT", -3);
			}
		},
		
		KILOWATT_HOURS("kWh") {
			@Override
			Reading apply(Reading input) {
				return scaleUnit(input, "WATT_HOURS", "KILOWATT_HOURS", -3);
			}
		},
		
		;
		
		protected final String alias;
		
		private Rule(String alias) {
			this.alias=alias;
		}
		private Rule() {
			this.alias=null;
		}
		
		@Override
		public String toString() {
			if (alias==null) {
				return name();
			}
			return new StringBuilder()
				.append(name())
				.append("(").append(alias).append(")")
				.toString();
		}
		
		protected static Reading scaleUnit(Reading input, String expectedUnit, String newUnit, int scale) {
			if (!expectedUnit.equals(input.getUnit())) {
				return input;
			}
			return new Reading(input.getObisCode(), input.getName(), scaleByPowerOfTen(input.getValue(),scale), newUnit);
		}
		
		protected static Number scaleByPowerOfTen(Number input, int scale) {
			if (input==null) {
				return null;
			}
			return toBigDecimal(input).scaleByPowerOfTen(scale);
		}
		
		protected static BigDecimal toBigDecimal(Number input) {
			
	        if (input instanceof BigDecimal) {
	        	return (BigDecimal)input;
	        }
			
	        if (input instanceof Byte) {
	        	return new BigDecimal((byte) input);
	        }
	        if (input instanceof Short) {
	        	return new BigDecimal((short) input);
	        }
	        if (input instanceof Integer) {
	        	return new BigDecimal((int) input);
	        }
	        if (input instanceof Long) {
	        	return new BigDecimal((long) input);
	        }
	        if (input instanceof BigInteger) {
	        	return new BigDecimal((BigInteger) input);
	        }
	        
	        return BigDecimal.valueOf(input.doubleValue());
	    }
		
		
		protected static Rule find(String name) {
			for (Rule rule: Rule.values()) {
				if (rule.name().equalsIgnoreCase(name) || (rule.alias!=null && rule.alias.equalsIgnoreCase(name))) {
					return rule;
				}
			}
			throw new IllegalArgumentException("Invalid filter rule: "+name+". Allowed rules: "+Arrays.toString(Rule.values()));
		}
		
		abstract SMLMeterData.Reading apply(SMLMeterData.Reading input);
	}
	
	protected final Map<String,Rule> rules;
	
	public MeterReadingFilter(String specs) {
		rules=new HashMap<>();
		for (String spec: specs.split("[\\r\\n\\s]+")) {
			if (spec.isEmpty()) {
				continue;
			}
			int eqPos=spec.indexOf("=");
			if (eqPos<1 || eqPos==spec.length()-1) {
				throw new IllegalArgumentException("Invalid filter spec: "+spec);
			}
			String key=spec.substring(0,eqPos).toLowerCase();
			Rule rule=Rule.find(spec.substring(eqPos+1));
			
			rules.put(key, rule);
		}
	}
	
	public List<SMLMeterData.Reading> apply(List<SMLMeterData.Reading> input) {
		
		List<SMLMeterData.Reading> result=new ArrayList<>();
		
		for (SMLMeterData.Reading r: input) {
			Rule rule=rules.get(r.getObisCode().toLowerCase());
			if (rule==null && r.getName()!=null) {
				rule=rules.get(r.getName().toLowerCase());
			}
			if (rule==null) { // no filtering
				result.add(r);
			} else {
				r=rule.apply(r); // filter the reading
				if (r!=null) {
					result.add(r);
				}
			}
			
		}
		
		return result;
	}

}
