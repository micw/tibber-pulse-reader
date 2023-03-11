package de.wyraz.tibberpulse.sink;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import de.wyraz.tibberpulse.sml.SMLMeterData;

@Service
public class MQTTPublisher implements IMeterDataPublisher {
	
	@Value("${publish.mqtt.host}")
	protected String mqttHost;
	
	@Value("${publish.mqtt.port:1883}")
	protected int mqttPort;

	@Value("${publish.mqtt.username}")
	protected String mqttUsername;

	@Value("${publish.mqtt.password}")
	protected String mqttPassword;

	@Value("${publish.mqtt.topic}")
	protected String mqttTopic;

	@Value("${publish.mqtt.payload}")
	protected String mqttPayload;
	
	protected IMqttClient mqttClient;
	
	@PostConstruct
	public void startMqttClient() throws Exception {
		mqttClient = new MqttClient("tcp://"+mqttHost+":"+mqttPort,UUID.randomUUID().toString(), new MemoryPersistence());
		MqttConnectOptions options = new MqttConnectOptions();
		options.setAutomaticReconnect(false);
		options.setCleanSession(true);
		options.setConnectionTimeout(10);
		options.setUserName(mqttUsername);
		options.setPassword(mqttPassword.toCharArray());
		mqttClient.setCallback(new MqttCallback() {
			
			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
			}
			
			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
			}
			
			@Override
			public void connectionLost(Throwable cause) {
				System.err.println("Disconnected");
				System.exit(0);
			}
		});
		
		mqttClient.connect(options);
	}
	
	
	@Override
	public void publish(SMLMeterData data) throws IOException {
		if (data.getMeterId()==null) {
			return;
		}
		for (SMLMeterData.Reading reading: data.getReadings()) {
			String topic=replace(mqttTopic,reading);
			String payload=replace(mqttPayload,reading);
			try {
				mqttClient.publish(topic, payload.getBytes(StandardCharsets.UTF_8),0,false);
			} catch (MqttException ex) {
				throw new IOException(ex);
			}
			
		}
	}
	
	protected static String replace(String template, SMLMeterData.Reading reading) {
		return template
			.replace("{name}",StringUtils.firstNonBlank(reading.getName(),"unknown"))
			.replace("{obisCode}",StringUtils.firstNonBlank(reading.getObisCode(),"unknown"))
			.replace("{nameOrObisCode}",StringUtils.firstNonBlank(reading.getName(),reading.getObisCode(),"unknown"))
			.replace("{unit}",StringUtils.firstNonBlank(reading.getUnit(),""))
			.replace("{value}",reading.getValue().toString())
			;
	}

}
