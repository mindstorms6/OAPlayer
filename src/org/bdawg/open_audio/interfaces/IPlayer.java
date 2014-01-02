package org.bdawg.open_audio.interfaces;


public interface IPlayer {
	public void play(long ntpTime, String localToPlay);
	public void pause(long ntpTime);
	public void setVolume(int newVolume);
	public void stop(long ntpTime);
	public void setOverlay(String overlay);
	public void setAboutToEndCallback(Runnable r);
	public void setEndedCallback(Runnable r);
}
