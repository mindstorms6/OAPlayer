package org.bdawg.open_audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.bdawg.open_audio.OpenAudioProtos.ClientCommand;
import org.bdawg.open_audio.OpenAudioProtos.Sync;
import org.bdawg.open_audio.PropertyManager.PropertyKey;
import org.bdawg.open_audio.Utils.OAConstants;
import org.bdawg.open_audio.file_manager.FileManager;
import org.bdawg.open_audio.http_utils.HttpUtils;
import org.bdawg.open_audio.impl.SinglePlayable;
import org.bdawg.open_audio.interfaces.IPlayer;
import org.bdawg.open_audio.interfaces.ISender;
import org.bdawg.open_audio.interfaces.ISimpleMQCallback;
import org.bdawg.open_audio.interfaces.ISinglePlayable;
import org.bdawg.open_audio.sntp.TimeManager;
import org.bdawg.open_audio.webObjects.HBResponse;
import org.bdawg.open_audio.webObjects.HearbeatObject;
import org.bdawg.open_audio.webObjects.Progress;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

public class SlaveManager implements ISimpleMQCallback {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private static final long DEFUALT_RUN_DELAY = 2;
	private static final long DEFAULT_PERIOD = 15;
	IPlayer player;
	ISender send;
	private ScheduledExecutorService scheduler;
	private ScheduledFuture<?> ntpHandle;
	private ISinglePlayable currentItem;
	private Runnable externalEndedCallback;
	private boolean syncLock = false;
	private boolean syncLockTwo = false;
	
	private Runnable HBRunnable = new Runnable() {

		@Override
		public void run() {
			sendHBMessage(false);
		}
	};

	public SlaveManager(IPlayer toControl, ISender send) {
		this.player = toControl;
		this.send = send;
		scheduler = Executors.newScheduledThreadPool(5);
		this.player.setEndedCallback(new Runnable() {

			@Override
			public void run() {
				SlaveManager.this.currentItem = null;
				if (SlaveManager.this.externalEndedCallback != null) {
					SlaveManager.this.externalEndedCallback.run();
				}
			}
		});
	}

	public void setProgressRunnable(int intervalSeconds, Runnable toExecute) {
		this.scheduler.scheduleAtFixedRate(toExecute, 0, intervalSeconds,
				TimeUnit.SECONDS);
	}

	public void setEndedRunnable(Runnable r) {
		this.externalEndedCallback = r;
	}

	public void setAboutToEndRunnable(Runnable r) {
		this.player.setAboutToEndCallback(r);
	}

	@Override
	public void messageArrived(String topic, ByteBuffer message) {
		// TODO Auto-generated method stub
		try {
			ClientCommand cc = ClientCommand.parseFrom(message.array());
			switch (cc.getClientAction()) {
			case PLAY:
				syncLock=false;
				syncLockTwo=false;
				SinglePlayable aboutToPlay = SinglePlayable
						.fromClientCommand(cc);
				
				this.currentItem = aboutToPlay;
				// If we have already downloaded it, then we're good, play
				// it. (Which, hopefully, it already is downloaded)
				// If we haven't downloaded it, and we need to, download,
				// then do the play
				// FileManager will also check if VLC can play direct
				
				FileManager.playWhenReady(cc.getTimestamp(), aboutToPlay,
						this.player, cc);
				break;
			case PAUSE:
				syncLock=false;
				syncLockTwo=false;
				this.player.pause(cc.getTimestamp());
				break;
			case VOLUME:
				this.player.setVolume(cc.getNewVolume());
				break;
			case STOP:
				syncLock=false;
				syncLockTwo=false;
				this.player.stop(cc.getTimestamp());
				break;
			case HEARTBEAT_REQ:
				sendHBMessage(false);
				break;
			case DOWNLOAD:
				FileManager.queueForDownload(SinglePlayable
						.fromClientCommand(cc));
				break;
			case SYNC:
				this.alignPlayback(cc.getSync());
				break;
			default:
				break;
			}
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			logger.warn("Invalid message came across the wire!", e);
		} catch (IOException ioex) {
			logger.warn("Filemanager failed to download");
		}
	}

