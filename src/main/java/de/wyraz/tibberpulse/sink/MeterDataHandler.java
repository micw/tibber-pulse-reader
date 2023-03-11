package de.wyraz.tibberpulse.sink;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.wyraz.tibberpulse.sml.SMLMeterData;

@Component
public class MeterDataHandler {
	
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	protected IMeterDataPublisher[] publishers;
	
	public void publish(SMLMeterData data) throws IOException {
		log.debug("Read meter data:\n{}",data);
		for (IMeterDataPublisher publisher: publishers) {
			publisher.publish(data);
		}
	}

}
