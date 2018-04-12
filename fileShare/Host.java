package fileShare;

import java.io.IOException;
import java.util.BitSet;

import configuration.*;
import fileProcess.*;

public class Host extends Thread {
	/* peer information */
	private int hostID;
	private int hostIndex;
	private int portNumber;
	private boolean hasFile;
	private int numberofpieces; 		// number of pieces
	private int numberofpeers; 			// number of total peers
	private BitSet bitfield; 			// file status in bits
	private BitSet completedPeers; 		// indicates if a peer has the complete file
	private Common common; 				// common configurations
	private PeerInfo peerinfo;
	private FileProcess fp;

	public Host(int peerID, Common common, PeerInfo peerinfo) {
		this.common = common;
		this.peerinfo = peerinfo;
		hostID = peerID;
		hostIndex = peerinfo.Indexof(peerID);
		portNumber = peerinfo.PortNumberof(peerID);
		hasFile = peerinfo.HasFile(peerID);
		numberofpeers = peerinfo.getAmount();
		numberofpieces = common.getAmount();
		bitfield = new BitSet(numberofpieces);
		completedPeers = new BitSet(numberofpeers);
		for (int i = 0; i < numberofpeers; i++) {
			if (peerinfo.HasFile(i))
				completedPeers.set(i);
		}
		try {
			fp = new FileProcess(hostID, common);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (hasFile) {
			bitfield.flip(0, numberofpieces);
			fp.split();
		}
	}

	public void run() {

		// first connect to all the peers listed before host in PeerInfo.cfg
		for (int i = 0; i < hostIndex; i++) {
			connect(i,peerinfo);
			System.out.println("Now connected to peer " + i);
		}
		VirtualServer vs = new VirtualServer();
		vs.start();

		while (true) {
			try {
				sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("Current completed peers: " + completedPeers);
			synchronized (completedPeers) {
				if (completedPeers.nextClearBit(0) >= numberofpeers)
					break;
			}
		}

	}

	private class VirtualServer extends Thread {

		public void run() {

		}
	}

	private void connect(int index, PeerInfo peerinfo) {

	}

}
