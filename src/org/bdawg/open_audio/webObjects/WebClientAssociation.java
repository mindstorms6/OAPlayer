package org.bdawg.open_audio.webObjects;

/**
 * Created by breland on 12/19/13.
 */
public class WebClientAssociation {
    public String clientId;
    public String groupId;
    public String groupName;

    public WebClientAssociation(){
    	this.clientId = "";
    	this.groupId="";
    	this.groupName="";
    }
    
    public WebClientAssociation(String clientId, String groupId, String groupName){
    	this.clientId = clientId;
    	this.groupId = groupId;
    	this.groupName = groupName;
    }

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

}