	private void alignPlayback(Sync sync) {
		if (sync != null) {
			//ensure ssame thing
			if (this.currentItem.getOwningPlayableId().equals(sync.getOwningPBId()) && this.currentItem.getSubIndex() == sync.getSubIndex()){
				logger.info("At " + sync.getMasterNTP() + " master was at " + sync.getMasterElapsed());
				long diffMasterAndMe = sync.getMasterNTP() - TimeManager.getTMInstance().getCurrentTimeMillis();
				long whereWasI = diffMasterAndMe + this.player.getCurrentProgress().getProgressTime();
				logger.info("At " + sync.getMasterNTP() + " I was at " + whereWasI);
				long difference = sync.getMasterElapsed() - whereWasI;
				if (Math.abs(difference) <= 50){
					if (syncLock){
						syncLockTwo=true;
					} else {
						syncLock=true;
						syncLockTwo=false;
					}
				} else if (Math.abs(difference) > 500){
					syncLock=false;
					syncLockTwo=false;
				}
				logger.info("Which means I'm off by " + difference);
				if ( syncLock && syncLockTwo){
					logger.info("Had sync lock, NOT JUMPING");
					this.player.setHasSyncLock(true);
					//Have sync lokc
				} else {
					long jumpPenalty = 10;
					long shouldJumpTo = this.player.getCurrentProgress().getProgressTime() + difference + jumpPenalty;
					this.player.jumpTo(shouldJumpTo);
					logger.info("Jumping to " + shouldJumpTo);
					this.player.setHasSyncLock(false);
					//Don't have sync lock
				}
			} else {
				logger.warn("Tried to sync different item than what was playing.");
			}
		}
	}
	
	public void init() throws IOException {
		HBResponse resp = actualDoHB(true);
		if (resp.getOwner().equals(Utils.OAConstants.NOT_OWNED_STRING)) {
			this.player.setOverlay(Utils.getMacAddresses());
		}
		if (resp.getManualOffset() != 0){
			TimeManager.getTMInstance().setManualOffset(resp.getManualOffset());
		}
		ntpHandle = scheduler.scheduleAtFixedRate(HBRunnable,
				DEFUALT_RUN_DELAY, DEFAULT_PERIOD, TimeUnit.SECONDS);
	}

	public void sendHBMessage(final boolean markStartup) {
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				HBResponse response = actualDoHB(markStartup);
				if (response != null){
					TimeManager.getTMInstance().setManualOffset(response.getManualOffset());
				}
				if (response != null
						&& !response.getOwner().equals(
								Utils.OAConstants.NOT_OWNED_STRING)) {
					SlaveManager.this.player.setOverlay("");
					
				} else {
					try {
						SlaveManager.this.player.setOverlay(Utils
								.getMacAddresses());
					} catch (IOException e) {
						logger.error("Failed to set overlay with MAC address", e);
					}
				}

			}
		});
		t.start();

	}

	private HBResponse actualDoHB(final boolean markStartup) {
		String baseURL = PropertyManager.getStringForKey(PropertyKey.WS_HOST,
				OAConstants.WS_HOST);
		String url = baseURL + "/clients/hb";
		HearbeatObject toSend = new HearbeatObject();
		try {
			toSend.setClientId(Utils.getMacAddresses());
			toSend.setInitHeartbeat(markStartup);
			toSend.setTimestampClient(TimeManager.getTMInstance()
					.getCurrentTimeMillis());
			HttpResponse r = HttpUtils.executePost(url, toSend);
			if (r.getStatusLine().getStatusCode() != 200) {
				logger.warn("Failed to send heart beat!");
				return null;
			} else {
				ObjectMapper m = new ObjectMapper();
				return m.readValue(r.getEntity().getContent(), HBResponse.class);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public ISinglePlayable getCurrentItem() {
		return this.currentItem;
	}

	public Progress getCurrentProgress() {
		Progress tr = this.player.getCurrentProgress();
		if (tr != null) {
			tr.setItemUUID(this.currentItem.getOwningPlayableId());
			tr.setSubIndex(this.currentItem.getSubIndex());
		}
		return tr;
	}

}
