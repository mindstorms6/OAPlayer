package org.bdawg.open_audio.vlc;

import java.awt.Dimension;

import javax.swing.JFrame;

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

public class VLCManager implements IPlayer{

	private final Logger log = LoggerFactory.getLogger(VLCManager.class);
    private final EmbeddedMediaPlayerComponent mediaPlayerComponent;
    private JFrame frame;
    private IPlayItem currentItem;
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
			// TODO Auto-generated method stub
			
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
		public void mediaDurationChanged(MediaPlayer mediaPlayer, long newDuration) {
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
	
	public VLCManager() {
		if (RuntimeUtil.isWindows()){
			NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(),
					"C:\\Program Files\\VideoLAN\\VLC");
		}
		if (RuntimeUtil.isMac()){
			NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(),
					"/Applications/VLC.app/Contents/MacOS");
		}
		if (RuntimeUtil.isNix()){
			NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(),
					"/usr/lib/");
		}
		
		
		Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
        frame = new JFrame("OpenAudio");
        mediaPlayerComponent = new EmbeddedMediaPlayerComponent();

        frame.setContentPane(mediaPlayerComponent);

        frame.setLocation(0, 0);
        Dimension d = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(d.width, d.height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);


        mediaPlayerComponent.getMediaPlayer().setFullScreen(true);

        mediaPlayerComponent.getMediaPlayer().addMediaPlayerEventListener(listener);
	}
	
	@Override
	public void setVolume(int volume){
		mediaPlayerComponent.getMediaPlayer().setVolume(volume);
	}
	
	public int getVolume(){
		return mediaPlayerComponent.getMediaPlayer().getVolume();
	}
	
	public Progress getPlaybackProgress(){
		
		Progress p = new Progress();
		if (currentItem != null){
			p.setItemUUID(currentItem.getItemId().toString());
			p.setTotalItemTime(mediaPlayerComponent.getMediaPlayer().getLength());
			p.setProgressTime(mediaPlayerComponent.getMediaPlayer().getTime());
			p.setNTPTime(TimeManager.getTMInstance().getCurrentTimeMillis());
		}
		return p;
	}

	@Override
	public void play(long ntpTime, IPlayItem toPlay) {
		currentItem = toPlay;
		if (ntpTime <= TimeManager.getTMInstance().getCurrentTimeMillis())
		{
			log.warn("Shit, we're late! Compensating by skipping!");
			//compensate...
			mediaPlayerComponent.getMediaPlayer().playMedia(toPlay.getFile().getAbsolutePath());
			mediaPlayerComponent.getMediaPlayer().skip(TimeManager.getTMInstance().getCurrentTimeMillis() - ntpTime);		
		} else {
			log.debug("We're early, watiting for a bit...");
			try {
				Thread.sleep(Math.abs(TimeManager.getTMInstance().getCurrentTimeMillis() - ntpTime));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				mediaPlayerComponent.getMediaPlayer().playMedia(toPlay.getFile().getAbsolutePath());		
			}
			
		}
	}

	@Override
	public void pause(long ntpTime) {
		if (ntpTime <= TimeManager.getTMInstance().getCurrentTimeMillis())
		{
			
			//compensate...
			mediaPlayerComponent.getMediaPlayer().pause();
			mediaPlayerComponent.getMediaPlayer().skip(TimeManager.getTMInstance().getCurrentTimeMillis() - ntpTime);		
		} else {
			try {
				Thread.sleep(Math.abs(TimeManager.getTMInstance().getCurrentTimeMillis() - ntpTime));
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
		if (ntpTime <= TimeManager.getTMInstance().getCurrentTimeMillis())
		{
			mediaPlayerComponent.getMediaPlayer().stop();
		} else {
			try {
				Thread.sleep(Math.abs(TimeManager.getTMInstance().getCurrentTimeMillis() - ntpTime));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				mediaPlayerComponent.getMediaPlayer().stop();
			}
			
		}
		
	}
}
