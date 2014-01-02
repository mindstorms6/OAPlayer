package org.bdawg.open_audio.webObjects;

public class HearbeatObject {

	private String clientId;
	private long timestampClient;
	private boolean initHeartbeat;
	
	public HearbeatObject(){
		
	}
	
	public String getClientId() {
		return clientId;
	}
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	public long getTimestampClient() {
		return timestampClient;
	}
	public void setTimestampClient(long timestampClient) {
		this.timestampClient = timestampClient;
	}
	public boolean isInitHeartbeat() {
		return initHeartbeat;
	}
	public void setInitHeartbeat(boolean initHeartbeat) {
		this.initHeartbeat = initHeartbeat;
	}
}
