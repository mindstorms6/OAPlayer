package org.bdawg.open_audio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Map;

import org.bdawg.open_audio.OpenAudioProtos.ClientCommand;
import org.bdawg.open_audio.OpenAudioProtos.ClientCommand.ClientAction;
import org.bdawg.open_audio.OpenAudioProtos.MasterCommand;
import org.bdawg.open_audio.OpenAudioProtos.MasterCommand.MasterAction;
import org.bdawg.open_audio.OpenAudioProtos.MasterPlayable;
import org.bdawg.open_audio.OpenAudioProtos.SinglePBItem;
import org.bdawg.open_audio.OpenAudioProtos.Sync;
import org.bdawg.open_audio.Utils.OAConstants;
import org.bdawg.open_audio.file_manager.FileManager;
import org.bdawg.open_audio.interfaces.IPlayable;
import org.bdawg.open_audio.interfaces.IPlayer;
import org.bdawg.open_audio.interfaces.ISender;
import org.bdawg.open_audio.interfaces.ISimpleMQCallback;
import org.bdawg.open_audio.interfaces.ISinglePlayable;
import org.bdawg.open_audio.playables.SimpleURIPlayable;
import org.bdawg.open_audio.sntp.TimeManager;
import org.bdawg.open_audio.sources.MalformedMetaException;
import org.bdawg.open_audio.sources.YoutubeSource;
import org.bdawg.open_audio.webObjects.Progress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.turn.ttorrent.client.SharedTorrent;
import com.turn.ttorrent.common.Torrent;
import com.turn.ttorrent.tracker.TrackedTorrent;

public class MasterManager implements ISimpleMQCallback {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ISender sender;
	private IPlayable masterPlayable;
	private ISinglePlayable next;
	private Thread sendProgressThread;
	private IPlayer player;

	public MasterManager(ISender sender, IPlayer player) {
		this.sender = sender;
		this.player = player;
	}

