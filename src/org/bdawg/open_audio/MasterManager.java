package org.bdawg.open_audio;

import java.nio.ByteBuffer;
import java.util.Map;

import org.bdawg.open_audio.OpenAudioProtos.ClientCommand;
import org.bdawg.open_audio.OpenAudioProtos.ClientCommand.ClientAction;
import org.bdawg.open_audio.OpenAudioProtos.MasterCommand;
import org.bdawg.open_audio.OpenAudioProtos.MasterCommand.MasterAction;
import org.bdawg.open_audio.OpenAudioProtos.MasterPlayable;
import org.bdawg.open_audio.OpenAudioProtos.SinglePBItem;
import org.bdawg.open_audio.Utils.OAConstants;
import org.bdawg.open_audio.exceptions.MalformedMetaException;
import org.bdawg.open_audio.interfaces.IPlayable;
import org.bdawg.open_audio.interfaces.ISender;
import org.bdawg.open_audio.interfaces.ISimpleMQCallback;
import org.bdawg.open_audio.interfaces.ISinglePlayable;
import org.bdawg.open_audio.playables.SimpleURIPlayable;
import org.bdawg.open_audio.sntp.TimeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

public class MasterManager implements ISimpleMQCallback {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ISender sender;
	private IPlayable masterPlayable;
	private ISinglePlayable next;

	public MasterManager(ISender sender) {
		this.sender = sender;
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
		}

	}

	public void init() {

	}

	public void masterManagePlayable(IPlayable playable) {
		this.masterPlayable = playable;
		if (this.masterPlayable.needsDistribute()) {

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
				doPlayOnAll(nextToPlay);
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
			doPlayOnAll(next);
		} else {
			log.info("Not telling clients to play anything, as next was null");
		}
	}

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

	public void doPlayOnAll(ISinglePlayable toPlayOnAll) {
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
		ByteBuffer toSend = ByteBuffer.wrap(b.build().toByteArray());
		for (String clientID : this.masterPlayable.getClients()) {
			this.sender.sendToTopic(toSend, OAConstants.BASE_TOPIC + clientID);
		}
	}
}
