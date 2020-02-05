package work.lclpnet.archivepart.model;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class ArchiveEntry {

	public static final long CRC_UNDEFINED = Long.MIN_VALUE;
	
	private String file;
	private int part;
	private long offset, length, crc32;
	
	public ArchiveEntry(String file, int part, long offset, long length, long crc32) {
		this.file = file;
		this.part = part;
		this.offset = offset;
		this.length = length;
		this.crc32 = crc32;
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
	
	public long getCrc32() {
		return crc32;
	}
	
	private String getCrc32HexString() {
		if(crc32 == CRC_UNDEFINED) return "undefined";
		return Long.toHexString(crc32).toUpperCase();
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
				e.length == this.length &&
				e.crc32 == this.crc32;
	}
	
	@Override
	public String toString() {
		return "ArchiveEntry{file=\"" + file + "\";part=" + part + ";offset=" + offset + ";length=" + length + ";crc32=" + getCrc32HexString() +  "}";
	}

}
