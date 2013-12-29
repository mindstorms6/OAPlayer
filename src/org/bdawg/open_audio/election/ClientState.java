package org.bdawg.open_audio.election;

public enum ClientState {

	MASTER,
	SLAVE,
	ELECTING,
	DECONFLICTING,
	STARTUP
}
