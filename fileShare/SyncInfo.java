package fileShare;

import java.util.BitSet;

import configuration.*;

public class SyncInfo {
	private BitSet completedPeers;
	private BitSet bitfield;
	private boolean[] want;
	private boolean[] wanted;
	private boolean[] requested;
	private int numOfPeers;
	private int numOfPiece;
	private boolean[] isChoke;

	public SyncInfo(Common common, PeerInfo peerinfo) {
		numOfPeers = peerinfo.getAmount();
		numOfPiece = common.getPieceAmount();
		bitfield = new BitSet(numOfPiece);
		completedPeers = new BitSet(numOfPeers);
		want = new boolean[numOfPeers];
		wanted = new boolean[numOfPeers];
		requested=new boolean[numOfPiece];
		isChoke = new boolean[numOfPiece];
	}
	
	public boolean[] getIsChoke() {
		return isChoke;
	}
	
	public int getNumOfPeers() {
		return numOfPeers;
	}
	
	public void resetRequested() {
		synchronized(requested) {
			requested=new boolean[numOfPiece];
		}
	}
	public void updateRequested(int pieceIndex, boolean b) {
		synchronized(requested) {
			requested[pieceIndex]=b;
		}
	}
	
	public boolean getRequested(int pieceIndex) {
		synchronized(requested) {
			return requested[pieceIndex];
		}
	}
	
	public BitSet getCompletedPeers() {
		synchronized (completedPeers) {
			return completedPeers;
		}
	}

	public void updateCompletedPeers(int index) {
		synchronized (completedPeers) {
			completedPeers.set(index);
		}
	}
	
	public BitSet getBitfield() {
		synchronized (bitfield) {
			return (BitSet) bitfield.clone();
		}
	}
	
	public boolean haspiecie(int pieceIndex) {
			return bitfield.get(pieceIndex);
	}

	public void updateBitfield(int pieceIndex) {
		synchronized (bitfield) {
			bitfield.set(pieceIndex);
		}
	}

	public void flipBitfield() {
		synchronized (bitfield) {
			bitfield.flip(0, numOfPiece);
		}
	}

	public boolean allComplete() {
		synchronized (completedPeers) {
			return completedPeers.nextClearBit(0) >= numOfPeers;
		}
	}

	public boolean isHostComplete() {
		synchronized (bitfield) {
			return bitfield.nextClearBit(0) >= numOfPiece;
		}
	}
	
	
	public boolean interest(int index) {
//		synchronized(want) {
			return want[index];
//		}
	}
	
	public boolean interested(int index) {
//		synchronized(wanted) {
			return wanted[index];
//		}
	}
	
	public void updateInterest(int index, boolean b) {
		synchronized(want) {
			want[index]=b;
		}
	}
	
	public void updateInterested(int index, boolean b) {
		synchronized(wanted) {
			wanted[index]=b;
		}
	}

}
