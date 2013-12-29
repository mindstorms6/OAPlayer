package org.bdawg.open_aduio.file_manager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.bdawg.open_audio.OpenAudioProtos.PlayItem;
import org.bdawg.open_audio.Utils;
import org.bdawg.open_audio.interfaces.IPlayItem;

import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;

public class Playable implements IPlayItem {

	private File downloaded;
	private Client torrentClient;
	private UUID myId;
	private SharedTorrent torrent;

	public Playable(File f) {
		// presumably, the file is downloaded
		this.downloaded = f;
		this.myId = UUID.fromString(f.getName());
	}

	public Playable(UUID id) throws IOException {
		this.myId = id;
		File mightExist = new File(FileManager.getDownloadsDir(), id.toString());
		if (mightExist.exists() && mightExist.canRead()) {
			// cool!
			downloaded = mightExist;
		} else {
			// TODO:need to start the torrent
		}
	}

	public Playable(SharedTorrent t) throws UnknownHostException, IOException {
		this.torrent = t;
		this.myId = UUID.fromString(t.getName().replace(".torrent", ""));
		this.torrentClient = new Client(Utils.getLocalInetAddress(),
				this.torrent);
		this.torrentClient.download();

	}

	@Override
	public File getFile() {
		if (this.getTorrentClient() != null) {
			this.getTorrentClient().waitForCompletion();
		}
		return this.downloaded;
	}

	@Override
	public Client getTorrentClient() {
		return this.torrentClient;
	}

	@Override
	public UUID getItemId() {
		return this.myId;
	}

	public static Playable fromPlayItem(PlayItem item) throws IOException {
		File destFile = new File(FileManager.getDownloadsDir(),
				item.getItemId());
		if (!destFile.exists()) {
			destFile.createNewFile();
			destFile.setWritable(true, true);
		}
		Playable tr = null;

		switch (item.getPlayType()) {
		case BYTES:
			if (item.hasItemBytes()) {
				item.getItemBytes().writeTo(new FileOutputStream(destFile));
			}
			tr = new Playable(destFile);
			break;
		case INET_URI:
			FileUtils.copyURLToFile(new URL(item.getItemUri()), destFile);
			tr = new Playable(destFile);
			break;
		case TORRENT:
			SharedTorrent t = new SharedTorrent(item.getTorrentBytes()
					.toByteArray(), destFile);
			tr = new Playable(t);
			tr.myId = UUID.fromString(item.getItemId());
			break;
		default:
			break;
		}
		return tr;
	}

}
