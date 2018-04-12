package fileShare;

import configuration.*;
import message.*;

public class P2P extends Thread{
	private Common common;
	private PeerInfo peerinfo;
	private SyncInfo syncinfo;
	private int hostID;
	private int neighborID;
	
	public P2P(Common common, PeerInfo peerinfo, SyncInfo syncinfo, int hostID, int neighborID) {
		this.common=common;
		this.peerinfo=peerinfo;
		this.syncinfo=syncinfo;
		this.hostID=hostID;
		this.neighborID=neighborID;
	}
	
	public void run() {
		
	}

}