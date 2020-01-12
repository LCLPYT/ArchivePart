package test.test.archivepart.model;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class ArchiveEntry {

	private String file;
	private int part;
	private long offset, length;
	
	public ArchiveEntry(String file, int part, long offset, long length) {
		this.file = file;
		this.part = part;
		this.offset = offset;
		this.length = length;
	}
	
	public String getFile() {
		return file;
	}
	
	public long getOffset() {
		return offset;
	}
	
	public void setOffset(long offset) {
		this.offset = offset;
	}
	
	public int getPart() {
		return part;
	}
	
	public long getLength() {
		return length;
	}
	
	public ArchiveEntryInputStream openInputStream(ArchivePartFile archive) throws IOException, GeneralSecurityException {
		return archive.getInputStream(this);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof ArchiveEntry)) return false;
		ArchiveEntry e = (ArchiveEntry) obj;
		return 
				e.file.equals(this.file) && 
				e.part == this.part && 
				e.offset == this.offset && 
				e.length == this.length;
	}
	
	@Override
	public String toString() {
		return "ArchiveEntry{file=\"" + file + "\";part=" + part + ";offset=" + offset + ";length=" + length + "}";
	}
	
}
