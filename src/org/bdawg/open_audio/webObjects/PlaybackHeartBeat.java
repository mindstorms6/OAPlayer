package org.bdawg.open_audio.webObjects;

public class PlaybackHeartBeat {

	private String itemId;
	private int subIndex;
	private long totalTime;
	private long progressTime;

	public String getItemId() {
		return itemId;
	}

	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	public int getSubIndex() {
		return subIndex;
	}

	public void setSubIndex(int subIndex) {
		this.subIndex = subIndex;
	}

	public long getTotalTime() {
		return totalTime;
	}

	public void setTotalTime(long totalTime) {
		this.totalTime = totalTime;
	}

	public long getProgressTime() {
		return progressTime;
	}

	public void setProgressTime(long progressTime) {
		this.progressTime = progressTime;
	}
	
}
