package org.bdawg.open_audio;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.log4j.BasicConfigurator;
import org.bdawg.open_audio.Utils.OAConstants;
import org.bdawg.open_audio.interfaces.ISender;
import org.bdawg.open_audio.interfaces.ISinglePlayable;
import org.bdawg.open_audio.mqtt.MQTTManager;
import org.bdawg.open_audio.sntp.TimeManager;
import org.bdawg.open_audio.vlc.VLCManager;
import org.bdawg.open_audio.webObjects.Progress;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;

public class Runner {

	static String myMacAddress = null;

	public static void main(String[] args) throws MqttException,
			InterruptedException, JsonParseException, JsonMappingException,
			IOException {
		BasicConfigurator.configure();
		System.setProperty("org.apache.commons.logging.Log",
				"org.apache.commons.logging.impl.SimpleLog");
		System.setProperty("org.apache.commons.logging.simplelog.showdatetime",
				"true");
		System.setProperty(
				"org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient",
				"warn");

		Logger logger = LoggerFactory.getLogger(Runner.class);
		logger.debug("Starting...");
		RateLimiter limit = RateLimiter.create(2);
		logger.debug("Getting MAC");
		do {
			try {
				limit.acquire();
				myMacAddress = Utils.getMacAddresses();
			} catch (IOException IOEX) {
				logger.warn("Error getting mac... waithing...");
			}
		} while (myMacAddress == null);
		logger.debug("Got MAC");

		VLCManager vlcMgr = new VLCManager();

		final String clientTopic = OAConstants.BASE_TOPIC + myMacAddress;
		final String masterClientTopic = clientTopic + "/master";

		logger.debug("Starting NTP");
		// Start the NTP manager
		TimeManager.getTMInstance().updateNow();
		logger.debug("Starting MQTT");
		// Start the MQTT manager
		MQTTManager.getMQInstance();

		logger.debug("Starting slave manager");
		final SlaveManager sm = new SlaveManager(vlcMgr, new ISender() {
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
		logger.debug("Initing Slave Mgr");
		sm.init();

		logger.debug("Starting master manager");
		final MasterManager mm = new MasterManager(new ISender() {
			@Override
			public void sendToPeers(ByteBuffer message) {
				// no op for this

			}

			@Override
			public void sendToTopic(ByteBuffer message, String topic) {
				MQTTManager.getMQInstance().sendMessage(message, topic);

			}
		});
		logger.debug("Subbing to master topic");
		MQTTManager.getMQInstance().subscribe(masterClientTopic, mm);
		logger.debug("Initing master manager.");
		mm.init();
		logger.debug("Setting complete callback");
		sm.setEndedRunnable(new Runnable() {

			@Override
			public void run() {
				if (sm.getCurrentItem().getMasterId()
						.equals(Runner.myMacAddress)) {
					mm.singlePlayableCallback();
				}

			}
		});
		sm.setAboutToEndRunnable(new Runnable() {

			@Override
			public void run() {
				if (sm.getCurrentItem().getMasterId()
						.equals(Runner.myMacAddress)) {
					mm.aboutToEndPlayableCallback();
				}

			}
		});

		sm.setProgressRunnable(10, new Runnable() {
			@Override
			public void run() {
				if (sm.getCurrentItem() != null
						&& sm.getCurrentItem().getMasterId()
								.equals(Runner.myMacAddress)) {
					mm.broadcastProgress(sm.getCurrentProgress());
				}
			}
		});

		logger.debug("Startup completed.");

		// This thread is for timeing precision. Not just to spawn another
		// thread, I promise.
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
			logger.debug("Main thread joining. Bye!");
			Thread.currentThread().join();
		} catch (InterruptedException ex) {
			Thread.interrupted();
		}

	}
}
