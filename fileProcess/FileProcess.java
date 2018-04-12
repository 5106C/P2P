package fileProcess;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import configuration.*;

public class FileProcess {
	private String directory;
	private String filepath;
	private String filename;
	private int filesize;
	private int piecesize;
	private int numberofpieces;
	private int lastsize;

	public FileProcess(int peerID, Common common) throws IOException {
		directory = System.getProperty("user.dir") + File.separator + "peer_" + peerID + File.separator;
		File folder = new File(directory);
		folder.mkdirs();
		filename = common.getFileName();
		filesize = common.getFileSize();
		piecesize = common.getAmount();
		numberofpieces = common.getAmount();
		lastsize = common.getLastSize();
	}

	public void split() {
		if (filesize == 0) {
			System.out.println("empty file!");
			return;
		}
		if (piecesize == 0) {
			System.out.println("endless spliting...");
			return;
		}
		filepath = directory + filename;
		FileInputStream filein;
		try {
			filein = new FileInputStream(filepath);
			BufferedInputStream instream = new BufferedInputStream(filein);// may need revising
			byte[] temp = new byte[filesize];
			instream.read(temp, 0, filesize);
			instream.close();
			for (int i = 0; i < numberofpieces; i++) {
				BufferedOutputStream outstream = new BufferedOutputStream(new FileOutputStream(filepath + i + ".part"));
				if (i == numberofpieces - 1 && lastsize != 0)
					outstream.write(temp, piecesize * i, lastsize);
				else {
					outstream.write(temp, piecesize * i, piecesize);
				}
				outstream.flush();
				outstream.close();
			}
			System.out.println("File split successfully");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void rebuild() {
		try {
			BufferedOutputStream outstream = new BufferedOutputStream(new FileOutputStream(directory + filename));
			for (int i = 0; i < numberofpieces; i++) {
				byte[] temp = new byte[piecesize];
				BufferedInputStream instream = new BufferedInputStream(		//may need revising
						new FileInputStream(directory + filename + i + ".part"));
				if (i == numberofpieces - 1 && lastsize != 0) {
					instream.read(temp, 0, lastsize);
					outstream.write(temp, 0, lastsize);
				}
				else {
					instream.read(temp, 0, piecesize);
					outstream.write(temp, 0, piecesize);
				}
				instream.close();
				outstream.close();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void deletePieces() throws IOException {
		for(int i=0;i<numberofpieces;i++) {
			Files.deleteIfExists(Paths.get(directory+filename+i+".part"));
		}
	}

	public int getFilesize() {
		return filesize;
	}

	public int getPiecesize() {
		return piecesize;
	}

	public int getNumberofpiece() {
		return numberofpieces;
	}

	public int getLastsize() {
		return lastsize;
	}
}
