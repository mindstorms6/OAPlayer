package org.bdawg.open_audio.impl;

import java.util.Map;

import org.bdawg.open_audio.OpenAudioProtos.ClientCommand;
import org.bdawg.open_audio.Utils;
import org.bdawg.open_audio.interfaces.ISinglePlayable;

public class SinglePlayable implements ISinglePlayable{
	
	private int subIndex;
	private boolean canVLCPlayDirect;
	private String masterId;
	private Map<String,String> meta;
	private String dlType;
	
	private SinglePlayable(){
		
	}
	
	public static SinglePlayable fromClientCommand(ClientCommand incoming){
		SinglePlayable tr = new SinglePlayable();
		tr.subIndex = incoming.getItem().getSubIndex();
		tr.canVLCPlayDirect = incoming.getItem().getDirectPlaybackFlag();
		tr.masterId = incoming.getItem().getMasterId();
		tr.meta = Utils.kvListToMap(incoming.getItem().getMetaList());
		tr.dlType = incoming.getItem().getDlType();
		return tr;
	}
	
	
	
	@Override
	public Map<String,String> getMeta(){
		return this.meta;
	}

	@Override
	public int getSubIndex() {
		return this.subIndex;
	}

	@Override
	public String getMasterId() {
		return this.masterId;
	}

	@Override
	public boolean canVLCPlayDirect() {
		return this.canVLCPlayDirect;
	}

	@Override
	public String getDLType() {
		return this.dlType;
	}
}
