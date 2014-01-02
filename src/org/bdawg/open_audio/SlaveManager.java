package org.bdawg.open_audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.bdawg.open_audio.OpenAudioProtos.ClientCommand;
import org.bdawg.open_audio.file_manager.Playable;
import org.bdawg.open_audio.http_utils.HttpUtils;
import org.bdawg.open_audio.interfaces.IPlayer;
import org.bdawg.open_audio.interfaces.ISender;
import org.bdawg.open_audio.mqtt.ISimpleMQCallback;
import org.bdawg.open_audio.sntp.TimeManager;
import org.bdawg.open_audio.webObjects.HBResponse;
import org.bdawg.open_audio.webObjects.HearbeatObject;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

public class SlaveManager implements ISimpleMQCallback{
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	private static final long DEFUALT_RUN_DELAY = 2;
	private static final long DEFAULT_PERIOD = 15;
	IPlayer player;
	ISender send;
	private ScheduledExecutorService scheduler;
	private ScheduledFuture<?> ntpHandle;
	private Runnable HBRunnable = new Runnable() {
		
		@Override
		public void run() {
			sendHBMessage(false);
		}
	};
	
	public SlaveManager(IPlayer toControl, ISender send){
		this.player = toControl;
		this.send = send;
		scheduler = Executors.newScheduledThreadPool(5);
	}
	
	
	@Override
	public void messageArrived(String topic, ByteBuffer message) {
		// TODO Auto-generated method stub
		try {
			ClientCommand cc = ClientCommand.parseFrom(message.array());
			switch (cc.getClientAction()){
			case PLAY:
				this.player.play(cc.getTimestamp(), Playable.fromPlayItem(cc.getItem()));
				break;
			case PAUSE:
				this.player.pause(cc.getTimestamp());
				break;
			case VOLUME:
				this.player.setVolume(cc.getNewVolume());
				break;
			case STOP:
				this.player.stop(cc.getTimestamp());
				break;
			case HEARTBEAT_REQ:
				sendHBMessage(false);
				break;
			default:
				break;
			}
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	
	
	public void init() throws IOException{		
		HBResponse resp = actualDoHB(true);
		if (resp.getOwner().equals(Utils.getNotOwnedString())){
			this.player.setOverlay(Utils.getMacAddresses());
		}
		ntpHandle = scheduler.scheduleAtFixedRate(HBRunnable, DEFUALT_RUN_DELAY,
				DEFAULT_PERIOD, TimeUnit.SECONDS);
	}
	
	public void sendHBMessage(final boolean markStartup){
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				HBResponse r = actualDoHB(markStartup);
				if (!r.getOwner().equals(Utils.getNotOwnedString())){
					SlaveManager.this.player.setOverlay("");
				} else {
					try {
						SlaveManager.this.player.setOverlay(Utils.getMacAddresses());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}
		});
		t.start();
		
		
	}
	
	private HBResponse actualDoHB(final boolean markStartup){
		String url = "http://oa.bdawg.org/clients/hb";
		HearbeatObject toSend = new HearbeatObject();
		try {
			toSend.setClientId(Utils.getMacAddresses());
			toSend.setInitHeartbeat(markStartup);
			toSend.setTimestampClient(TimeManager.getTMInstance().getCurrentTimeMillis());
			HttpResponse r = HttpUtils.executePost(url, toSend);
			if (r.getStatusLine().getStatusCode() != 200){
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
	
	
	
}
