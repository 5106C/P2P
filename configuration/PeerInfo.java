package configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PeerInfo {
	private int NoP; // Number of Peers;
	private HashMap<Integer, Integer> peerID; // the ID and the associated index of a peer
	private List<String> HostName; // the name associated with an given index
	private List<Integer> PortNumber; // the port# associated with an given index
	private List<Boolean> HasFile; // If a peer associated with an given index has the complete file
	private List<Integer> ID; // also need to get peerID with a given Index

	public PeerInfo() {
		peerID = new HashMap<>();
		HostName = new ArrayList<>();
		PortNumber = new ArrayList<>();
		HasFile = new ArrayList<>();
		ID = new ArrayList<>();
		readConfig();
	}

	private void readConfig() {
		NoP = 0;
		String filePath = System.getProperty("user.dir") + File.separator;
		try {
			FileReader fr = new FileReader(filePath + "PeerInfo.cfg");
			BufferedReader br = new BufferedReader(fr);
			while (true) {
				String line = br.readLine();
				if (line == null)
					break;
				String[] info = line.split(" ");
				int pID = Integer.parseInt(info[0]);
				String HN = info[1];
				int PN = Integer.parseInt(info[2]);
				boolean HF = Integer.parseInt(info[3]) == 1 ? true : false;
				HostName.add(NoP, HN);
				PortNumber.add(NoP, PN);
				HasFile.add(NoP, HF);
				ID.add(NoP, pID);
				peerID.put(pID, NoP++);
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.err.println("config file not found!");
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

	public boolean hasPeer(int peerID) {
		return this.peerID.containsKey(peerID);
	}

	public int Indexof(int peerID) {
		return this.peerID.get(peerID);
	}
	
	public int getPeerID(int index) {
		return ID.get(index);
	}

	public String HostNameof(int index) {
		return HostName.get(index);
	}

	public int PortNumberof(int index) {
		return PortNumber.get(index);
	}

	public boolean HasFile(int index) {
		return HasFile.get(index);
	}

	public int getAmount() {
		return NoP;
	}
}
