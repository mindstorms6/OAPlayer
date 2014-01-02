package org.bdawg.open_audio.webObjects;

public class Progress {
	
	private long NTPTime;
	private long totalItemTime;
	private long progressTime;
	private boolean isStreaming;
	private String itemUUID;
	private int subIndex;
	
	public Progress(){
		
	}
	
	public long getNTPTime() {
		return NTPTime;
	}
	public void setNTPTime(long nTPTime) {
		NTPTime = nTPTime;
	}
	public long getTotalItemTime() {
		return totalItemTime;
	}
	public void setTotalItemTime(long totalItemTime) {
		this.totalItemTime = totalItemTime;
	}
	public long getProgressTime() {
		return progressTime;
	}
	public void setProgressTime(long progressTime) {
		this.progressTime = progressTime;
	}
	public boolean isStreaming() {
		return isStreaming;
	}
	public void setStreaming(boolean isStreaming) {
		this.isStreaming = isStreaming;
	}
	public String getItemUUID() {
		return itemUUID;
	}
	public void setItemUUID(String itemUUID) {
		this.itemUUID = itemUUID;
	}

	public int getSubIndex() {
		return subIndex;
	}

	public void setSubIndex(int subIndex) {
		this.subIndex = subIndex;
	}

}
