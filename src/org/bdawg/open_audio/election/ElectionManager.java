package org.bdawg.open_audio.election;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.log4j.Logger;
import org.bdawg.open_audio.OpenAudioProtos.ElectionMessage;
import org.bdawg.open_audio.OpenAudioProtos.ElectionMessage.ElectionMessageType;
import org.bdawg.open_audio.interfaces.ISender;
import org.bdawg.open_audio.mqtt.ISimpleMQCallback;
import org.bdawg.open_audio.mqtt.MQTTManager;
import org.bdawg.open_audio.sntp.TimeManager;




import com.google.protobuf.InvalidProtocolBufferException;

/**
 * The things you learn :
 * 
 * So, this is hard. While waiting for election for example, you may not
 * 
 * @author breland
 * 
 */
public class ElectionManager implements ISimpleMQCallback {

	private static final int SKIPS_BEFORE_ELECTION = 4;
	private static final long SLEEP_TIME_MS = 2000; // 2 seconds
	private static final long MAX_MISS_TIME = 8000; // 10 seconds = failure
	private static final int MAX_ELECTION_WAITS = 4;
	private static final long MASTER_SLEEP_TIME_MS = 1000; // send every 1
															// second
	private static final long MAX_OFFER_VALID = 30000; // 30 seconds offer
														// validitiy
	private static final Logger log = Logger.getLogger(ElectionManager.class);
	private static final Random random = new Random();

	ISender sender;
	private volatile Long lastMasterHB;
	private volatile String lasterMasterId;
	private Thread masterMonitorThread;
	private Thread masterThread;
	private volatile ClientState myState;
	private Map<String, ElectionMessage> offers = Collections
			.synchronizedMap(new HashMap<String, ElectionMessage>());

	public ElectionManager(ISender s) {
		this.sender = s;
	}

