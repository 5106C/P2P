package fileShare;

import java.util.BitSet;

public class SyncInfo {
	
	/* instance variables */
  private BitSet completedLabel;
	private BitSet bitfield;
	private boolean[] isInteresetedOnMe;
	private boolean[] myInterest;
	
	/** constructor */
	public SyncInfo(BitSet completedLabel, BitSet bitfield, boolean[] isInteresetedOnMe, boolean[] myInterest) {
		 this.completedLabel = completedLabel;
		 this.bitfield = bitfield;
		 this.isInteresetedOnMe = isInteresetedOnMe;
		 this.myInterest = myInterest;
	}
	
	public BitSet getCompletedLabel() {
		return completedLabel;
	}
	
	public BitSet getBitField () {
		return bitfield;
	}
	
	public boolean[] getIsInterestedOnMe () {
		return isInteresetedOnMe;
	}
	
	public boolean[] getMyInterested () {
		return myInterest;
	}
	
	// isInterestedOnMe P2P Type2 & 3
	public void setInterestedOnMe (int peerID, boolean onMe) {
		synchronized (isInteresetedOnMe) {
            isInteresetedOnMe[peerID] = onMe;
        }
	}
	
	// myInterested P2P Type4 & 5
	public void setMyInterest (int peerID, boolean MyInt) {
		synchronized (myInterest) {
            myInterest[peerID] = MyInt;
        }
	}
	
	//
 }

