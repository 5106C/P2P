package fileShare;

import java.util.*;
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
	private boolean handshakefirst;

	public P2P(Common common, PeerInfo peerinfo, SyncInfo syncinfo, int hostID, int neighborIndex,
			ConcurrentHashMap<Integer, Integer> downloadRate, ConcurrentHashMap<Integer, Neighbor> neighborInfo,
			boolean handshakefirst) {
		this.common = common;
		this.peerinfo = peerinfo;
		this.syncinfo = syncinfo;
		this.hostID = hostID;
		this.neighborIndex = neighborIndex;
		this.neighborInfo = neighborInfo;
		this.downloadRate = downloadRate;
		neighborID = peerinfo.getPeerID(neighborIndex);
		neighbor = this.neighborInfo.get(neighborIndex);
		this.handshakefirst = handshakefirst;
		ischoked = true;
		running = true;
	}

	public void stopRunning() {
		running = false;
	}

	public void run() {
		if (handshakefirst) {
			creatnewhandshake();
			System.out.println(hostID + "send my handshake to " + neighborID); // neighborID, peerID
		}

		while (running) {
			Object receivedMessage = null;
			receivedMessage = neighbor.receive();
			if (receivedMessage instanceof HandShake) {
				HandShake hs=(HandShake) receivedMessage;
				if (!handshakeCheck(hs)) {
					break;
				}
				if (!handshakefirst) {
					creatnewhandshake();
					System.out.println(hostID + " send handshake to " + neighborID);
				}
				int type = 5;
				byte[] payload = bits2byte(syncinfo.getBitfield());
				int length = payload.length;
				ActualMessage bitfield = new ActualMessage(length, type, payload);
				sendMsg(bitfield);
			}

			if (receivedMessage instanceof ActualMessage) {
				ActualMessage msg = (ActualMessage) receivedMessage;
				//choke
				if (msg.getType() == 0) {
					ischoked = true;
					String log="to do";
					writelog(log);
				}
				//unchoke
				else if (msg.getType() == 1) {
					ischoked = false;
					if(!syncinfo.interest(neighborIndex)) break;
					int pieceIndex = choosePiece();
					byte[] payload = int2byte(pieceIndex);
					int type=6;
					int length=1+4;
					ActualMessage request = new ActualMessage(length, type, payload);
					sendMsg(request);
					String log="to do";
					writelog(log);
				}
				//interested
				else if (msg.getType() == 2) {
					syncinfo.updateInterested(neighborIndex, true);
					String log="to do";
					writelog(log);
				}
				//not interested
				else if (msg.getType() == 3) {
					syncinfo.updateInterested(neighborIndex, false);
					String log="to do";
					writelog(log);
				}
				//have
				else if(msg.getType() == 4) {
					int pieceIndex=byte2int(msg.getPayload());
					neighbor.updateBitfield(pieceIndex);
					if(neighbor.isComplete()) syncinfo.updateCompletedPeers(neighborIndex);
					if(!syncinfo.haspiecie(pieceIndex)) {
						ActualMessage interested=new ActualMessage(1,2,null);						
						sendMsg(interested);
					}
				}
				//bitfield
				else if(msg.getType() == 5) {
					BitSet neighborBF=byte2bits(msg.getPayload());
					neighbor.setBitfield(neighborBF);
					if(checkBitfield()) {
						syncinfo.updateInterest(neighborIndex, true);
						ActualMessage interested=new ActualMessage(1,2,null);						
						sendMsg(interested);
					}
					else {
						syncinfo.updateInterest(neighborIndex, false);
						ActualMessage notInterested=new ActualMessage(1,3,null);						
						sendMsg(notInterested);
					}
				}
				
				else if(msg.getType() == 6) {
					
				}
			}
		}
	}
	
	private boolean checkBitfield() {
		BitSet hostBF=syncinfo.getBitfield();
		BitSet neighborBF=neighbor.getBitfield();
		hostBF.and(neighborBF);
		if(hostBF.equals(neighborBF)) return false;
		return true;
	}
	

	private boolean handshakeCheck(HandShake hs) {
		// TODO Auto-generated method stub

	}

	private void creatnewhandshake() {
		// TODO Auto-generated method stub

	}

	private int choosePiece() {

	}

	private byte[] int2byte(int i) {

	}

	private int byte2int(byte[] b) {

	}

	private byte[] bits2byte(BitSet bs) {

	}
	
	private BitSet byte2bits(byte[] b) {
		
	}

	private void sendMsg(ActualMessage msg) {

	}

	private void writelog(String s) {

	}

}