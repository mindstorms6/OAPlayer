package org.bdawg.open_audio.mqtt;

import java.nio.ByteBuffer;

public interface ISimpleMQCallback {

	public void messageArrived(String topic, ByteBuffer message);

}
