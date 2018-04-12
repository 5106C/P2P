import configuration.*;
import fileShare.Host;

public class PeerProcess {
	public static void main(String[] args) {
		PeerInfo peerinfo = new PeerInfo();
		int peerID = Integer.parseInt(args[0]);
		if (!peerinfo.hasPeer(peerID)) {
			System.out.println("invalid peerID!");
			System.exit(0);
		}
		Common common = new Common();
		Host host = new Host(peerID, common, peerinfo);
		host.start();
		System.out.println("Peer_" + peerID + "is online~");
	}

}
