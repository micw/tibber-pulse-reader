package de.wyraz.tibberpulse.sink;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import de.wyraz.tibberpulse.sml.SMLMeterData;

@Component
public class MeterDataHandler {
	
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired(required = false)
	protected IMeterDataPublisher[] publishers;
	
	protected MeterReadingFilter filter;
	
	@Value("${publish.filters}")
	protected void setFilterSpec(String filters) {
		if (!StringUtils.isBlank(filters)) {
			this.filter=new MeterReadingFilter(filters);
		}
	}
	
	@PostConstruct
	public void checkPublishers() {
		if (publishers==null || publishers.length==0) {
			log.warn("No publishers are configured.");
		}
	}
	
	public void publish(SMLMeterData data) throws IOException {
		
		if (filter!=null) {
			data=new SMLMeterData(data.getMeterId(),
					filter.apply(data.getReadings()));
		}
		
		if (publishers==null) {
			log.warn("Got meter data but no publishers are configured:\n{}",data);
		} else {
			log.debug("Publishing meter data:\n{}",data);
			for (IMeterDataPublisher publisher: publishers) {
				publisher.publish(data);
			}
		}
	}

}
