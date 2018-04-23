package fileShare;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
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
	private int hostIndex;
	private boolean ischoked;
	private ConcurrentHashMap<Integer, Integer> downloadRate;
	private ConcurrentHashMap<Integer, Neighbor> neighborInfo;
	private boolean handshakefirst;
	private ArrayList<Integer> requestPiece;

	private String filePath;
	private String fileName;
	FileInputStream fi;
	FileOutputStream fo;

	public P2P(Common common, PeerInfo peerinfo, SyncInfo syncinfo, int hostID, int neighborIndex,
			ConcurrentHashMap<Integer, Integer> downloadRate, ConcurrentHashMap<Integer, Neighbor> neighborInfo,
			boolean handshakefirst) {
		this.common = common;
		this.peerinfo = peerinfo;
		this.syncinfo = syncinfo;
		this.hostID = hostID;
		hostIndex = peerinfo.Indexof(hostID);
		this.neighborIndex = neighborIndex;
		neighborID = this.peerinfo.getPeerID(neighborIndex);
		this.neighborInfo = neighborInfo;
		this.downloadRate = downloadRate;
		neighborID = this.peerinfo.getPeerID(neighborIndex);
		neighbor = this.neighborInfo.get(neighborIndex);
		this.handshakefirst = handshakefirst;
		ischoked = true;
		running = true;
		filePath = System.getProperty("user.dir") + File.separator + "peer_" + hostID + File.separator;
		fileName = common.getFileName();
		requestPiece = new ArrayList<>();
	}

	public void stopRunning() {
		running = false;
	}

	public void run() {
		if (handshakefirst) {
			creatnewhandshake();
			System.out.println(hostID + " send handshake to " + neighborID);
			writelog("Peer " + hostID + " makes a connection to peer " + neighborID);
		}
		while (running) {
			Object receivedMessage = null;
			receivedMessage = neighbor.receive();
			if (receivedMessage instanceof HandShake) {
				HandShake hs = (HandShake) receivedMessage;
				if (!handshakeCheck(hs)) {
					continue;
				}
				if (!handshakefirst) {
					creatnewhandshake();
					System.out.println(hostID + " send handshake to " + neighborID);
					writelog("Peer " + hostID + " is connetcted form peer " + neighborID);
				}
				int type = 5;
				byte[] payload = bits2byte(syncinfo.getBitfield());
				int length = payload.length + 1;
				ActualMessage bitfield = new ActualMessage(length, type, payload);
				sendMsg(bitfield);
			}

			if (receivedMessage instanceof ActualMessage) {
				ActualMessage msg = (ActualMessage) receivedMessage;
				// choke
				if (msg.getType() == 0) {
					ischoked = true;
					String log = "Peer " + hostID + " is choked by " + this.neighborID;
					writelog(log);
				}
				// unchoke
				else if (msg.getType() == 1) {
					ischoked = false;
					String log = "Peer " + hostID + " is unchoked by " + this.neighborID;
					writelog(log);
					int pieceIndex = choosePiece();
					byte[] payload = int2byte(pieceIndex);
					int type = 6;
					int length = 1 + 4;
					ActualMessage request = new ActualMessage(length, type, payload);
					sendMsg(request);
					// syncinfo.updateRequested(pieceIndex, true);
					String log2 = "sent a request of piece " + pieceIndex + " to peer " + neighborID;
					writelog(log2);
				}
				// interested
				else if (msg.getType() == 2) {
					syncinfo.updateInterested(neighborIndex, true);
					String log = "Peer " + hostID + " received the 'interested' messsage from " + this.neighborID;
					writelog(log);
				}
				// not interested
				else if (msg.getType() == 3) {
					syncinfo.updateInterested(neighborIndex, false);
					String log = "Peer " + hostID + " received the 'not interested' message from " + this.neighborID;
					writelog(log);
				}
				// have
				else if (msg.getType() == 4) {
					int pieceIndex = byte2int(msg.getPayload());
					neighbor.updateBitfield(pieceIndex);
					String log = "Peer " + hostID + " received the 'have' message from " + neighborID
							+ " for the piece " + pieceIndex;
					writelog(log);
					if (neighbor.isComplete())
						syncinfo.updateCompletedPeers(neighborIndex);

					if (!syncinfo.haspiecie(pieceIndex)) {
						requestPiece.add(pieceIndex);
						// syncinfo.updateInterest(neighborIndex, true);
						ActualMessage interested = new ActualMessage(1, 2, null);
						sendMsg(interested);
					}
				}
				// bitfield
				else if (msg.getType() == 5) {
					// neighbor.updateHandShake();
					BitSet neighborBF = byte2bits(msg.getPayload());
					neighbor.setBitfield(neighborBF);
					if (neighbor.isComplete())
						syncinfo.updateCompletedPeers(neighborIndex);
					setRequestPiece();
					if (checkInterest()) {
						ActualMessage interested = new ActualMessage(1, 2, null);
						sendMsg(interested);
					} else {
						ActualMessage notInterested = new ActualMessage(1, 3, null);
						sendMsg(notInterested);
					}
				}
				// request
				else if (msg.getType() == 6) {
					int pieceIndex = byte2int(msg.getPayload());
					String log = "received a request " + pieceIndex + " from " + neighborID;
					writelog(log);
					// check if neighbor choked
					if (!syncinfo.getIsChoke()[this.neighborIndex]) {
						ActualMessage piece = creatPieceMsg(pieceIndex);
						sendMsg(piece);// send piece Msg
						writelog("sent a piece " + pieceIndex + " to peer " + neighborID);
					}
				}

				// piece
				else if (msg.getType() == 7) {
					int pieceIndex = byte2int(msg.getIndex());
					if (!syncinfo.haspiecie(pieceIndex)) {
						downloadPiece(msg);
						writelog("Peer " + hostID + " has downloaded the piece " + byte2int(msg.getIndex()) + " from "
								+ neighbor.getPeerID() + "  piece size:" + (msg.getLength() - 5));
						ActualMessage havepiece = new ActualMessage(4 + 1, 4, int2byte(pieceIndex));
						sendMsgAllPeer(havepiece);
						syncinfo.updateBitfield(pieceIndex);
						if (syncinfo.isHostComplete()) {
							// int type = 5;
							// byte[] pl = bits2byte(syncinfo.getBitfield());
							// int length = payload.length+1;
							// ActualMessage bitfield = new ActualMessage(length, type, pl);
							// sendMsg(bitfield);
							writelog("Peer "+hostID+" has downloaded the complete file");
							syncinfo.updateCompletedPeers(hostIndex);
							continue;
						}
					}
					if (!checkInterest()) {
						ActualMessage notinterested = new ActualMessage(1, 3, null);
						sendMsg(notinterested);
						continue;
					}
					int rate = downloadRate.get(neighborIndex);
					downloadRate.replace(neighborIndex, rate + 1);
					updateRequestPiece();
					int chosenPiece = choosePiece();
					byte[] payload = int2byte(chosenPiece);
					ActualMessage request = new ActualMessage(4 + 1, 6, payload);
					sendMsg(request);
					syncinfo.updateRequested(chosenPiece, true);
				}
			}
		}
	}

	private ActualMessage creatPieceMsg(int pieceIndex) {
		int size = common.getPieceSize();
		// create a byte[] with certain pieceSize
		byte[] pl = new byte[size];
		// try catch from .part
		try {
			fi = new FileInputStream(filePath + fileName + pieceIndex + ".part");
			fi.read(pl);
			fi.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// create pieceMessage
		ActualMessage pieceMessgae = new ActualMessage(size + 5, 7, int2byte(pieceIndex), pl);
		return pieceMessgae;
	}

	private void downloadPiece(ActualMessage msg) {
		int pieceIndex = byte2int(msg.getIndex());
		byte[] pl = msg.getPayload();
		try {
			fo = new FileOutputStream(filePath + fileName + pieceIndex + ".part");
			fo.write(pl);
			fo.flush();
			fo.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void setRequestPiece() {
		requestPiece = new ArrayList<>();
		BitSet pieceList = neighbor.getBitfield();
		pieceList.andNot(syncinfo.getBitfield());
		int i = 0;
		while (true) {
			i = pieceList.nextSetBit(i);
			if (i == -1)
				break;
			requestPiece.add(i++);
		}
		// if (requestPiece.isEmpty())
		// syncinfo.updateInterest(neighborIndex, false);
		// else
		// syncinfo.updateInterest(neighborIndex, true);
	}

	private void updateRequestPiece() {
		// int length=requestPiece.size();
		for (int i = 0; i < requestPiece.size(); i++) {
			while (syncinfo.haspiecie(requestPiece.get(i))) {
				requestPiece.remove(i);
				if (i >= requestPiece.size())
					break;
			}
		}
		// if (requestPiece.isEmpty())
		// syncinfo.updateInterest(neighborIndex, false);
	}

	private boolean checkInterest() {
		BitSet hostBF = syncinfo.getBitfield();
		BitSet neighborBF = neighbor.getBitfield();
		hostBF.and(neighborBF);
		if (hostBF.equals(neighborBF))
			return false;
		return true;
	}

	private boolean handshakeCheck(HandShake hs) {

		boolean handshakeCheck = false;
		// boolean idCheck=false;
		String standardheader = "P2PFILESHARINGPROJ";
		String header = hs.getHeader();
		int id = hs.getPeerID();
		if ((header.equals(standardheader)) && ((id == hostID))) {
			handshakeCheck = true;
		}

		return handshakeCheck;
	}

	private void creatnewhandshake() {
		String handshakeHeader = "P2PFILESHARINGPROJ";
		HandShake handShakemsg = new HandShake(handshakeHeader, neighborID);
		neighbor.send(handShakemsg);

	}

	private int choosePiece() {
		Random rd = new Random();
		int pieceIndex;
		while (true) {
			pieceIndex = requestPiece.get(rd.nextInt(requestPiece.size()));
			// if (syncinfo.haspiecie(pieceIndex))
			// continue;
			if (!syncinfo.getRequested(pieceIndex))
				break;
		}
		return pieceIndex;
	}

	private byte[] int2byte(int i) {
		ByteBuffer settobyte = ByteBuffer.allocate(4);
		settobyte.putInt(i);
		return settobyte.array();
	}

	private int byte2int(byte[] b) {
		return new BigInteger(b).intValue();
	}

	private byte[] bits2byte(BitSet bits) {
		byte[] bytes = new byte[bits.length() / 8 + 1];
		for (int i = 0; i < bits.length(); i++) {
			if (bits.get(i)) {
				bytes[bytes.length - i / 8 - 1] |= 1 << (i % 8);
			}
		}
		return bytes;
	}

	private BitSet byte2bits(byte[] bytes) {
		BitSet bits = new BitSet();
		for (int i = 0; i < bytes.length * 8; i++) {
			if ((bytes[bytes.length - i / 8 - 1] & (1 << (i % 8))) > 0) {
				bits.set(i);
			}
		}
		return bits;
	}

	private void sendMsg(ActualMessage msg) {
		neighborInfo.get(neighborIndex).send(msg);
	}

	private void writelog(String log) {

		String logname = System.getProperty("user.dir") + File.separator + "log_peer_" + hostID + ".log";
		try {
			SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String logtime = time.format(new Date().getTime());
			log = "[" + logtime + "]: " + log;
			FileWriter fw = new FileWriter(new File(logname), true);
			PrintWriter pw = new PrintWriter(fw);
			pw.println(log);
			pw.flush();
			fw.flush();
			pw.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendMsgAllPeer(ActualMessage havepiece) {
		for (Neighbor x : neighborInfo.values()) {
			// if (x.isHandShaked())
			x.send(havepiece);
			// String log="Peer "+hostID+" sent 'have'"+byte2int(havepiece.getPayload())+"
			// to peer "+x.getPeerID();
			// writelog(log);
		}
	}
}
