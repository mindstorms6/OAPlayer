package org.bdawg.open_audio.impl;

import java.util.List;
import java.util.Map;

import org.bdawg.open_audio.interfaces.IPlayable;

public abstract class AbstractPlayable implements IPlayable {

	String masterId;
	String playableId;
	boolean needsDistribute = false;
	List<String> clients;
	Map<String,String> meta;
	
	public AbstractPlayable(String plyableId, String masterId, boolean needsDistribute, List<String> clients){
		this.masterId = masterId;
		this.playableId = plyableId;
		this.needsDistribute = needsDistribute;
		this.clients = clients;
	}

	@Override
	public String getMasterClientId() {
		return this.masterId;
	}

	@Override
	public String getId() {
		return this.playableId;
	}

	@Override
	public boolean needsDistribute() {
		return this.needsDistribute;
	}

	@Override
	public List<String> getClients() {
		return this.clients;
	}

}
