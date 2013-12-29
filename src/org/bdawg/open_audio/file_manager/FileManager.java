package org.bdawg.open_audio.file_manager;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bdawg.open_audio.interfaces.IPlayItem;

import com.turn.ttorrent.client.SharedTorrent;

public abstract class FileManager {

	private static Map<String, IPlayItem> items;
	private static File tmpDirectory = new File(
			System.getProperty("java.io.tmpdir"));
	private static File downloadDirectory;
	private static File torrentsDirectory;
	private static boolean inited = false;

	public static File getDownloadsDir() throws IOException {
		checkInit();
		return downloadDirectory;
	}

	public static File getTorrentsDir() throws IOException {
		checkInit();
		return torrentsDirectory;
	}

	private static void init() throws IOException {
		if (!(tmpDirectory.exists() && tmpDirectory.canWrite() && tmpDirectory
				.isDirectory())) {
			throw new IOException("Can't write to a download directory!");
		}
		items = new HashMap<String, IPlayItem>();
		downloadDirectory = new File(tmpDirectory, "oa_downloads");
		if (!downloadDirectory.exists()) {
			downloadDirectory.mkdir();
		}
		downloadDirectory.setWritable(true, true);
		torrentsDirectory = new File(downloadDirectory, "tor_files");
		if (!torrentsDirectory.exists()) {
			torrentsDirectory.mkdir();
		}
		torrentsDirectory.setWritable(true, true);
		File[] existringTorrentFiles = torrentsDirectory
				.listFiles(new FileFilter() {

					@Override
					public boolean accept(File pathname) {
						if (pathname.getName().endsWith(".torrent")) {
							return true;
						}
						return false;
					}
				});
		for (File f : existringTorrentFiles) {
			SharedTorrent t = SharedTorrent.fromFile(f, downloadDirectory);
			Playable p = new Playable(t);
			items.put(p.getItemId().toString(), p);
		}

		inited = true;
	}

	private static void checkInit() throws IOException {
		if (!inited) {
			init();
		}
	}

	public static FileProgress queueToDownload(IPlayItem item)
			throws IOException {
		checkInit();
		return new FileProgress();
	}

}
