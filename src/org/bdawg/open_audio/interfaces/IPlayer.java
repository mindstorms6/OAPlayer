package org.bdawg.open_audio.interfaces;


public interface IPlayer {
	public void play(long ntpTime, IPlayItem toPlay);
	public void pause(long ntpTime);
	public void setVolume(int newVolume);
	public void stop(long ntpTime);
	
}
