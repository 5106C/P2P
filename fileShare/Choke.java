/** This thread choose choke & unchoke peers during certain interval, and send choke and unchoke msgs. */

package fileShare;

import message.ActualMessage;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import configuration.*;

public class Choke extends Thread {
	
    private volatile boolean running = true;
    
    private int unchokeInterval;
    private int optUnchokeInterval;
    private int numOfPreferedNerghbor;
    
    private int numOfPeers;
    private int numOfNeighbors;
    private boolean[] isChoke;  // = new boolean[numOfNeighbors];
    
    private int optIndex = -1;
    private int preOptIndex = -1; // detect if optNeighbor has changed
    
    private ConcurrentHashMap<Integer, Integer> downloadRate; // neighbor index -> sending rate in n of parts
    private boolean[] wanted;
    private ConcurrentHashMap<Integer, Neighbor> neighborsInfo; // index -> peer info from PeerInfo.cfg

    private int hostID;


    public Choke(ConcurrentHashMap<Integer, Integer> downloadRate, ConcurrentHashMap<Integer, Neighbor> neighborsInfo,
			SyncInfo syncInfo, Common common, int hostID) {
    	
    	this.numOfPeers = syncInfo.getNumOfPeers();
        // peerInfo
        this.numOfNeighbors = neighborsInfo.size(); // what if size change?
        
        // syncInfo
        for(int i = 0; i < this.numOfPeers; i++) {
        	wanted[i] = syncInfo.interested(i);
        }
        
        isChoke = syncInfo.getIsChoke();
        for(int i = 0; i < this.numOfPeers; i++) {
            isChoke[i] = true;
        } 
        
        // downloadRate
        this.downloadRate = downloadRate;

        // Common
        this.unchokeInterval = common.getUI();
        this.optUnchokeInterval = common.getOUI();
        this.numOfPreferedNerghbor = common.getNPN();
        
        // neighborsInfo 
        this.neighborsInfo = neighborsInfo;
        
        // hostID
        this.hostID = hostID;
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

        return this.numOfNeighbors;
    }

    public int getOptIndex(){

        return this.optIndex;
    }

    public void run(){
    	
        if(numOfNeighbors == 0){
            System.out.println("peer connection fail");
        }
        if(numOfPreferedNerghbor == 0){
            System.out.println("process may finished");
        }
        if(numOfPreferedNerghbor > numOfNeighbors){
            System.out.println("Check the process");
        }
        
        int count = 1; // timer
        List<Integer> preferNeighbor = new LinkedList<>();
        
        while(running){
            try{
                sleep(1000);
            }catch (InterruptedException e) {
                e.printStackTrace();
            }
            count++;
            
            // unchoke Interval
            if(count % unchokeInterval == 0) {
                int logFlag = 0; // detect if chokeList changes
                
                //contain the index of neighbor interested in P
                preferNeighbor = maxRateNeighbor(); // list of prefer Neighbor's index
                System.out.println(preferNeighbor);
                
                // check and choke unused Neighbors
                for(int i = 0; i < this.numOfPeers; i++){
                    if(!isChoke[i] && !preferNeighbor.contains(i) && i != optIndex){
                        isChoke[i] = true;
                        ActualMessage msg = new ActualMessage(1, 0, null);
                        sendMessage(msg, i);
                        System.out.println("[" + count + "]" + "Peer choke peer " + i);
                        logFlag = 1;
                    }
                }

                //choose high speed to unchoke
                for (int i = 0; i < numOfPreferedNerghbor && i < preferNeighbor.size(); i++) {
                    int index = preferNeighbor.get(i);
                    if (isChoke[index]) {
                    	ActualMessage msg = new ActualMessage(1, 1, null);
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
                    writelog("Peer " + hostID + " has preferred neighbors " + unchokeList);
                }
            }

            //optimistically unchoke interval
            if(count % optUnchokeInterval == 0) {
            
                if(wanted.length == 0){
                    System.out.println("all other peers have my messages");
                }
                
                // create optUnchokeList, currently choke and wanted
                List<Integer> optUnchokeList = new ArrayList<>();
                for(int i = 0; i < this.numOfPeers; i++){
                    if(isChoke[i] && wanted[i]) {
                        optUnchokeList.add(i);
                    }
                }
                
                //randomly unchock from optUnchockList
                if(!optUnchokeList.isEmpty()) {
                	
                    int i = (int) (Math.random() * optUnchokeList.size());
                    optIndex = optUnchokeList.get(i);
                    
                    // detect if optNeighbor has changed
                    if(optIndex != preOptIndex) {
                    	ActualMessage msg = new ActualMessage(1, 1, null);
                        sendMessage(msg, optIndex);
                        isChoke[optIndex] = false;
                        writelog("Peer " + hostID + " has the optimistically unchoked neighbor " +
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
                
                // reset preOptIndex
                preOptIndex = optIndex;
            }
        }
    }

    private List<Integer> maxRateNeighbor() {
    	
        List<Integer> ReMaxRate = new LinkedList<>();
        int i = 0;
        Queue<Pair> maxRate = new PriorityQueue<>(); 
        
        for (int index : downloadRate.keySet()) {
            System.out.println("current sending me message: " + index);        	
        }
        
        for (int k : downloadRate.keySet()) {
            if(wanted[k]) {
                maxRate.offer(new Pair(downloadRate.get(k), k));
            }
            downloadRate.put(k, 0);
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

