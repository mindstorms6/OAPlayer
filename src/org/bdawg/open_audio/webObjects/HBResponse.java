package org.bdawg.open_audio.webObjects;

public class HBResponse {
	private String owner;
	private long manualOffset;

	
	public HBResponse(){
		
	}
	
	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public long getManualOffset() {
		return manualOffset;
	}

	public void setManualOffset(long manualOffset) {
		this.manualOffset = manualOffset;
	}
}
