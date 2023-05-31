package de.wyraz.tibberpulse.source;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import de.wyraz.tibberpulse.sink.MeterDataHandler;
import de.wyraz.tibberpulse.sml.SMLDecoder;
import de.wyraz.tibberpulse.sml.SMLMeterData;

public class MQTTSource {

	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	@Value("${sml.ignoreCrcErrors:false}")
	protected boolean ignoreCrcErrors;

	@Value("${tibber.pulse.mqtt.host}")
	protected String mqttHost;
	
	@Value("${tibber.pulse.mqtt.tls:false}")
	protected boolean mqttTls;
	
	@Value("${tibber.pulse.mqtt.port:1883}")
	protected int mqttPort;

	@Value("${tibber.pulse.mqtt.username}")
	protected String mqttUsername;

	@Value("${tibber.pulse.mqtt.password}")
	protected String mqttPassword;

	@Value("${tibber.pulse.mqtt.topic}")
	protected String mqttTopic;

	@Value("${tibber.pulse.mqtt.payloadEncoding}")
	protected PayloadEncoding mqttPayloadEncoding;
	
	protected IMqttClient mqttClient;
	
	@PostConstruct
	public void startMqttClient() throws Exception {
		
		String protocol=mqttTls?"ssl":"tcp";
		
		mqttClient = new MqttClient(protocol+"://"+mqttHost+":"+mqttPort,UUID.randomUUID().toString(), new MemoryPersistence());
		MqttConnectOptions options = new MqttConnectOptions();
		options.setAutomaticReconnect(false);
		options.setCleanSession(true);
		options.setConnectionTimeout(10);
		if (!StringUtils.isAnyBlank(mqttUsername, mqttPassword)) {
			options.setUserName(mqttUsername);
			options.setPassword(mqttPassword.toCharArray());
		}
		mqttClient.setCallback(new MqttCallback() {
			
			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				handleMessage(topic, message);
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
		mqttClient.subscribe(mqttTopic);
	}
	
	@Autowired
	protected MeterDataHandler handler;
	
	protected void handleMessage(String topic, MqttMessage message) {
		SMLMeterData data;
		try {
			byte[] payload=mqttPayloadEncoding.decode(message.getPayload());
			if (payload==null) {
				return;
			}
			
			data=SMLDecoder.decode(payload, !ignoreCrcErrors);
		} catch (Exception ex) {
			log.warn("Unable to parse SML from response",ex);
			return;
		}
		
		if (data!=null) {
			try {
				handler.publish(data);
			} catch (Exception ex) {
				log.warn("Unable publish meter data",ex);
				return;
			}
		}
	}
	

}
