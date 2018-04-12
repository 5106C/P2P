package message;

import java.io.Serializable;

public class ActualMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int length;
	private int type;
	private byte[] payload;
	private byte[] index;

	public ActualMessage(int l, int t, byte[] pl) {
		length = l;
		type = t;
		payload = pl;
	}
	
	public ActualMessage(int l, int t, byte[] pl, byte[] in) {
		length = l;
		type = t;
		payload = pl;
		index= in;
	}
	
	public int getLength(){
		return length;
	}
	
	public int getType(){
		return type;
	}
	
	public byte[] getPayload(){
		return payload;
	}
	
	public byte[] getIndex(){
		return index;
	}
}
