package message;

import java.io.Serializable;

public class HandShake implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String header;
	private byte[] zerobits;
	private int peerID;

	public HandShake(String hdr, int pID) {
		header = hdr;
		zerobits = new byte[10];
		peerID = pID;
	}

	public String getHeader() {
		return header;
	}

	public byte[] getZerobits() {
		return zerobits;
	}

	public int getPeerID() {
		return peerID;
	}

}
