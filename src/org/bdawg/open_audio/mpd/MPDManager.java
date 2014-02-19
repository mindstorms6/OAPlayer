package org.bdawg.open_audio.mpd;

import org.bdawg.open_audio.interfaces.IPlayer;
import org.bdawg.open_audio.webObjects.Progress;

public class MPDManager implements IPlayer {

	@Override
	public void play(long ntpTime, String localToPlay) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pause(long ntpTime) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setVolume(int newVolume) {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop(long ntpTime) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setOverlay(String overlay) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setAboutToEndCallback(Runnable r) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setEndedCallback(Runnable r) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isPlaying() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Progress getCurrentProgress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void jumpTo(long jumpTo) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setHasSyncLock(boolean hasSync) {
		// TODO Auto-generated method stub

	}

}
