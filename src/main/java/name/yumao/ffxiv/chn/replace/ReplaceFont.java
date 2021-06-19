package name.yumao.ffxiv.chn.replace;

import name.yumao.ffxiv.chn.builder.BinaryBlockBuilder;
import name.yumao.ffxiv.chn.builder.TexBlockBuilder;
import name.yumao.ffxiv.chn.model.SqPackIndex;
import name.yumao.ffxiv.chn.model.SqPackIndexFile;
import name.yumao.ffxiv.chn.model.SqPackIndexFolder;
import name.yumao.ffxiv.chn.util.FFCRC;
import name.yumao.ffxiv.chn.util.LERandomAccessFile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;


public class ReplaceFont {

	private String pathToIndex;
	private String resourceFolder;

	public ReplaceFont(String pathToIndex, String resourceFolder) {
		this.pathToIndex = pathToIndex;
		this.resourceFolder = resourceFolder;
	}

	public void replace() throws Exception {

		System.out.println("Loading Index File...");
		HashMap<Integer, SqPackIndexFolder> index = new SqPackIndex(pathToIndex).resloveIndex();
		System.out.println("Loading Index Complete");

		LERandomAccessFile leIndexFile = new LERandomAccessFile(pathToIndex, "rw");
		LERandomAccessFile leDatFile = new LERandomAccessFile(pathToIndex.replace("index", "dat0"), "rw");

		long datLength = leDatFile.length();
		leDatFile.seek(datLength);

		File resourceFolderFile = new File(resourceFolder);
		if (resourceFolderFile.isDirectory()) {
			for (File resourceFile : resourceFolderFile.listFiles()) {
				if (resourceFile.isFile()) {
					//read file
					LERandomAccessFile lera = new LERandomAccessFile(resourceFile, "r");
					byte[] data = new byte[(int) lera.length()];
					lera.readFully(data);
					lera.close();
					//build block
					byte[] block = new byte[0];
					if(resourceFile.getName().endsWith(".tex")) {
						//type 4
						block = new TexBlockBuilder(data).buildBlock();
					}else {
						block = new BinaryBlockBuilder(data).buildBlock();
					}
					//add index
					System.out.println("Replace : " + resourceFile.getName());
					Integer folderCRC = FFCRC.ComputeCRC(("common/font").toLowerCase().getBytes());
					Integer fileCRC = FFCRC.ComputeCRC((resourceFile.getName()).toLowerCase().getBytes());
					SqPackIndexFile indexFile = index.get(folderCRC).getFiles().get(fileCRC);
					leIndexFile.seek(indexFile.getPt() + 8);
					leIndexFile.writeInt((int) (datLength / 8));
					//add dat
					datLength += block.length;
					leDatFile.write(block);
				}
			}
		}
		leDatFile.close();
		leIndexFile.close();
	}
	
	public static void main(String[] args) throws Exception {
		
		File inputFolder = new File("input");
		
		if(inputFolder.isDirectory() && inputFolder.list().length > 0) {
			for(File inputFile : inputFolder.listFiles()) {
				if(inputFile.isFile()) {
					LERandomAccessFile lera = new LERandomAccessFile("input" + File.separator + inputFile.getName(), "r");
					byte[] dist = new byte[(int)lera.length()];
					lera.readFully(dist);
					lera.close();
					FileOutputStream fos = new FileOutputStream(new File("output" + File.separator + inputFile.getName()));
					fos.write(dist);
					fos.flush();
					fos.close();
				}
			}
		}
		new ReplaceFont("output" + File.separator + "000000.win32.index", "resource").replace();
	}
}
