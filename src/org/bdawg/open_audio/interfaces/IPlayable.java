package org.bdawg.open_audio.interfaces;

import java.util.List;


public interface IPlayable {
	
	/**
	 * Used to get the next {@code}ISinglePlayable that {@code}SlaveManager will start playing.
	 * Ideally, the slaves will have already downloaded this file. 
	 * @return The next SinglePlayable in the playlist
	 */
	public ISinglePlayable getNextSinglePlayable();
	
	/**
	 * The master that is responsible for this {@code}IPlayable.
	 * @return The masterId for this Playable
	 */
	public String getMasterClientId();
	
	/**
	 * The ID of this playable.
	 * @return The id of this playable
	 */
	public String getId();
	
	/**
	 * Used to find out if the master needs to download and distribute the file, or is a slave can do so on it's own.
	 * We use things for things like pandora, 8tracks, etc where we need only fetch once and distribute on our own.
	 * @return boolean indicating if it's safe to let all slaves download on their own, or if we should only download from source once.
	 */
	public boolean needsDistribute();
	
	/**
	 * Returns a list of the client IDs we care about for this playable.
	 * @return List<String> clients
	 */
	public List<String> getClients();
	
	public String getDownloadType();
}
