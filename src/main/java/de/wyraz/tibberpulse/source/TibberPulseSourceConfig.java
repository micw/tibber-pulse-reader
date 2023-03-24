package de.wyraz.tibberpulse.source;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TibberPulseSourceConfig {
	
	@Bean
	public Object getTibberPulseSource(@Value("${tibber.pulse.source}") String source) {
		if ("http".equals(source)) {
			return new TibberPulseHttpReader();
		}
		if ("mqtt".equals(source)) {
			return new MQTTSource();
		}
		
		throw new IllegalArgumentException("Source most be 'http' or 'mqtt'");
	}

}
