package fileShare;

import java.util.concurrent.ConcurrentHashMap;

import configuration.*;
import message.*;

public class P2P extends Thread {
	private volatile boolean running;
	private Common common;
	private PeerInfo peerinfo;
	private SyncInfo syncinfo;
	private int neighborID;
	private int neighborIndex;
	private Neighbor neighbor;
	private int hostID;
	private boolean ischoked;
	private ConcurrentHashMap<Integer, Integer> downloadRate;
	private ConcurrentHashMap<Integer, Neighbor> neighborInfo;

	public P2P(Common common, PeerInfo peerinfo, SyncInfo syncinfo, int hostID, int neighborIndex, ConcurrentHashMap<Integer, Integer> downloadRate,
	ConcurrentHashMap<Integer, Neighbor> neighborInfo) {
		this.common = common;
		this.peerinfo = peerinfo;
		this.syncinfo = syncinfo;
		this.hostID = hostID;
		this.neighborIndex = neighborIndex;
		neighborID=peerinfo.getPeerID(neighborIndex);
		neighbor=neighborInfo.get(neighborIndex);
		ischoked=true;
		this.requested=requested;
		running=true;
	}

	public void run() {
		

	}

	public void stopRunning() {
		running = false;
	}

}