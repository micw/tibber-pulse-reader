package de.wyraz.tibberpulse.sink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import de.wyraz.tibberpulse.sml.SMLMeterData;

public class MeterReadingFilterTests {
	
	static final List<SMLMeterData.Reading> readings=Arrays.asList(
			new SMLMeterData.Reading("1-0:1.8.0*255", "energyImportTotal", 123123.45, "WATT_HOURS"),
			new SMLMeterData.Reading("1-0:2.8.0*255", "energyExportTotal", 345234.56, "WATT_HOURS"),
			new SMLMeterData.Reading("1-0:16.7.0*255", "powerTotal", 11111.22, "WATT"),
			new SMLMeterData.Reading("1-0:36.7.0*255", "powerL1", 22333.33, "WATT")
			);

	@Test
	public void testFilterNone() {
		assertThat(new MeterReadingFilter("").apply(readings))
			.extracting(Object::toString)
			.containsExactlyInAnyOrder(
				"1-0:1.8.0*255 / energyImportTotal = 123123.45 WATT_HOURS",
			    "1-0:2.8.0*255 / energyExportTotal = 345234.56 WATT_HOURS",
				"1-0:16.7.0*255 / powerTotal = 11111.22 WATT",
				"1-0:36.7.0*255 / powerL1 = 22333.33 WATT"
			);
	}
	
	@Test
	public void testFilterInvalid() {
		assertThatCode(() -> new MeterReadingFilter("somestuff"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Invalid filter spec: somestuff")
			;

		assertThatCode(() -> new MeterReadingFilter("=somestuff"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Invalid filter spec: =somestuff")
			;
		
		assertThatCode(() -> new MeterReadingFilter("somestuff="))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Invalid filter spec: somestuff=")
			;

		
		assertThatCode(() -> new MeterReadingFilter("somestuff=UNKNOWN"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageStartingWith("Invalid filter rule: UNKNOWN. Allowed rules: [IGNORE, KILOWATT(kW)")
			;
		
	}
	
	@Test
	public void testFilterIgnore() {
		assertThat(new MeterReadingFilter("PoWerToTal=IgNoRe 1-0:2.8.0*255=IGNORE").apply(readings))
			.extracting(Object::toString)
			.containsExactlyInAnyOrder(
					"1-0:1.8.0*255 / energyImportTotal = 123123.45 WATT_HOURS",
					"1-0:36.7.0*255 / powerL1 = 22333.33 WATT"
				);
	}

	@Test
	public void testFilterKilowatt() {
		assertThat(new MeterReadingFilter("powerTotal=KILOWATT").apply(readings))
			.extracting(Object::toString)
			.containsExactlyInAnyOrder(
					"1-0:1.8.0*255 / energyImportTotal = 123123.45 WATT_HOURS",
				    "1-0:2.8.0*255 / energyExportTotal = 345234.56 WATT_HOURS",
					"1-0:16.7.0*255 / powerTotal = 11.11122 KILOWATT",
					"1-0:36.7.0*255 / powerL1 = 22333.33 WATT"
				);
	}

	@Test
	public void testFilterKW() {
		assertThat(new MeterReadingFilter("powerTotal=kW").apply(readings))
			.extracting(Object::toString)
			.containsExactlyInAnyOrder(
					"1-0:1.8.0*255 / energyImportTotal = 123123.45 WATT_HOURS",
				    "1-0:2.8.0*255 / energyExportTotal = 345234.56 WATT_HOURS",
					"1-0:16.7.0*255 / powerTotal = 11.11122 KILOWATT",
					"1-0:36.7.0*255 / powerL1 = 22333.33 WATT"
				);
	}
	
	@Test
	public void testFilterKWWrongUnit() {
		assertThat(new MeterReadingFilter("energyImportTotal=kWh").apply(readings))
			.extracting(Object::toString)
			.containsExactlyInAnyOrder(
					"1-0:1.8.0*255 / energyImportTotal = 123.12345 KILOWATT_HOURS",
				    "1-0:2.8.0*255 / energyExportTotal = 345234.56 WATT_HOURS",
					"1-0:16.7.0*255 / powerTotal = 11111.22 WATT",
					"1-0:36.7.0*255 / powerL1 = 22333.33 WATT"
				);
	}
	
}