	public void init() {
		masterMonitorThread = new Thread(new Runnable() {
			@Override
			public void run() {
				int waitSkipCount = 0;

				int electionWaits = 0;
				setState(ClientState.STARTUP);
				ElectionMessage lastOffer = null;
				while (!masterMonitorThread.isInterrupted()) {
					if (lastMasterHB == null
							&& waitSkipCount < SKIPS_BEFORE_ELECTION) {
						// wait again
						waitSkipCount++;
						//log.debug(String.format("WaitSkipCount = %d",waitSkipCount));
					} else if ((lastMasterHB != null && Math.abs(lastMasterHB
							.longValue()
							- TimeManager.getTMInstance()
									.getCurrentTimeMillis()) >= MAX_MISS_TIME)
							|| waitSkipCount >= SKIPS_BEFORE_ELECTION) {
						//log.debug("It appears an election should or is happening");
						if (!ElectionManager.this.getState()
								.equals(ClientState.ELECTING)) {
							//log.debug("O NO! IT APPEARS THE MASTER IS NEWLY DEAD!");
							//log.debug("SENDING AND AWAITING ELECTIONS");
							electionWaits = 0;
							setState(ClientState.ELECTING);
							lastOffer = ElectionMessage
									.newBuilder()
									.setOffer(random.nextInt())
									.setNodeId(
											MQTTManager.getMQInstance().getId())
									.setTimestamp(
											TimeManager.getTMInstance()
													.getCurrentTimeMillis())
									.setType(ElectionMessageType.OFFER).build();
						} else {
							// If we're already electing, we should evaluate and
							// wait
							//log.debug("An election is already happening.");
							electionWaits++;
							if (electionWaits > MAX_ELECTION_WAITS) {
								//log.debug("Evaluating election results!");
								ElectionMessage winningest = null;
								int winningestInt = Integer.MIN_VALUE;
								for (Entry<String, ElectionMessage> entry : ElectionManager.this.offers
										.entrySet()) {
									if (winningestInt == entry.getValue()
											.getOffer()) {
										log.warn("O FUCK ALL. Redo the election, two offers matched.");
										winningest = null;
										winningestInt = Integer.MIN_VALUE;
										waitSkipCount = 0;
										// TODO:Redo the election
										break;
									}
									if (entry.getValue().getOffer() > winningestInt
											&& Math.abs(entry.getValue()
													.getTimestamp()
													- TimeManager
															.getTMInstance()
															.getCurrentTimeMillis()) < MAX_OFFER_VALID) {
										winningest = entry.getValue();
										winningestInt = entry.getValue()
												.getOffer();
									}
								}
								if (winningest == null) {
									log.error("The election failed.");
								} else {
									waitSkipCount = 0;
									log.debug(String
											.format("Picked a new master. Master is now %s",
													winningest.getNodeId()));
									if (winningest.getNodeId()
											.equals(MQTTManager.getMQInstance()
													.getId())) {
										//log.debug("Hey, I'm the new master!");
										setState(ClientState.MASTER);
									} else {
										//log.debug("Falling back to client mode.");
										setState(ClientState.SLAVE);
										try {
											Thread.sleep(200);
										} catch (InterruptedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									}
								}
								lastOffer = null;
								electionWaits = 0;
							}
						}
						// Send the last offer again either way
						if (lastOffer != null) {
							ElectionManager.this.sender.sendToPeers(ByteBuffer
									.wrap(lastOffer.toByteArray()));
						}
						// In electing mode, we care about offers
					} else if (lastMasterHB != null
							&& Math.abs(lastMasterHB.longValue()
									- TimeManager.getTMInstance()
											.getCurrentTimeMillis()) < MAX_MISS_TIME) {
						// We've seen the master HB recently
						// log.debug("We've seen the master, so we're still good.");
						String masterId = ElectionManager.this.lasterMasterId;

						if (!masterId.equals(MQTTManager.getMQInstance()
								.getId())) {
							// log.debug("I'm not the master, so setting to slave.");
							setState(ClientState.SLAVE);
						} else {
							// log.debug("I'm apparently the master... ");
						}
					}
					try {
						Thread.sleep(SLEEP_TIME_MS);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Thread.currentThread().interrupt();
					}
				}
			}
		});
		masterThread = new Thread(new Runnable() {
			public void run() {
				while (!Thread.interrupted()) {
					if (ElectionManager.this.getState() != null
							&& ElectionManager.this.getState()
									.equals(ClientState.MASTER)) {
						// Just send the HB yo
						ElectionMessage hbMessage = ElectionMessage
								.newBuilder()
								.setNodeId(MQTTManager.getMQInstance().getId())
								.setTimestamp(
										TimeManager.getTMInstance()
												.getCurrentTimeMillis())
								.setType(ElectionMessageType.MASTER_HB).build();
						ElectionManager.this.sender.sendToPeers(ByteBuffer
								.wrap(hbMessage.toByteArray()));
					} else {
						// We're not the master so NBD yo
					}
					try {
						Thread.sleep(MASTER_SLEEP_TIME_MS);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Thread.currentThread().interrupt();
					}
				}
			}
		});
		masterThread.start();
		masterMonitorThread.start();
	}

	/**
	 * This gets called when a new message arrives!
	 */
	@Override
	public void messageArrived(String topic, ByteBuffer bb) {
		// Got a new message from the wire! Neat!

		ElectionMessage em;
		try {
			em = ElectionMessage.parseFrom(bb.array());
		} catch (InvalidProtocolBufferException badMessageEx) {
			return;
		}
		if (em.getType().equals(ElectionMessageType.MASTER_HB)) {
			if (this.getState().equals(ClientState.MASTER)
					&& !em.getNodeId().equals(this.lasterMasterId)) {
				this.lastMasterHB = TimeManager.getTMInstance()
						.getCurrentTimeMillis() - 30000;
			} else {
				this.lastMasterHB = em.getTimestamp();
			}

			this.lasterMasterId = em.getNodeId();

		} else if (em.getType().equals(ElectionMessageType.OFFER)) {
			this.offers.put(em.getNodeId(), em);
		}

	}

	private synchronized void setState(ClientState setTo) {
		if (!setTo.equals(this.getState())) {
			ClientState old = this.getState();
			this.myState = setTo;
			String oldValue = old == null ? "NULL" : old.toString();
			if (setTo.equals(ClientState.SLAVE)) {
				log.debug("RWAW");
			}
			log.debug(String.format("Changed from %s to %s", oldValue,
					setTo.toString()));
		}
	}
	
	private synchronized ClientState getState(){
		return this.myState;
	}
}
