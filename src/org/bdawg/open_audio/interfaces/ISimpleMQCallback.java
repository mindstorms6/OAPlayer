package org.bdawg.open_audio.interfaces;

import java.nio.ByteBuffer;

public interface ISimpleMQCallback {

	public void messageArrived(String topic, ByteBuffer message);

}
