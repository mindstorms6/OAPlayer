package org.bdawg.open_audio.file_manager;

import org.bdawg.open_audio.Utils.OAConstants;
import org.bdawg.open_audio.impl.SinglePlayable;
import org.bdawg.open_audio.interfaces.IPlayer;
import org.bdawg.open_audio.interfaces.ISinglePlayable;

public class FileManager {

	private FileManager() {

	}

	public static void playWhenReady(long timestamp,
			ISinglePlayable aboutToPlay, IPlayer player) {
		// Fuck it, send straight to VLC
		if (aboutToPlay.getDLType().equals(OAConstants.DL_TYPE_SIMPLE_URL)) {
			String path = aboutToPlay.getMeta().get(
					OAConstants.META_SIMPLE_URL_LOCATION_KEY);
			player.play(timestamp, path);
		} else {
			throw new RuntimeException(
					"Not implemented anything besides simple URI downloads.");
		}

	}

	public static void queueForDownload(ISinglePlayable playable) {
		// TODO Auto-generated method stub
		if (playable.canVLCPlayDirect()) {
			// no - op
		}
	}

}
