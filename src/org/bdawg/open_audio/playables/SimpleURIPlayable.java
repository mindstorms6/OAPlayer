package org.bdawg.open_audio.playables;

import java.util.List;
import java.util.Map;

import org.bdawg.open_audio.Utils;
import org.bdawg.open_audio.Utils.OAConstants;
import org.bdawg.open_audio.exceptions.MalformedMetaException;
import org.bdawg.open_audio.impl.AbstractPlayable;
import org.bdawg.open_audio.interfaces.ISinglePlayable;

public class SimpleURIPlayable extends AbstractPlayable{
	
	private String mediaLocation;
	private ISinglePlayable lastReturned;
	private boolean canPlayDirect;
	
	public SimpleURIPlayable(String id, String masterId, List<String> clients,  Map<String,String> params) throws MalformedMetaException{
		super(id, masterId, false, clients, params);
		
		if (this.getMeta() == null){
			throw new MalformedMetaException();
		}
		if (!this.getMeta().containsKey(OAConstants.META_SIMPLE_URL_LOCATION_KEY)){
			throw new MalformedMetaException();
		} else {
			mediaLocation = this.getMeta().get(OAConstants.META_SIMPLE_URL_LOCATION_KEY);
		}
		
		if (!this.getMeta().containsKey(OAConstants.META_SIMPLE_URL_CAN_PLAY_DIRECT_KEY)){
			throw new MalformedMetaException();
		} else {
			canPlayDirect = this.getMeta().get(OAConstants.META_SIMPLE_URL_CAN_PLAY_DIRECT_KEY).equals(OAConstants.TRUE_STRING) ? true : false;
		}
	}
	
	public String getLocation(){
		return mediaLocation;
	}

	@Override
	public ISinglePlayable getNextSinglePlayable() {
		if (lastReturned == null){
			ISinglePlayable next = new ISinglePlayable() {
				
				@Override
				public int getSubIndex() {
					//Only one item for SimpleURI playables
					return 0;
				}
				
				@Override
				public String getMasterId() {
					//Refer to the super
					return SimpleURIPlayable.this.getMasterClientId();
				}

				@Override
				public boolean canVLCPlayDirect() {
					return SimpleURIPlayable.this.canPlayDirect;
				}
				
				@Override
				public Map<String,String> getMeta(){
					return SimpleURIPlayable.this.getMeta();
				}

				@Override
				public String getDLType() {
					return SimpleURIPlayable.this.getDownloadType();
					
				}

				@Override
				public String getOwningPlayableId() {
					return SimpleURIPlayable.this.getId();
				}
			};
			lastReturned=next;
			return next;
		} else {
			return null;
		}
	}

	@Override
	public String getDownloadType() {
		return OAConstants.DL_TYPE_SIMPLE_URL;
	}
}
