package org.bdawg.open_audio.vlc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JLabel;

import org.bdawg.open_audio.interfaces.IPlayItem;
import org.bdawg.open_audio.interfaces.IPlayer;
import org.bdawg.open_audio.sntp.TimeManager;
import org.bdawg.open_audio.webObjects.Progress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;
import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventListener;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

public class VLCManager implements IPlayer {

	private final Logger log = LoggerFactory.getLogger(VLCManager.class);
	private final EmbeddedMediaPlayerComponent mediaPlayerComponent;
	private JFrame frame;
	private IPlayItem currentItem;
	private boolean hasNotifiedAboutToEnd=false;
	private Runnable aboutToEndRunnable;
	private Runnable endCallback;
	private JLabel label; 

	private MediaPlayerEventListener listener = new MediaPlayerEventListener() {

		@Override
		public void videoOutput(MediaPlayer mediaPlayer, int newCount) {
			// TODO Auto-generated method stub

		}

		@Override
		public void titleChanged(MediaPlayer mediaPlayer, int newTitle) {
			// TODO Auto-generated method stub

		}

		@Override
		public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
			if (!hasNotifiedAboutToEnd && mediaPlayer.getLength() - newTime < 30*1000){
				log.debug("Less than 30 seconds to go!!!");
				hasNotifiedAboutToEnd=true;
				Thread t = new Thread(aboutToEndRunnable);
				t.start();
			}

		}

		@Override
		public void subItemPlayed(MediaPlayer mediaPlayer, int subItemIndex) {
			// TODO Auto-generated method stub

		}

		@Override
		public void subItemFinished(MediaPlayer mediaPlayer, int subItemIndex) {
			// TODO Auto-generated method stub

		}

		@Override
		public void stopped(MediaPlayer mediaPlayer) {
			// TODO Auto-generated method stub

		}

		@Override
		public void snapshotTaken(MediaPlayer mediaPlayer, String filename) {
			// TODO Auto-generated method stub

		}

		@Override
		public void seekableChanged(MediaPlayer mediaPlayer, int newSeekable) {
			// TODO Auto-generated method stub

		}

		@Override
		public void positionChanged(MediaPlayer mediaPlayer, float newPosition) {
			// TODO Auto-generated method stub

		}

		@Override
		public void playing(MediaPlayer mediaPlayer) {
			// TODO Auto-generated method stub

		}

		@Override
		public void paused(MediaPlayer mediaPlayer) {
			// TODO Auto-generated method stub

		}

		@Override
		public void pausableChanged(MediaPlayer mediaPlayer, int newPausable) {
			// TODO Auto-generated method stub

		}

		@Override
		public void opening(MediaPlayer mediaPlayer) {
			// TODO Auto-generated method stub

		}

		@Override
		public void newMedia(MediaPlayer mediaPlayer) {
			// TODO Auto-generated method stub

		}

		@Override
		public void mediaSubItemAdded(MediaPlayer mediaPlayer,
				libvlc_media_t subItem) {
			// TODO Auto-generated method stub

		}

		@Override
		public void mediaStateChanged(MediaPlayer mediaPlayer, int newState) {
			// TODO Auto-generated method stub

		}

		@Override
		public void mediaParsedChanged(MediaPlayer mediaPlayer, int newStatus) {
			// TODO Auto-generated method stub

		}

		@Override
		public void mediaMetaChanged(MediaPlayer mediaPlayer, int metaType) {
			// TODO Auto-generated method stub

		}

		@Override
		public void mediaFreed(MediaPlayer mediaPlayer) {
			// TODO Auto-generated method stub

		}

		@Override
		public void mediaDurationChanged(MediaPlayer mediaPlayer,
				long newDuration) {
			// TODO Auto-generated method stub

		}

		@Override
		public void mediaChanged(MediaPlayer mediaPlayer, libvlc_media_t media,
				String mrl) {
			// TODO Auto-generated method stub

		}

		@Override
		public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {
			// TODO Auto-generated method stub

		}

		@Override
		public void forward(MediaPlayer mediaPlayer) {
			// TODO Auto-generated method stub

		}

