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
	    if(handshakefirst){
	        creatnewhandshake(neighbor);
            System.out.println(hostID +"send my handshake to "+neighborID );    //neighborID, peerID
            writetolog(hostID+ "makes a connection to peer "+ neighborID);

        }

        while(running){
	        Object messagereceived=null;
	       try{
	           messagereceived=neighbor.receive();

//           } catch (IOException ioException) {
//                ioException.printStackTrace();

//	       } catch (ClassNotFoundException e) {
//               System.err.println("Data received in unknown format!");
           }

           if (messagereceived instanceof HandShake){
               if(!handshakeCheck((HandShake)messagereceived, neighbor)){
                   break;
               }
	           if (!handshakefirst){
	               creatnewhandshake();
                   System.out.println(hostID+" send handshake to "+neighborID);
                   
                   int type=5;
                   byte[] payload=bits2byte(syncinfo.getBitfield());
                   int length=payload.length;
                   ActualMessage bitfield= new ActualMessage(length, type, payload);
                   sendMsg(bitfield);
                   
               }
               
           }
           
           if(messagereceived instanceof ActualMessage){
	           ActualMessage msg=(ActualMessage)messagereceived;
	           
	           if (msg.getType()==2){
	               syncinfo.updateInterested(neighborIndex, true);
                   writetolog();
	           }
	           
	           if(msg.getType()==3){
	               syncinfo.updateInterested(neighborIndex,false);
	               
               }
               
               
           }
           
           


        }

	}

	public void stopRunning() {
		running = false;
	}


	}

	public void stopRunning() {
		running = false;
	}

}
