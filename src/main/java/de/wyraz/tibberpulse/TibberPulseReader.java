package de.wyraz.tibberpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TibberPulseReader {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(TibberPulseReader.class, args);
	}

}
