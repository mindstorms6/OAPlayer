package org.bdawg.open_audio.file_manager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.apache.http.HttpResponse;
import org.bdawg.open_audio.Utils.OAConstants;
import org.bdawg.open_audio.http_utils.HttpUtils;
import org.bdawg.open_audio.interfaces.IPlayer;
import org.bdawg.open_audio.interfaces.ISinglePlayable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;

public class FileManager {

	private static File tmpDirectory = new File(
			System.getProperty("java.io.tmpdir"));
	private static File downloadDirectory;
	private static boolean inited = false;

	private static Logger log = LoggerFactory.getLogger(FileManager.class);

	private FileManager() {

	}

	public static void playWhenReady(final long timestamp,
			ISinglePlayable aboutToPlay, final IPlayer player)
			throws IOException {
		checkInit();
		// Fuck it, send straight to VLC
		if (aboutToPlay.getDLType().equals(OAConstants.DL_TYPE_SIMPLE_URL)
				&& aboutToPlay.canVLCPlayDirect()) {
			String path = aboutToPlay.getMeta().get(
					OAConstants.META_SIMPLE_URL_LOCATION_KEY);
			player.play(timestamp, path);
		} else if (aboutToPlay.getDLType().equals(
				OAConstants.DL_TYPE_SIMPLE_URL)
				&& !aboutToPlay.canVLCPlayDirect()) {
			// Download first to file, then play
			// Check if exists already
			final String URI = aboutToPlay.getMeta().get(
					OAConstants.META_SIMPLE_URL_LOCATION_KEY);
			String fileName = aboutToPlay.getOwningPlayableId() + "_"
					+ aboutToPlay.getSubIndex();
			final File mightExist = new File(downloadDirectory, fileName);
			if (mightExist.exists() && mightExist.canRead()) {
				player.play(timestamp, mightExist.getAbsolutePath());
			} else {
				mightExist.createNewFile();
				Thread t = new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							ReadableByteChannel rbc = Channels
									.newChannel(new URL(URI).openStream());
							FileOutputStream fos = new FileOutputStream(
									mightExist);
							fos.getChannel().transferFrom(rbc, 0,
									Long.MAX_VALUE);
							fos.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
				t.start();
				Thread m = new Thread(new Runnable() {
					float expectedSize = 0;
					boolean didAttempStart = false;
					
					@Override
					public void run() {
						HttpResponse r;
						try {
							RateLimiter rater = RateLimiter.create(4);
							r = HttpUtils.executeHead(URI);
							expectedSize = Float.parseFloat(r.getHeaders("Content-Length")[0].getValue());
							while (true){
								rater.acquire();
								long currDownloaded = mightExist.length();
								float currP = (float)((float)currDownloaded / expectedSize);
								currP = (float)Math.floor(currP * 100);
								if (currP > 15 && !didAttempStart){
									player.setOverlay("");
									didAttempStart = true;
									player.play(timestamp, mightExist.getAbsolutePath());
									break;
								} else {
									player.setOverlay((int)currP+ "%");
								}
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}
				});
				m.start();
			}
		} else {
			throw new RuntimeException(
					"Not implemented anything besides simple URI downloads.");
		}

	}

	private static void init() {
		if (!(tmpDirectory.exists() && tmpDirectory.canWrite() && tmpDirectory
				.isDirectory())) {
			throw new RuntimeException("Can't write to a download directory!");
		}
		downloadDirectory = new File(tmpDirectory, "oa_downloads");
		if (!downloadDirectory.exists()) {
			downloadDirectory.mkdir();
		}
		downloadDirectory.setWritable(true, true);
		inited = true;
	}

	private static void checkInit() {
		if (!inited) {
			init();
		}
	}

	public static void queueForDownload(ISinglePlayable playable) {
		// TODO Auto-generated method stub
		if (playable.canVLCPlayDirect()) {
			// no - op
		}
	}

}
