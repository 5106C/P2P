/** This thread choose choke & unchoke peers during certain interval, and send choke and unchoke msgs. */

package fileShare;

import message.ActualMessage;

import java.io.*;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Choke extends Thread {
	
    private volatile boolean running = true; // running flag
    
    private int unchokeInterval;
    private int optUnchokeInterval;
    private int numOfPreferedNerghbor;
    
    private int numOfPeers;   //num of peers
    private boolean[] isChoke;  // = new boolean[numOfNeighbor];
    //better to set as peer index
    private int optIndex = -1;
    private int preOptIndex = -1;
    //access to get variable
    private ConcurrentHashMap<Integer, Integer> curSendMeMsg; // neighbor index -> sending rate in n of parts
    private boolean[] curInterestedMe;
    private ConcurrentHashMap<Integer, Neighbor> neighborsInfo; // index -> peer info from PeerInfo.cfg

    private int ID_me;


    public Choke(ConcurrentHashMap<Integer,Integer> curSendMeMsg, boolean[] curInterestedMe, Common common,
                 ConcurrentHashMap<Integer, Neighbor> neighborsInfo, int ID_me) {
        
        this.numOfPeers = curInterestedMe.length;
        this.curInterestedMe = curInterestedMe;
        
        isChoke = new boolean[numOfPeers];
        for(int i = 0; i<isChoke.length; i++) {
            isChoke[i] = true;
        }
        this.curSendMeMsg = curSendMeMsg;

        
        //Common
        this.unchokeInterval = common.UnchokingInterval;
        this.optUnchokeInterval = common.OptimisticUnchokingInterval;
        this.numOfPreferedNerghbor = common.NumberOfPreferredNeighbors;
        
        this.neighborsInfo = neighborsInfo;
        this.ID_me = ID_me;
    }
    
    public int getUnchockInterval(){

        return this.unchokeInterval;
    }

    public  int getOptUnchockInterval(){

        return this.optUnchokeInterval;
    }

    public int getNumOfPreferedNerghbor(){

        return this.numOfPreferedNerghbor;
    }

    public int getNumOfNeighbor(){

        return this.numOfPeers;
    }

    public int getOptIndex(){

        return this.optIndex;
    }

    public void run(){
    	
        if(numOfPeers == 0){
            System.out.println("peer connection fail");
        }
        if(numOfPreferedNerghbor==0){
            System.out.println("process may finished");
        }
        if(numOfPreferedNerghbor>numOfPeers){
            System.out.println("Check the process");
        }
        
        int count = 1; //counter
        List<Integer> preferNeighbor = new LinkedList<>();
        
        while(running){
            try{
                sleep(1000);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            count++;
            
            // unchoke Interval
            if(count % unchokeInterval == 0){
                int logFlag = 0;
                
                //contain the index of neighbor interested in P
                preferNeighbor = maxRateNeighbor(); // list of prefer Neighbor's index
                System.out.println(preferNeighbor);
                
                // check and choke unused Neighbors
                for(int i = 0; i < isChoke.length; i++){
                    if(!isChoke[i] && !preferNeighbor.contains(i) && i != optIndex){
                        isChoke[i] = true;
                        //create chock message;
                        ActualMessage msg = new ActualMessage(1, 0, null);
                        //send chock to peer i;
                        sendMessage(msg, i);
                        System.out.println("[" + count + "]" + "Peer choke peer " + i);
                        logFlag = 1;
                    }
                }

                //choose high speed to unchoke
                for (int i = 0; i < numOfPreferedNerghbor && i < preferNeighbor.size(); i++) {
                    int index = preferNeighbor.get(i);
                    if (isChoke[index]) {
                        //create unchock message
                    	ActualMessage msg = new ActualMessage(1, 1, null);
                        //send unchock to peer i;
                        sendMessage(msg, index);
                        //waiting for request message from i;
                        isChoke[index] = false;
                        System.out.println("[" + count + "]" + "Peer unchoke peer" + index);
                        logFlag = 1;
                    }
                }
                
                // writelog the current unchokeList
                if(logFlag == 1) {
                    List<String> unchokeList = new ArrayList<>();
                    if(!preferNeighbor.isEmpty()) {
                        for (int i : preferNeighbor) {
                            unchokeList.add("" + neighborsInfo.get(i).getPeerID());
                        }
                    }
                    writelog("Peer " + ID_me + " has preferred neighbors " + unchokeList);
                }
            }

            //optimistically unchoke interval
            if(count % optUnchokeInterval == 0){
            	
                //actually not new, waiting for coming
                if(curInterestedMe.length == 0){
                    System.out.println("all other peers have my messages");
                }

                List<Integer> optChooseList = new ArrayList<>();
                for(int i = 0; i < numOfPeers; i++){
                    if(isChoke[i] && curInterestedMe[i]) {
                        optChooseList.add(i);
                    }
                }
                
                //randomly unchock among currently chocked peer
                if(!optChooseList.isEmpty()) {
                	
                    int i = (int) (Math.random() * optChooseList.size());
                    optIndex = optChooseList.get(i);
                    
                    // send unchoke msg if optpeer is changed
                    if(optIndex != preOptIndex) {
                    	ActualMessage msg = new ActualMessage(1, 1, null);
                        sendMessage(msg, optIndex);
                        isChoke[optIndex] = false;
                        writelog("Peer " + ID_me + " has the optimistically unchoked neighbor " +
                                neighborsInfo.get(optIndex).getPeerID());
                        System.out.println("[" + count + "]" + "Peer optimistically unchoke peer" + optIndex);
                    }

                }

                // choke previous optPeer 
                if(preOptIndex != -1 && preOptIndex != optIndex && preferNeighbor != null && 
                		!preferNeighbor.contains(preOptIndex) && !isChoke[preOptIndex]) {
                	
                	ActualMessage msg = new ActualMessage(1, 0, null);
                    //send chock to peer index;
                    sendMessage(msg, preOptIndex);
                    isChoke[preOptIndex] = true;
                    System.out.println("[" + count + "]" + "Peer choke peer " + preOptIndex);
                }
                preOptIndex = optIndex;
            }
        }
    }

    private List<Integer> maxRateNeighbor() {
    	
        List<Integer> ReMaxRate = new LinkedList<>();
        int i = 0;
        Queue<Pair> maxRate = new PriorityQueue<>(); 
        
        for (int index : curSendMeMsg.keySet()) {
            System.out.println("current sending me message: " + index);        	
        }
        for (int k : curSendMeMsg.keySet()) {
            if(curInterestedMe[k]) {
                maxRate.offer(new Pair(curSendMeMsg.get(k), k));
            }
            curSendMeMsg.put(k, 0);
        }
        
        while (i < numOfPreferedNerghbor && !maxRate.isEmpty()) {
            ReMaxRate.add(maxRate.poll().index);
            i++;
        }
        
        System.out.println("my reMaxRate is " + ReMaxRate);
        return ReMaxRate;
    }

    private void sendMessage(ActualMessage msg, int i) {
        neighborsInfo.get(i).send(msg);
    }

    private void writelog(String log) {

        String filePath = System.getProperty("user.dir") + File.separator;
        String logname= filePath+"log_peer_"+ ID_me + ".log";
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

    public void stopRunning() {
        running = false;
    }

    private class Pair implements Comparable<Pair> {
    	
        int rate;
        int index;
        public Pair(int rate, int index) {
            this.rate = rate;
            this.index = index;
        }

        @Override
        // sort sequence: high rate pair first
        public int compareTo(Pair that) {
        	
            if(that.rate != this.rate) return that.rate - this.rate;
            else {
                Random random = new Random();
                boolean b = random.nextBoolean();
                if(b) return 1;
                else return -1;
            }
        }
    }
}

