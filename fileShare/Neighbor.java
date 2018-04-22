package fileShare;

import java.io.*;
import java.net.Socket;
import java.util.BitSet;
import configuration.*;

public class Neighbor {
	private int peerID; //
	private int index; //
	private int portNum;
	private String hostname;
	private BitSet bitfield; // this is the neighbor's bitfield, not host's!
	private int numofpiece;
	private Socket connection;
	private P2P p2p;
	ObjectInputStream in;
	ObjectOutputStream out;

	public Neighbor(Common common, PeerInfo peerinfo, int index, Socket connection, ObjectInputStream in,
			ObjectOutputStream out) {
		this.index = index;
		this.peerID = peerinfo.getPeerID(index);
		this.portNum = peerinfo.PortNumberof(index);
		this.hostname = peerinfo.HostNameof(index);
		numofpiece = common.getPieceAmount();
		this.connection = connection;
		this.in = in;
		this.out = out;
		bitfield = new BitSet(numofpiece);
		if (peerinfo.HasFile(index))
			bitfield.flip(0, numofpiece);
	}

	public int getPeerID() {
		return peerID;
	}

	public int getIndex() {
		return index;
	}

	public int getPortNum() {
		return portNum;
	}

	public String getHostName() {
		return hostname;
	}

	public BitSet getBitfield() {
		synchronized (bitfield) {
			return (BitSet) bitfield.clone();
		}
	}

	public void setBitfield(BitSet bitf) {
		synchronized (bitfield) {
			bitfield = bitf;
		}
	}

	public void updateBitfield(int pieceindex) {
		synchronized (bitfield) {
			bitfield.set(pieceindex);
		}
	}

	public boolean isComplete() {
		synchronized (bitfield) {
			return bitfield.nextClearBit(0) >= numofpiece;
		}
	}

	public boolean hasPiece(int pieceindex) {
		synchronized (bitfield) {
			return bitfield.get(pieceindex);
		}
	}

	public void send(Object obj) {
		synchronized (out) {
			try {
				out.writeObject(obj);
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public Object receive() {
		Object obj = null;
		synchronized (in) {
			try {
				obj = in.readObject();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
			}
		}
		return obj;
	}

	/** associate the very p2p to this neighbor instance */
	public void setP2P(P2P p2p) {
		this.p2p = p2p;
	}

	public void closeConnection() {
		p2p.stopRunning();
		try {
			in.close();
			out.close();
			connection.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
