package fileShare;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;

import configuration.*;
import fileProcess.*;

public class Host extends Thread {
	/* peer information */
	private int hostID;
	private int hostIndex;
	private int portNumber;
	private boolean hasFile; // the initial file status of the host
	private int numberofpeers; // number of total peers
	private Common common; // common configurations
	private PeerInfo peerinfo;
	private SyncInfo syncinfo;
	private ConcurrentHashMap<Integer, Integer> downloadRate;
	private ConcurrentHashMap<Integer, Neighbor> neighborInfo;
	private FileProcess fp;

	public Host(int peerID, Common common, PeerInfo peerinfo) {
		this.common = common;
		this.peerinfo = peerinfo;
		syncinfo = new SyncInfo(common, peerinfo);
		downloadRate = new ConcurrentHashMap<>();
		neighborInfo = new ConcurrentHashMap<>();
		hostID = peerID;
		hostIndex = peerinfo.Indexof(peerID);
		portNumber = peerinfo.PortNumberof(hostIndex);
		hasFile = peerinfo.HasFile(hostIndex);
		numberofpeers = peerinfo.getAmount();
		for (int i = 0; i < numberofpeers; i++) {
			if (peerinfo.HasFile(i))
				syncinfo.updateCompletedPeers(i);
		}
		try {
			fp = new FileProcess(hostID, common);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (hasFile) {
			syncinfo.flipBitfield();
			fp.split();
		}
	}

	public void run() {

		// first connect to all the peers listed before host in PeerInfo.cfg
		for (int i = 0; i < hostIndex; i++) {
			connect(i);
		}
		Choke choke = new Choke(downloadRate, neighborInfo, syncinfo, common, hostID, peerinfo);
		choke.start();
		VirtualServer vs = new VirtualServer();
		vs.start();
		while (true) {
			syncinfo.resetRequested();
			try {
				sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("Current completed peers: " + syncinfo.getCompletedPeers());
			if (syncinfo.allComplete()) {
				System.out.println("mission complete! please wait till programs end.");
				break;
			}
		}
		try {
			sleep(3000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		choke.stopRunning();
		vs.stopRunning();
		for (Neighbor n : neighborInfo.values())
			n.closeConnection();
		if (!hasFile)
			fp.rebuild();
		fp.delete();
		try {
			sleep(3000);
			System.out.println("game over!");
			System.exit(0);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private class VirtualServer extends Thread {
		private volatile boolean running;
		private ServerSocket listener;
		private int numberofconnected; // number of connected peers

		VirtualServer() {
			running = true;
			numberofconnected = hostIndex;
		}

		public void run() {
			System.out.println("The server is running.");
			try {
				listener = new ServerSocket(portNumber);
				while (running) {
					Socket connection = listener.accept();
					ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
					out.flush();
					ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
					int neighborIndex = ++numberofconnected;
					Neighbor neighbor = new Neighbor(common, peerinfo, neighborIndex, connection, in, out);
					downloadRate.put(neighborIndex, 0);
					neighborInfo.put(neighborIndex, neighbor);
					P2P p2p = new P2P(common, peerinfo, syncinfo, hostID, neighborIndex, downloadRate,
							neighborInfo,false);
					neighbor.setP2P(p2p);
					p2p.start();
					System.out.println("Peer "+hostID+" is connected from peer "+ peerinfo.getPeerID(neighborIndex));
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void stopRunning() {
			running = false;
			try {
				listener.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
//				e.printStackTrace();
			}
		}

	}

	private void connect(int index) {
		Socket requestSocket = null; // socket connect to the server
		ObjectOutputStream out = null; // stream write to the socket
		ObjectInputStream in = null;
		int peerID = peerinfo.getPeerID(index);
		String peerHostName = peerinfo.HostNameof(index);
		int peerPort = peerinfo.PortNumberof(index);

		try {
			requestSocket = new Socket(peerHostName, peerPort);
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());
		} catch (ConnectException e) {
			System.err.println("Connection failed! start up the destination peer first!");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Neighbor neighbor = new Neighbor(common, peerinfo, index, requestSocket, in, out);
		downloadRate.put(index, 0);
		neighborInfo.put(index, neighbor);
		P2P p2p = new P2P(common, peerinfo, syncinfo, hostID, index, downloadRate, neighborInfo, true);
		neighbor.setP2P(p2p);
		p2p.start();
		System.out.println("peer "+hostID+"makes a connection to peer " + peerID);
	}
}
