package org.bdawg.open_audio;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.bdawg.open_audio.OpenAudioProtos.ClientCommand;
import org.bdawg.open_audio.file_manager.Playable;
import org.bdawg.open_audio.interfaces.IPlayer;
import org.bdawg.open_audio.interfaces.ISender;
import org.bdawg.open_audio.mqtt.ISimpleMQCallback;

import com.google.protobuf.InvalidProtocolBufferException;

public class SlaveManager implements ISimpleMQCallback{
	
	IPlayer player;
	ISender send;
	public SlaveManager(IPlayer toControl, ISender send){
		this.player = toControl;
		this.send = send;
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

	
	public void init(){
		
	}
	
}
