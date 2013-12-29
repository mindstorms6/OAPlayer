package org.bdawg.open_audio;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.BasicConfigurator;
import org.bdawg.open_audio.election.ElectionManager;
import org.bdawg.open_audio.http_utils.HttpUtils;
import org.bdawg.open_audio.interfaces.ISender;
import org.bdawg.open_audio.mqtt.MQTTManager;
import org.bdawg.open_audio.sntp.TimeManager;
import org.bdawg.open_audio.vlc.VLCManager;
import org.bdawg.open_audio.webObjects.WebClientAssociation;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;

public class Runner {

	private static final String baseURL = "http://oa.bdawg.org/";
	private static final String GroupResource = "associations";

	public static void main(String[] args) throws MqttException,
			InterruptedException, JsonParseException, JsonMappingException,
			IOException {
		BasicConfigurator.configure();
		Logger logger = LoggerFactory.getLogger(Runner.class);
		ObjectMapper om = new ObjectMapper();
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
		String URI = String.format("%s%s/%s", baseURL, GroupResource,
				myMacAddress);
		logger.debug("Attempting to fetch Group from URI : " + URI);
		int lastStatus = -1;
		HttpEntity respEntity;
		do {
			limit.acquire();
			HttpResponse resp = HttpUtils.executeGet(URI);
			lastStatus = resp.getStatusLine().getStatusCode();
			respEntity = resp.getEntity();
		} while (lastStatus < 200 || lastStatus > 299);
		logger.debug("Got Group!");
		String value = EntityUtils.toString(respEntity);
		logger.debug(value);
		WebClientAssociation assoc = om.readValue(value,
				WebClientAssociation.class);
		
		VLCManager vlcMgr = new VLCManager();

		final String baseTopic = "/home/breland/pi_audio/";
		final String groupName = assoc.getGroupId() + "/";
		final String electionTopic = "election";
		final String fullElectionTopic = baseTopic + groupName + electionTopic;
		final String clientTopic = baseTopic + myMacAddress;
		
		logger.debug("Starting NTP");
		// Start the NTP manager
		TimeManager.getTMInstance();
		logger.debug("Starting MQTT");
		// Start the MQTT manager
		MQTTManager.getMQInstance();
		logger.debug("Starting ElectionMGR");
		final ElectionManager em = new ElectionManager(new ISender() {
			@Override
			public void sendToPeers(ByteBuffer bb) {
				MQTTManager.getMQInstance().sendMessage(bb, fullElectionTopic);
			}
		});
		logger.debug("Subscribing to Election Topic");
		MQTTManager.getMQInstance().subscribe(fullElectionTopic, em);
		em.init();
		
		logger.debug("Starting slave manager");
		final SlaveManager sm = new SlaveManager(vlcMgr, new ISender(){
			@Override
			public void sendToPeers(ByteBuffer message) {
				MQTTManager.getMQInstance().sendMessage(message, clientTopic);
			}
		});
		logger.debug("Subscribing to ClientTopic");
		MQTTManager.getMQInstance().subscribe(clientTopic, sm);
		
		sm.init();
		
		logger.debug("Startup completed.");
		
		try {
			Thread.currentThread().join();
		} catch (InterruptedException ex) {
			Thread.interrupted();
		}

	}
}
