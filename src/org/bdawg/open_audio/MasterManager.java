package org.bdawg.open_audio;

import java.nio.ByteBuffer;

import org.bdawg.open_audio.interfaces.ISender;
import org.bdawg.open_audio.mqtt.ISimpleMQCallback;

public class MasterManager implements ISimpleMQCallback{

	
	private ISender sender;
	
	public MasterManager(ISender sender){
		this.sender = sender;
	}

	@Override
	public void messageArrived(String topic, ByteBuffer message) {
		// TODO Auto-generated method stub
		
	}
	
	public void init(){
		
	}
}