		@Override
		public void finished(MediaPlayer mediaPlayer) {
			log.debug("Media finished!");
			Thread t = new Thread(endCallback);
			t.start();

		}

		@Override
		public void error(MediaPlayer mediaPlayer) {
			// TODO Auto-generated method stub

		}

		@Override
		public void endOfSubItems(MediaPlayer mediaPlayer) {
			// TODO Auto-generated method stub

		}

		@Override
		public void buffering(MediaPlayer mediaPlayer, float newCache) {
			// TODO Auto-generated method stub

		}

		@Override
		public void backward(MediaPlayer mediaPlayer) {
			// TODO Auto-generated method stub

		}
	};

	public VLCManager(Runnable aboutToEndCallback, Runnable endCallback) {
		if (RuntimeUtil.isWindows()) {
			NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(),
					"C:\\Program Files\\VideoLAN\\VLC");
		}
		if (RuntimeUtil.isMac()) {
			NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(),
					"/Applications/VLC.app/Contents/MacOS");
		}
		if (RuntimeUtil.isNix()) {
			NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(),
					"/usr/lib/");
		}

		Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
		this.endCallback = endCallback;
		this.aboutToEndRunnable = aboutToEndCallback;
		frame = new JFrame("OpenAudio");
		label = new JLabel("Fetching data");
		label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 50));
		label.setForeground(Color.white);
		
		frame.setUndecorated(true);
		mediaPlayerComponent = new EmbeddedMediaPlayerComponent();

		frame.setContentPane(mediaPlayerComponent);
		mediaPlayerComponent.add(label);
		
		Rectangle screenBounds = java.awt.GraphicsEnvironment
				.getLocalGraphicsEnvironment().getDefaultScreenDevice()
				.getDefaultConfiguration().getBounds();
		

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setBounds(screenBounds);
		frame.setAlwaysOnTop(true);
		frame.setVisible(true);

		mediaPlayerComponent.getMediaPlayer().setFullScreen(true);

		mediaPlayerComponent.getMediaPlayer().addMediaPlayerEventListener(
				listener);

	}
	
	@Override
	public void setVolume(int volume) {
		mediaPlayerComponent.getMediaPlayer().setVolume(volume);
	}

	public int getVolume() {
		return mediaPlayerComponent.getMediaPlayer().getVolume();
	}

	public Progress getPlaybackProgress() {

		Progress p = new Progress();
		if (currentItem != null) {
			p.setItemUUID(currentItem.getItemId().toString());
			p.setTotalItemTime(mediaPlayerComponent.getMediaPlayer()
					.getLength());
			p.setProgressTime(mediaPlayerComponent.getMediaPlayer().getTime());
			p.setNTPTime(TimeManager.getTMInstance().getCurrentTimeMillis());
		}
		return p;
	}

	@Override
	public void play(final long ntpTime, IPlayItem toPlay) {
		currentItem = toPlay;
		File playFile = toPlay.getFile();
		MediaPlayerEventListener timerFixer = new MediaPlayerEventListener() {

			@Override
			public void videoOutput(MediaPlayer mediaPlayer, int newCount) {
				// TODO Auto-generated method stub

			}

			@Override
			public void titleChanged(MediaPlayer mediaPlayer, int newTitle) {
				// TODO Auto-generated method stub

			}

			@Override
			public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
				// TODO Auto-generated method stub

			}

			@Override
			public void subItemPlayed(MediaPlayer mediaPlayer, int subItemIndex) {
				// TODO Auto-generated method stub

			}

			@Override
			public void subItemFinished(MediaPlayer mediaPlayer,
					int subItemIndex) {
				// TODO Auto-generated method stub

			}

			@Override
			public void stopped(MediaPlayer mediaPlayer) {
				// TODO Auto-generated method stub

			}

			@Override
			public void snapshotTaken(MediaPlayer mediaPlayer, String filename) {
				// TODO Auto-generated method stub

			}

			@Override
			public void seekableChanged(MediaPlayer mediaPlayer, int newSeekable) {
				// TODO Auto-generated method stub

			}

			@Override
			public void positionChanged(MediaPlayer mediaPlayer,
					float newPosition) {
				// TODO Auto-generated method stub

			}

			@Override
			public void playing(MediaPlayer mediaPlayer) {
				hasNotifiedAboutToEnd=false;
				long diff = TimeManager.getTMInstance().getCurrentTimeMillis()
						- ntpTime;
				if (diff > 0) {
					// behind, skip ahead
					log.debug("Behind by " + diff);
					mediaPlayer.skip(diff);
				} else if (diff < 0) {
					// early, wait
					mediaPlayer.pause();
					try {
						Thread.sleep(diff * -1);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						mediaPlayer.pause();
					}

				}
				mediaPlayer.removeMediaPlayerEventListener(this);

			}

			@Override
			public void paused(MediaPlayer mediaPlayer) {
				// TODO Auto-generated method stub

			}

			@Override
			public void pausableChanged(MediaPlayer mediaPlayer, int newPausable) {
				// TODO Auto-generated method stub

			}

			@Override
			public void opening(MediaPlayer mediaPlayer) {
				// TODO Auto-generated method stub

			}

			@Override
			public void newMedia(MediaPlayer mediaPlayer) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mediaSubItemAdded(MediaPlayer mediaPlayer,
					libvlc_media_t subItem) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mediaStateChanged(MediaPlayer mediaPlayer, int newState) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mediaParsedChanged(MediaPlayer mediaPlayer,
					int newStatus) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mediaMetaChanged(MediaPlayer mediaPlayer, int metaType) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mediaFreed(MediaPlayer mediaPlayer) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mediaDurationChanged(MediaPlayer mediaPlayer,
					long newDuration) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mediaChanged(MediaPlayer mediaPlayer,
					libvlc_media_t media, String mrl) {
				// TODO Auto-generated method stub

			}

			@Override
			public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {
				// TODO Auto-generated method stub

			}

			@Override
			public void forward(MediaPlayer mediaPlayer) {
				// TODO Auto-generated method stub

			}

			@Override
			public void finished(MediaPlayer mediaPlayer) {
				// TODO Auto-generated method stub

			}

			@Override
			public void error(MediaPlayer mediaPlayer) {
				// TODO Auto-generated method stub

			}

			@Override
			public void endOfSubItems(MediaPlayer mediaPlayer) {
				// TODO Auto-generated method stub

			}

			@Override
			public void buffering(MediaPlayer mediaPlayer, float newCache) {
				// TODO Auto-generated method stub

			}

			@Override
			public void backward(MediaPlayer mediaPlayer) {
				// TODO Auto-generated method stub

			}
		};
		mediaPlayerComponent.getMediaPlayer().addMediaPlayerEventListener(
				timerFixer);
		mediaPlayerComponent.getMediaPlayer().playMedia(
				playFile.getAbsolutePath());
		
	}

	@Override
	public void pause(long ntpTime) {
		if (ntpTime <= TimeManager.getTMInstance().getCurrentTimeMillis()) {

			// compensate...
			mediaPlayerComponent.getMediaPlayer().pause();
			mediaPlayerComponent.getMediaPlayer().skip(
					TimeManager.getTMInstance().getCurrentTimeMillis()
							- ntpTime);
		} else {
			try {
				Thread.sleep(Math.abs(TimeManager.getTMInstance()
						.getCurrentTimeMillis() - ntpTime));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				mediaPlayerComponent.getMediaPlayer().pause();
			}

		}
	}

	@Override
	public void stop(long ntpTime) {
		if (ntpTime <= TimeManager.getTMInstance().getCurrentTimeMillis()) {
			mediaPlayerComponent.getMediaPlayer().stop();
		} else {
			try {
				Thread.sleep(Math.abs(TimeManager.getTMInstance()
						.getCurrentTimeMillis() - ntpTime));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				mediaPlayerComponent.getMediaPlayer().stop();
			}

		}

	}

	@Override
	public void setOverlay(String overlay) {
		this.label.setText(overlay);
		
	}
}