	@Override
	public void messageArrived(String topic, ByteBuffer message) {
		try {
			MasterCommand incoming = MasterCommand.parseFrom(message.array());
			if (MasterAction.NEW_PLAYABLE.equals(incoming.getMasterAction())) {
				MasterPlayable playableToGet = incoming.getPlayable();
				Map<String, String> playableMeta = Utils
						.kvListToMap(playableToGet.getMetaList());
				IPlayable toManage = null;
				if (playableToGet.getPlaybackType().equals(
						Utils.OAConstants.DL_TYPE_SIMPLE_URL)) {
					// URI Type to play on all pb hosts in getClientIds;
					toManage = new SimpleURIPlayable(playableToGet.getId(),
							Runner.myMacAddress,
							playableToGet.getClientIdList(), playableMeta);
				} else if (playableToGet.getPlaybackType().equals(
						Utils.OAConstants.DL_TYPE_FS)) {
					// FileSysstem resource, local to this hosts presumably
				} else if (playableToGet.getPlaybackType().equals(
						Utils.OAConstants.PB_TYPE_YOUTUBE)) {
					toManage = new YoutubeSource(playableToGet.getId(),
							Runner.myMacAddress,
							playableToGet.getClientIdList(), playableMeta);
				}
				if (toManage != null) {
					this.masterManagePlayable(toManage);
				}

			} else {
				log.warn("Ummm Unknown master action?");
			}
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.warn("Invalid Message on master topic!");
		} catch (MalformedMetaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.warn("Could not form valid playable from incoming message");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void init() {

	}

	public void masterManagePlayable(IPlayable playable) throws InterruptedException, IOException, URISyntaxException {
		this.masterPlayable = playable;
		this.player.setHasSyncLock(true);
		if (this.masterPlayable.needsDistribute()) {
			ISinglePlayable toDistribute = this.masterPlayable
					.getNextSinglePlayable();
			File toShare = toDistribute.getToDistribute();
			File torrentFile = new File(FileManager.getTorrentDirectory(),FileManager.torrentFileNameFromSP(toDistribute));
			Torrent tFile = Torrent.create(toShare, new URI("udp://tracker.openbittorrent.com:80/announce"), Runner.myMacAddress);
			tFile.save(new FileOutputStream(torrentFile));
			doPlayOnAll(toDistribute, torrentFile);
			// download
			// make torrent
			// upload torrrent
			// notify
			throw new RuntimeException("Not implemented yet");
		} else {
			// Looks like these ISinglePlayables can be downloaded directly by
			// slave
			// No need to distribute
			ISinglePlayable nextToPlay = this.masterPlayable
					.getNextSinglePlayable();
			if (nextToPlay != null) {
				doPlayOnAll(nextToPlay,null);
			} else {
				log.info("getNextSinglePlayable retuned null. Assuming end of Playlist.");
			}
		}

	}

	/**
	 * This method gets invoked by the "slave" on the device that's also the
	 * master for the "IPlayable" For all other slaves that aren't masters, this
	 * is a no-op
	 */
	public void singlePlayableCallback() {
		if (this.next != null) {
			doPlayOnAll(next, null);
		} else {
			log.info("Not telling clients to play anything, as next was null");
		}
	}

	/**
	 * THis gets called when playback is about to end (30 seconds out) Gives a
	 * chance for the client to start downloading so the transititon is
	 * seemless.
	 */
	public void aboutToEndPlayableCallback() {
		ISinglePlayable nextToPlay = this.masterPlayable
				.getNextSinglePlayable();
		this.next = nextToPlay;
		if (nextToPlay != null) {
			this.doDownloadOnAll(nextToPlay);
		} else {
			log.info("getNextSinglePlayable returned null. Not telling clients to download anything.");
		}
	}

	public void doDownloadOnAll(ISinglePlayable toDownloadOnAll) {
		ClientCommand.Builder b = ClientCommand
				.newBuilder()
				.setClientAction(ClientAction.DOWNLOAD)
				.setItem(
						SinglePBItem
								.newBuilder()
								.setDirectPlaybackFlag(
										toDownloadOnAll.canVLCPlayDirect())
								.setMasterId(Runner.myMacAddress)
								.setSubIndex(toDownloadOnAll.getSubIndex())
								.addAllMeta(
										Utils.mapToKVList(toDownloadOnAll
												.getMeta()))
								.setDlType(
										this.masterPlayable.getDownloadType())
								.setOwningPBId(this.masterPlayable.getId()));

		ByteBuffer toSend = ByteBuffer.wrap(b.build().toByteArray());
		for (String clientID : this.masterPlayable.getClients()) {
			this.sender.sendToTopic(toSend, OAConstants.BASE_TOPIC + clientID);
		}
	}

	public void doPlayOnAll(ISinglePlayable toPlayOnAll, File toDistributeTorrent) {
		ClientCommand.Builder b = ClientCommand
				.newBuilder()
				.setClientAction(ClientAction.PLAY)
				.setItem(
						SinglePBItem
								.newBuilder()
								.setDirectPlaybackFlag(
										toPlayOnAll.canVLCPlayDirect())
								.setMasterId(Runner.myMacAddress)
								.setSubIndex(toPlayOnAll.getSubIndex())
								.addAllMeta(
										Utils.mapToKVList(toPlayOnAll.getMeta()))
								.setDlType(
										this.masterPlayable.getDownloadType())
								
								.setOwningPBId(this.masterPlayable.getId()))
				.setTimestamp(
						TimeManager.getTMInstance().getCurrentTimeMillis() + 5000);
		
		if (toDistributeTorrent != null){
			try {
				ByteString fileBS = ByteString.readFrom(new FileInputStream(toDistributeTorrent));
				b.getItemBuilder().setTorrentBytes(fileBS);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		ByteBuffer toSend = ByteBuffer.wrap(b.build().toByteArray());
		for (String clientID : this.masterPlayable.getClients()) {
			this.sender.sendToTopic(toSend, OAConstants.BASE_TOPIC + clientID);
		}
	}

	public void broadcastProgress(Progress p) {
		if (p != null) {
			ClientCommand b = ClientCommand
					.newBuilder()
					.setClientAction(ClientAction.SYNC)
					.setSync(
							Sync.newBuilder()
									.setMasterElapsed(p.getProgressTime())
									.setMasterNTP(p.getNTPTime())
									.setOwningPBId(p.getItemUUID())
									.setSubIndex(p.getSubIndex())).build();
			ByteBuffer toSend = ByteBuffer.wrap(b.toByteArray());
			for (String clientID : this.masterPlayable.getClients()) {
				if (!clientID.equals(this.masterPlayable.getMasterClientId())) {
					this.sender.sendToTopic(toSend, OAConstants.BASE_TOPIC
							+ clientID);
				}
			}
		}
	}
}
