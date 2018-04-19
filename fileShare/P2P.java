package fileShare;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
	
	private String filePath;
	private String fileName;

	public P2P(Common common, PeerInfo peerinfo, SyncInfo syncinfo, int hostID, int neighborIndex,
			ConcurrentHashMap<Integer, Integer> downloadRate, ConcurrentHashMap<Integer, Neighbor> neighborInfo,
			boolean handshakefirst,int hostIndex) {
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
		filePath = System.getProperty("user.dir") + File.separator
		            + "peer_" + hostID + File.separator;
		fileName = common.getFileName();
		hostIndex=peerinfo.getPeerID(hostID);
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
				//request
				else if(msg.getType() == 6) {
					//check if neighbor choked
					if(!syncinfo.getIsChoke()[this.neighborIndex]) {
						int pieceIndex = byte2int(msg.getPayload());
						ActualMessage piece = creatPieceMsg(pieceIndex);
						sendMsg(piece);//send piece Msg
					}
				}

				//piece
				else if(msg.getType() == 7) {

					int pieceIndex=byte2int(msg.getIndex());
					byte[] payload=msg.getPayload();
					creatOnePiece(pieceIndex, payload);
                    writelog("Peer " + hostID + " has downloaded the piece " + byte2int(msg.getIndex()) + " from " + neighbor.getPeerID());

					int rate=downloadRate.get(neighborIndex);
					downloadRate.replace(neighborIndex,rate+1);

					ActualMessage havepiece=new ActualMessage(4,4,int2byte(pieceIndex));
					sendMsgAllPeer(havepiece);

                    syncinfo.updateBitfield(pieceIndex);
                    if(syncinfo.isHostComplete()){
                        syncinfo.updateCompletedPeers(hostIndex);
                        break;
                    }

                    if(!checkBitfield()) {
                        ActualMessage notinterested = new ActualMessage(4, 3, null);
                        sendMsg(notinterested);
                    }else{

                        }




                    }

				}
			}
		}
	}
	
	private ActualMessage creatPieceMsg(int pieceIndex) {
		int size;
		// check if pieceIndex is the last piece
		if(pieceIndex < common.getPieceAmount() - 1) {
			size = common.getFileSize(); 
		} else {
			size = common.getLastSize();
		}
		// create a byte[] with certain pieceSize
		byte[] payload = new byte[size];
		// try catch from .part
		try {
            File file = new File(filePath + fileName + pieceIndex + ".part");
            FileInputStream fis = new FileInputStream(file);
            fis.read(payload);
        } catch (IOException e) {
            e.printStackTrace();
        }
		
		//create pieceMessage
		ActualMessage pieceMessgae = new ActualMessage(size + 1, 7, payload);
		return pieceMessgae;
	}

	private void creatOnePiece(int pieceIndex, byte[] payload){

	    try {
            File file = new File(filePath + fileName + pieceIndex + ".part");
            FileOutputStream fileOuput = new FileOutputStream(file);
            fileOuput.write(payload);

        }catch(IOException e){
	        e.printStackTrace();
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
        boolean handshakeCheck=false;
        //boolean idCheck=false;
        String standardheader="P2PFILESHARINGPROJ";
        String header=hs.getHeader();
        int id=hs.getPeerID();
        if((header.equals(standardheader)) && (( id==neighborID))){
            handshakeCheck=true;
        }

        return handshakeCheck;
	}

	private void creatnewhandshake() {
		// TODO Auto-generated method stub
        String handshakeHeader="P2PFILESHARINGPROJ";
        HandShake handShakemsg=new HandShake(handshakeHeader,neighborID);
        neighbor.send(handShakemsg);

	}

	private int choosePiece() {

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
                	bytes[bytes.length - i / 8 - 1] |= 1 << ( i % 8 );
            		}
        	}
        	return bytes;
	}
	
	private BitSet byte2bits(byte[] bytes) {
		BitSet bits = new BitSet();
        	for (int i = 0; i < bytes.length * 8; i++) {
            		if ((bytes[ bytes.length - i / 8 - 1 ] & ( 1 << ( i % 8 ))) > 0) {
                		bits.set(i);
            		}
        	}
        	return bits;
	}
	
	private void sendMsg(ActualMessage msg) {
		neighborInfo.get(this.neighborIndex).send(msg);
	}

	private void writelog(String log) {
        
        	String logname= filePath+"log_peer_"+ hostID + ".log";
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

	private void sendMsgAllPeer(ActualMessage havepiece){
		for(Neighbor x : neighborInfo.values()) {
			sendMsg(havepiece);
		}
    	}
}
