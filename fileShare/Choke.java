package fileShare;

import java.util.concurrent.ConcurrentHashMap;

import configuration.*;

public class Choke extends Thread {
	private volatile boolean running;

	public Choke(ConcurrentHashMap<Integer, Integer> downloadRate, ConcurrentHashMap<Integer, Neighbor> neighborInfo,
			SyncInfo syncinfo, Common common, int hostID) {
		
	}
	
	public void run() {
		
	}
	
	public void stopRunning() {
		running=false;
	}

}
