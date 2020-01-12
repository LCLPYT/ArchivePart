package work.lclpnet.archivepart.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

public class ArchiveEntryInputStream extends InputStream{

	private boolean closed = false;
	private long read = 0L;
	
	private ArchivePartFile apFile;
	private ArchiveEntry entry;
	private File file;
	private InputStream inStream;
	
	public ArchiveEntryInputStream(ArchivePartFile desc, ArchiveEntry entry) throws IOException, GeneralSecurityException {
		this.entry = entry;
		this.apFile = desc;
		this.file = this.apFile.getPartFile(entry.getPart());
		
		FileInputStream base = new FileInputStream(this.file);
		base.getChannel().position(entry.getOffset());
		
		this.inStream = base;
	}
	
	@Override
	public int read() throws IOException {
		if(read >= entry.getLength()) return -1;
		
		int b = inStream.read();
		read++;
		
		if(apFile.isEncrypted()) {
			long offset = read - 1;
			int i = (int) (offset % apFile.getPassword().length());
			
			return b ^ apFile.getPassword().charAt(i);
		} else return b;
	}
	
	@Override
	public int read(byte[] b) throws IOException {
		int length = (int) (entry.getLength() - read);
		if(length > b.length) length = b.length;
		
		int readNow = inStream.read(b, 0, length);
		if(read >= entry.getLength() || readNow == -1) return -1;
		
		read += readNow;
		
		if(apFile.isEncrypted()) {
			long offset = read - readNow;
			int ch = (int) (offset % apFile.getPassword().length());
			char[] pw = apFile.getPassword().toCharArray();
			
			for (int i = 0; i < b.length; i++) {
				if(ch >= pw.length) ch = 0;
				b[i] ^= pw[ch++];
			}
		}
		return readNow;
	}
	
	@Override
	public void close() throws IOException {
		if(closed || inStream == null) return;
		
		inStream.close();
		inStream = null;
		closed = true;
	}

}
