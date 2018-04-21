package configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Common {
	private int NPN; // Number of Preferred Neighbors
	private int UI; // Unchoking Interval (unit: s)
	private int OUI; // Optimistic Unchoking Interval (unit: s)
	private String FileName;
	private int FileSize; // unit byte
	private int PieceSize; // unit byte

	public Common() {
		readConfig();
	}

	private void readConfig() {
		String filePath = System.getProperty("user.dir") + File.separator;
		try {
			FileReader fr = new FileReader(filePath + "Common.cfg");
			BufferedReader br = new BufferedReader(fr);
			while (true) {
				String line = br.readLine();
				if (line == null)
					break;
				String info[] = line.split(" ");
				if (info[0].equals("NumberOfPreferredNeighbors"))
					NPN = Integer.parseInt(info[1]);
				else if (info[0].equals("UnchokingInterval"))
					UI = Integer.parseInt(info[1]);
				else if (info[0].equals("OptimisticUnchokingInterval"))
					OUI = Integer.parseInt(info[1]);
				else if (info[0].equals("FileName"))
					FileName = info[1];
				else if (info[0].equals("FileSize"))
					FileSize = Integer.parseInt(info[1]);
				else if (info[0].equals("PieceSize"))
					PieceSize = Integer.parseInt(info[1]);
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

	public int getNPN() {
		return NPN;
	}

	public int getUI() {
		return UI;
	}

	public int getOUI() {
		return OUI;
	}

	public int getFileSize() {
		return FileSize;
	}

	public int getPieceSize() {
		return PieceSize;
	}

	public int getLastSize() {
		return FileSize % PieceSize == 0 ? PieceSize : FileSize % PieceSize;
	}

	public int getPieceAmount() {
		return getLastSize() == PieceSize ? FileSize / PieceSize : FileSize / PieceSize + 1;
	}

	public String getFileName() {
		return FileName;
	}
}
