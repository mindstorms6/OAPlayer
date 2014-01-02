package org.bdawg.open_audio.interfaces;

import java.nio.ByteBuffer;

public interface ISender {

	public void sendToPeers(ByteBuffer message);
	
	public void sendToTopic(ByteBuffer message, String topic);
}
