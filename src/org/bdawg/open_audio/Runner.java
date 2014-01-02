package org.bdawg.open_audio;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.log4j.BasicConfigurator;
import org.bdawg.open_audio.interfaces.ISender;
import org.bdawg.open_audio.mqtt.MQTTManager;
import org.bdawg.open_audio.sntp.TimeManager;
import org.bdawg.open_audio.vlc.VLCManager;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;

public class Runner {

	public static void main(String[] args) throws MqttException,
			InterruptedException, JsonParseException, JsonMappingException,
			IOException {
		BasicConfigurator.configure();
		Logger logger = LoggerFactory.getLogger(Runner.class);
		logger.debug("Starting...");
		String myMacAddress = null;
		RateLimiter limit = RateLimiter.create(2);
		logger.debug("Getting MAC");
		do{
			try {
				limit.acquire();
				myMacAddress = Utils.getMacAddresses();
			} catch (IOException IOEX){
				logger.warn("Error getting mac... waithing...");
			}
		} while (myMacAddress == null);
		logger.debug("Got MAC");
		

		Runnable empty = new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				
			}
		};
		VLCManager vlcMgr = new VLCManager(empty,empty);

		final String baseTopic = "/home/breland/pi_audio/";
		final String clientTopic = baseTopic + myMacAddress;
		final String masterClientTopic = clientTopic + "/master";
		
		logger.debug("Starting NTP");
		// Start the NTP manager
		TimeManager.getTMInstance().updateNow();
		logger.debug("Starting MQTT");
		// Start the MQTT manager
		MQTTManager.getMQInstance();
		
		logger.debug("Starting slave manager");
		final SlaveManager sm = new SlaveManager(vlcMgr, new ISender(){
			@Override
			public void sendToPeers(ByteBuffer message) {
				MQTTManager.getMQInstance().sendMessage(message, clientTopic);
			}
			@Override
			public void sendToTopic(ByteBuffer message, String topic) {
				MQTTManager.getMQInstance().sendMessage(message, topic);
				
			}
		});
		logger.debug("Subscribing to ClientTopic");
		MQTTManager.getMQInstance().subscribe(clientTopic, sm);
		sm.init();
		
		final MasterManager mm = new MasterManager(new ISender(){
			@Override
			public void sendToPeers(ByteBuffer message) {
				//no op for this
				
			}
			@Override
			public void sendToTopic(ByteBuffer message, String topic) {
				MQTTManager.getMQInstance().sendMessage(message, topic);
				
			}
		});
		MQTTManager.getMQInstance().subscribe(masterClientTopic, mm);
		logger.debug("Startup completed.");
		mm.init();
		
		//This thread is for timeing precision. Not just to spawn another thread, I promise.
		Thread sleepers = new Thread(new Runnable() {
			
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(Long.MAX_VALUE);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}
		});
		sleepers.start();
		
		try {
			Thread.currentThread().join();
		} catch (InterruptedException ex) {
			Thread.interrupted();
		}

	}
}
