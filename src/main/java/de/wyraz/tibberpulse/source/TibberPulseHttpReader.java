package de.wyraz.tibberpulse.source;

import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;

import de.wyraz.tibberpulse.sink.MeterDataHandler;
import de.wyraz.tibberpulse.sml.SMLDecoder;
import de.wyraz.tibberpulse.sml.SMLMeterData;

public class TibberPulseHttpReader implements CommandLineRunner {

	protected final Logger log = LoggerFactory.getLogger(getClass());


	@Value("${sml.ignoreCrcErrors:false}")
	protected boolean ignoreCrcErrors;
	
	@Value("${tibber.pulse.http.url}")
	protected String tibberPulseUrl;

	@Value("${tibber.pulse.http.username}")
	protected String tibberPulseUsername;

	@Value("${tibber.pulse.http.password}")
	protected String tibberPulsePassword;

	protected final CloseableHttpClient http = HttpClients.createDefault();

	@Autowired
	protected ConfigurableApplicationContext ctx;

	@Autowired
	protected MeterDataHandler handler;
	
	@Override
	public void run(String... args) throws IOException {
		fetch(true);
	}
	
	@Scheduled(cron = "${tibber.pulse.http.cron}")
	public void fetch() throws IOException {
		fetch(false);
	}
	
	protected void fetch(boolean shutdownOnError) throws IOException {
		RequestBuilder get = RequestBuilder.get(tibberPulseUrl);
		if (!StringUtils.isAnyBlank(tibberPulseUsername, tibberPulsePassword)) {
			get.addHeader("Authorization","Basic "+Base64.encodeBase64String((tibberPulseUsername+":"+tibberPulsePassword).getBytes()))		;
		}

		try (CloseableHttpResponse resp = http.execute(get.build())) {
			if (resp.getStatusLine().getStatusCode() != 200) {
				log.warn("Invalid response from tibber pulse gateway endpoint: {}",resp.getStatusLine());
				if (shutdownOnError) {
					ctx.close();
				}
				return;
			}
			byte[] payload;
			try {
				payload=EntityUtils.toByteArray(resp.getEntity());
			} catch (Exception ex) {
				log.warn("Unable to extract payload from response",ex);
				if (shutdownOnError) {
					ctx.close();
				}
				return;
			}
			
			SMLMeterData data;
			try {
				data=SMLDecoder.decode(payload, !ignoreCrcErrors);
			} catch (Exception ex) {
				log.warn("Unable to parse SML from response",ex);
				if (shutdownOnError) {
					ctx.close();
				}
				return;
			}
			
			if (data!=null) {
				try {
					handler.publish(data);
				} catch (Exception ex) {
					log.warn("Unable publish meter data7",ex);
					if (shutdownOnError) {
						ctx.close();
					}
					return;
				}
			}
			
		} catch (Exception ex) {
			log.warn("Unable to fetch data from tibber pulse bridge",ex);
			if (shutdownOnError) {
				log.warn("Terminating.");
				ctx.close();
				System.exit(1);
			}
			return;
		}

	}

}
