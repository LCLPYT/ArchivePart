package work.lclpnet.archivepart.factory;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import work.lclpnet.archivepart.factory.readers.*;
import work.lclpnet.archivepart.model.APParseException;
import work.lclpnet.archivepart.model.ArchivePartFile;
import work.lclpnet.archivepart.ArchivePart;

public class APParser {

	private File file;
	private String password = null;
	
	public APParser(File f) {
		if(f == null || !f.exists() || !f.isFile()) throw new IllegalArgumentException("\"" + f.getAbsolutePath() + "\" is not a file.");
		this.file = f;
	}

	public APParser setPassword(String password) {
		this.password = password;
		return this;
	}
	
	public String getPassword() {
		return password;
	}
	
	public File getFile() {
		return file;
	}

	public ArchivePartFile parse() throws APParseException {
		ArchivePartFile apFile;
		DataInputStream in = null;

		try {
			in = new DataInputStream(new FileInputStream(file));
			int version = in.readInt();
			
			ArchivePart.log("Parsing ArchivePart file with file version " + version + "...");
			
			switch (version) {
			case 1:
				apFile = new APReaderV1().read(in, this);
				break;
			case 2:
				apFile = new APReaderV2().read(in, this);
				break;
			case 3:
				apFile = new APReaderV3().read(in, this);
				break;
			default:
				throw new APParseException("This ArchivePart can't parse AP files with version " + version + ". (max=" + ArchivePart.VERSION + ")");
			}

			return apFile;
		} catch (Exception e) {
			if(e instanceof APParseException) throw (APParseException) e;
			
			APParseException ex = new APParseException("The file \"" + file.getAbsolutePath() + "\" couldn't be parsed as ArchivePart file. (see suppressed errors for details)");
			ex.addSuppressed(e);
			ex.printStackTrace();
			return null;
		} finally {
			try {
				if(in != null) in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
