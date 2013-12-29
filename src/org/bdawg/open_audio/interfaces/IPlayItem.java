package org.bdawg.open_audio.interfaces;

import java.io.File;
import java.util.UUID;

import com.turn.ttorrent.client.Client;

public interface IPlayItem {
	File getFile();
	Client getTorrentClient();
	UUID getItemId();
	
}
