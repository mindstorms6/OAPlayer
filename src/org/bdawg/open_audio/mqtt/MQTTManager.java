package org.bdawg.open_audio.mqtt;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.bdawg.open_audio.PropertyManager;
import org.bdawg.open_audio.PropertyManager.PropertyKey;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

public class MQTTManager implements MqttCallback {

	private static final Logger log = Logger.getLogger(MQTTManager.class);
	

	private static MQTTManager mqInstance;

	private MqttClient client;
	private Thread setupThread;
	private Object clientWatcher = new Object();
	private static BlockingQueue<MQTTFullMessage> messageToSendQ = new LinkedBlockingQueue<MQTTFullMessage>();
	private Thread sendThread;
	private static Map<String, List<ISimpleMQCallback>> callbackMap = Collections
			.synchronizedMap(new HashMap<String, List<ISimpleMQCallback>>());
	private static final String MQ_ID = MqttClient.generateClientId();

	private class MQTTFullMessage {
		protected MqttMessage message;
		protected String topic;

		public MQTTFullMessage(String topic, MqttMessage message) {
			this.topic = topic;
			this.message = message;
		}

	}

	private void reconstructClient() {
		log.debug("reconstruct client called");
		if (client == null) {
			log.debug("Client is null, making a new one");
			setupThread = new Thread(new Runnable() {
				@Override
				public void run() {
					log.debug("Starting new setup thread RUN");
					while (true) {
						log.debug("Calling setup client");
						boolean didSetup = setupClient();
						if (didSetup) {
							log.debug("Did setup, breaking out of setup thread");
							break;
						} else {
							log.debug("Failed to setup.");
							try {
								log.debug("Sleeping for a bit.");
								Thread.sleep(10000);
								log.debug("Attemtping reconnect.");
							} catch (InterruptedException e) {
								log.warn("Interruped, thread sleep wait setup");
								Thread.interrupted();
							}
						}
					}
				}
			});
			log.debug("Starting setup thread");
			setupThread.start();
		} else {
			log.debug("not reconstructiong, client was not null");
		}
	}

	private MQTTManager() {
		log.debug("MQTTManager constructor");

		reconstructClient();
	}

	private synchronized boolean setupClient() {
		if (this.client != null) {
			log.debug("Setup client called when client was not null");
			return true;
		} else {
			log.debug("Setting up new clietn");
			try {

				MqttDefaultFilePersistence perst = new MqttDefaultFilePersistence(
						"/tmp");
				this.client = new MqttClient(makeURI(),
						MQ_ID, perst);
				log.debug("New client declared. Setting cb to this");
				this.client.setCallback(this);
				log.debug("New instance of client declared. Attemtpign connect");
				this.client.connect();

				log.debug("Connected.");
				synchronized (clientWatcher) {
					log.debug("Notifying clients.");
					clientWatcher.notifyAll();
				}
				sendThread = getSendThreadImpl();
				log.debug("Setting up send thread.");
				sendThread.start();
				log.debug("Send thread started. Resubscribing to previous topics");
				for (String topic : callbackMap.keySet()){
					log.debug("Subbing to (setupClient) " + topic);
					this.client.subscribe(topic);
				}
				log.debug("All resubs done. Returing");
				return true;
			} catch (MqttException mex) {
				this.client = null;
				log.error("Caught MQException.", mex);
				return false;
			}
		}
	}

	private Thread getSendThreadImpl() {
		Thread tr = new Thread(new Runnable() {
			public void run() {
				while (true) {
					MQTTFullMessage toSend = null;
					try {
						//log.debug("Send message thread is about to wait on client");
						waitClient();
						//log.debug("Send message thread is waiting on blocking queue");
						toSend = messageToSendQ.take();
						//log.debug("Got a message to send");
						MQTTManager.this.client.publish(toSend.topic,
								toSend.message);
						//log.debug("Message away!");
					} catch (InterruptedException iex) {
						log.error("Interruped.", iex);
						Thread.interrupted();
					} catch (MqttException e) {
						log.error("MQException in sending thread", e);
						if (toSend != null) {
							messageToSendQ.offer(toSend);
						}
					}
				}
			}
		});
		return tr;
	}

	private String makeURI() {
		String toReturn = String
				.format("%s://%s:%d", PropertyManager.getStringForKey(
						PropertyKey.MQTT_SCHEME, "tcp"), PropertyManager
						.getStringForKey(PropertyKey.MQTT_HOST,
								"home.bdawg.org"), PropertyManager
						.getIntForKey(PropertyKey.MQTT_PORT, 1883));
		//log.debug("Make uri is returning " + toReturn);
		return toReturn;
	}

	public static MQTTManager getMQInstance() {
		if (mqInstance == null) {
			mqInstance = new MQTTManager();
		}
		return mqInstance;
	}

	public void subscribe(String topic, ISimpleMQCallback cb) throws MqttException, InterruptedException {
		log.debug("Subbing to topic : " + topic);
		List<ISimpleMQCallback> toPut;
		if (callbackMap.containsKey(topic)) {
			toPut = callbackMap.get(topic);
		} else {
			toPut = new ArrayList<ISimpleMQCallback>();
		}
		if (!toPut.contains(cb)){
			toPut.add(cb);
		}
		callbackMap.put(topic, toPut);
		log.debug("Successfully added to map. Teling client about intersted subscription.");
		waitClient();
		log.debug("Finally telling client after wait.");
		this.client.subscribe(topic);
	}

	public String getId(){
		return MQ_ID;
	}
	
	/**
	 * This call will not block, however your message will be queued internally
	 * until a connection is established to the server. The return value only
	 * dictates whether or not the message has been queued for send, not if it
	 * was sent. Messages are fifo.
	 * 
	 * @param toSend
	 *            the message payload
	 * @param topic
	 *            topic to send to
	 * @return if the message was queued to be sent
	 */
	public synchronized boolean sendMessage(ByteBuffer bb, String topic) {
		//log.debug("Queing a message!");
		MqttMessage m = new MqttMessage(bb.array());
		return messageToSendQ.offer(new MQTTFullMessage(topic, m));
	}

	private void waitClient() throws InterruptedException {
		//log.debug("Wait client called");
		if (this.client == null) {
			//log.debug("Waiting since client was null");
			synchronized (clientWatcher) {
				this.clientWatcher.wait();
				log.debug("And done waiting");
			}
		} else {
			//log.debug("Not waiting since client was not null");
		}
	}

	@Override
	public void connectionLost(Throwable cause) {
		log.debug("ERRMERGAHD Conn lost!");
		this.client = null;
		reconstructClient();
	}

	@Override
	public void messageArrived(final String topic, final MqttMessage message)
			throws Exception {
		//log.debug("Got a messsage!");
		final ByteBuffer bb = ByteBuffer.wrap(message.getPayload());
		List<ISimpleMQCallback> externalCallbacks = callbackMap.get(topic);
		if (externalCallbacks != null) {
			for (final ISimpleMQCallback externalCallback : externalCallbacks) {
				Thread t = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							externalCallback.messageArrived(topic, bb);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
				t.start();
			}
		} else {
			log.debug("Didn't have an external cb for topic " + topic);
		}
	}

	@Override
	public void deliveryComplete(final IMqttDeliveryToken token) {
	}

}
