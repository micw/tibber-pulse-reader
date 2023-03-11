package de.wyraz.tibberpulse.sml;

import java.util.Map;
import java.util.TreeMap;

/**
 * https://www.bundesnetzagentur.de/DE/Beschlusskammern/BK06/BK6_83_Zug_Mess/835_mitteilungen_datenformate/Mitteilung_24/2_EDIFACT-Konsultationsdokumente/Codeliste%20der%20OBIS-Kennzahlen%20und%20Medien%202.4.pdf?__blob=publicationFile&v=1
 * https://www.promotic.eu/en/pmdoc/Subsystems/Comm/PmDrivers/IEC62056_OBIS.htm
 */
public class ObisNameMap {
	
	protected static final Map<String, String> obisNameMap = new TreeMap<>();
	static {
		obisNameMap.put("1-0:1.8.0*255", "energyImportTotal");
		obisNameMap.put("1-0:1.8.1*255", "energyImportTariff1");
		obisNameMap.put("1-0:1.8.2*255", "energyImportTariff2");
		obisNameMap.put("1-0:2.8.0*255", "energyExportTotal");
		obisNameMap.put("1-0:16.7.0*255", "powerTotal");
		obisNameMap.put("1-0:36.7.0*255", "powerL1");
		obisNameMap.put("1-0:56.7.0*255", "powerL2");
		obisNameMap.put("1-0:76.7.0*255", "powerL3");
	}
	
	public static String get(String obisCode) {
		return obisNameMap.get(obisCode);
	}

}

