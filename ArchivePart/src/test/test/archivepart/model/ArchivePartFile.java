package test.test.archivepart.model;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import test.test.archivepart.factory.APBuilder;
import test.test.archivepart.factory.APParser;
import test.test.archivepart.factory.BuilderState;
import work.lclpnet.archivepart.ArchivePart;

public class ArchivePartFile {

	private File baseFile;
	private String password;
	private List<ArchiveEntry> entries;
	private long maxPartSize;

	private String baseName;
	private String extension;

	public ArchivePartFile(File baseFile, String password, long maxPartSize) {
		this(baseFile, password, new ArrayList<>(), maxPartSize);
	}

	public ArchivePartFile(File baseFile, String password, List<ArchiveEntry> entries, long maxPartSize) {
		this.baseFile = baseFile;
		this.password = password;
		this.entries = entries;
		this.maxPartSize = maxPartSize;

		baseName = FilenameUtils.getBaseName(baseFile.getName());
		extension = FilenameUtils.getExtension(baseFile.getName());
	}

	public File getBaseFile() {
		return baseFile;
	}

	public String getBaseName() {
		return baseName;
	}

	public String getExtension() {
		return extension;
	}

	public long getMaxPartSize() {
		return maxPartSize;
	}

	public List<ArchiveEntry> getEntries() {
		return entries;
	}

	public void setEntries(List<ArchiveEntry> entries) {
		this.entries = entries;
	}

	public void addEntry(ArchiveEntry e) {
		if(e != null && !entries.contains(e)) entries.add(e);
	}

	public String getPassword() {
		return password;
	}

	public boolean isEncrypted() {
		return password != null;
	}

	public int getHighestPartFileNumber() {
		int max = 1;
		for(ArchiveEntry e : entries) 
			if(e.getPart() > max) max = e.getPart();
		return max;
	}

	public File getPartFile(int part) {
		if(part < 1) throw new IllegalArgumentException("The part has to be bigger than 0!");
		return new File(baseFile.getParentFile(), baseName + "." + part + "." + extension);
	}

	public ArchiveEntryInputStream getInputStream(ArchiveEntry entry) throws IOException, GeneralSecurityException {
		if(entry == null) throw new IllegalArgumentException("The given entry is null! Cannot create input stream...");
		return new ArchiveEntryInputStream(this, entry);
	}

	public ArchiveEntry getArchiveEntryByFile(String file) {
		if(file == null) throw new IllegalArgumentException("The given file path is null! Cannot retrieve entry...");

		for(ArchiveEntry entry : entries) 
			if(entry.getFile().equals(file)) 
				return entry;

		return null;
	}
	
	public List<ArchiveEntry> getArchiveEntriesByPrefix(String prefix) {
		if(prefix == null) throw new IllegalArgumentException("The given file prefix is null! Cannot retrieve entries...");
		
		List<ArchiveEntry> gathered = new ArrayList<>();
		
		for(ArchiveEntry entry : entries) {
			if(entry.getFile().startsWith(prefix))
				gathered.add(entry);
		}
		
		return gathered;
	}

	public boolean setMaxPartSize(long maxPartSize) {
		long before = this.maxPartSize;
		this.maxPartSize = maxPartSize;

		boolean written = updateHeaderFile();

		if(!written) this.maxPartSize = before;
		return written;
	}

	private boolean updateHeaderFile() {
		BuilderState state = new BuilderState(null, 0L, 0, 0L, password); // only 'password' will be used
		boolean written = APBuilder.writeSummary(state, this);
		return written;
	}

	/**
	 * Adds the contents of the directory to the root of the archive.<br>
	 * <br>
	 * Example: <br>
	 * <code>f = new File("/path/to/folder");</code> would add all the contents of the folder to the root of the archive.
	 * Folders inside the specified folder will keep their structure.
	 * 
	 * If <code>f.isFile()</code> the file will be directly added to the root of the directory.
	 * 
	 * If you want to add a folder to the archive and create a folder inside the root of the archive instead of only adding the contents
	 * of the folder, use {@link ArchivePartFile#addFolderToArchive(File)}.
	 * 
	 * @param f The file that should be added to the archive. Can be a directory (recusive) or a single file.
	 * @return true, if everything was added successfully.
	 */	
	public boolean addToArchive(File f) {
		return addToArchive(f, false);
	}
	
	/**
	 * Adds the contents of the directory to the root of the archive.<br>
	 * <br>
	 * Example: <br>
	 * <code>f = new File("/path/to/folder");</code> would add all the contents of the folder to the root of the archive.
	 * Folders inside the specified folder will keep their structure.
	 * 
	 * If <code>f.isFile()</code> the file will be directly added to the root of the directory.
	 * 
	 * If you want to add a folder to the archive and create a folder inside the root of the archive instead of only adding the contents
	 * of the folder, use {@link ArchivePartFile#addFolderToArchive(File)}.
	 * 
	 * @param f The file that should be added to the archive. Can be a directory (recusive) or a single file.
	 * @param override True if existing entries with the same path should be replaced.
	 * @return true, if everything was added successfully.
	 */
	public boolean addToArchive(File f, boolean override) {
		if(!f.exists()) {
			System.err.println("\"" + f.getAbsolutePath() + "\" does not exist.");
			return false;
		}
		if(f.isFile()) return addToArchive(f, "/" + f.getName(), override);
		else return addToArchiveRecursively(f, f, false, override);
	}

	private boolean addToArchiveRecursively(File f, File root, boolean rootFolderPrefix, boolean override) {
		if(f.isDirectory()) {
			for(File children : f.listFiles()) 
				if(!addToArchiveRecursively(children, root, rootFolderPrefix, override)) return false;

			return true;
		}

		String path = f.getAbsolutePath().substring((rootFolderPrefix ? root.getParentFile() : root).getAbsolutePath().length()).replace(File.separatorChar, '/');
		return addToArchive(f, path, override);
	}

	/**
	 * Adds a folder to the root of the archive.<br>
	 * <br>
	 * Example: <br>
	 * <code>folder = new File("path/to/folder");</code> will add the folder "folder" and all its contents to the root of the archive. 
	 * 
	 * If you just want to add the contents of the folder to the root, use {@link ArchivePartFile#addToArchive(File)}. It can also be used
	 * to add single files.
	 * 
	 * @throws IllegalArgumentException if the specified file is not a folder.
	 * @param folder The folder to add to the archive.
	 * @return true, if everything was added successfully.
	 */
	public boolean addFolderToArchive(File folder) {
		return addFolderToArchive(folder, false);
	}
	
	/**
	 * Adds a folder to the root of the archive.<br>
	 * <br>
	 * Example: <br>
	 * <code>folder = new File("path/to/folder");</code> will add the folder "folder" and all its contents to the root of the archive. 
	 * 
	 * If you just want to add the contents of the folder to the root, use {@link ArchivePartFile#addToArchive(File)}. It can also be used
	 * to add single files.
	 * 
	 * @throws IllegalArgumentException if the specified file is not a folder.
	 * @param folder The folder to add to the archive.
	 * @param override True if existing entries with the same path should be replaced.
	 * @return true, if everything was added successfully.
	 */
	public boolean addFolderToArchive(File folder, boolean override) {
		if(folder.isFile()) throw new IllegalArgumentException("\"" + folder.getAbsolutePath() + "\" is not a directory!");
		if(folder.listFiles() == null || folder.listFiles().length <= 0) return true;

		return addToArchiveRecursively(folder, folder, true, override);
	}

	public boolean addToArchive(File f, String pathInsideArchive) {
		return addToArchive(f, pathInsideArchive, false);
	}
	
	public boolean addToArchive(File f, String pathInsideArchive, boolean override) {
		if(f == null || !f.exists()) {
			System.err.println("\"" + f + "\" does not exist.");
			return false;
		}
		
		ArchiveEntry en = getArchiveEntryByFile(pathInsideArchive);
		if(en != null) {
			if(override) removeEntry(en); 
			else {
				System.err.println("There is already an entry with path \"" + pathInsideArchive + "\" in this archive.");
				return false;
			}
		}

		final int maxPart = getHighestPartFileNumber();
		
		int currentPart = maxPart;
		for (int i = 1; i <= maxPart; i++) {
			File check = new File(baseFile.getParentFile(), FilenameUtils.getBaseName(baseFile.getName()) + "." + i + "." + extension);
			if(check.length() + f.length() <= maxPartSize) {
				currentPart = i;
				break;
			}
		}
		
		File currentFile = new File(baseFile.getParentFile(), FilenameUtils.getBaseName(baseFile.getName()) + "." + currentPart + "." + extension);
		if(!currentFile.exists()) ArchivePart.log("Now writing into \"" + currentFile.getAbsolutePath() + "\" ..."); // if the archive has no entries
		OutputStream currentOut;
		try {
			currentOut = APBuilder.openOutputStream(currentFile, true);
		} catch (GeneralSecurityException | IOException e) {
			e.printStackTrace();
			return false;
		}

		BuilderState state = new BuilderState(currentOut, currentFile.length(), currentPart, maxPartSize, password);

		if(!APBuilder.write(state, f, pathInsideArchive, this)) return false;

		sortEntries();
		
		return APBuilder.writeSummary(state, this);
	}

	public boolean removeEntry(ArchiveEntry entry) {
		if(entry == null) return true;
		
		File source = getPartFile(entry.getPart());
		if(!source.exists()) return false;

		File target = new File(source.getParentFile(), source.getName() + ".tmp");

		final long lengthBefore = entry.getOffset();
		final long lengthAfter = source.length() - lengthBefore - entry.getLength();
		
		try (RandomAccessFile raf = new RandomAccessFile(source, "r");
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target))) {
			final int maxBufferSize = 8 * 1024; //8KB

			long remain = lengthBefore;
			while(remain > 0) {
				int bufferSize = remain > maxBufferSize ? maxBufferSize : (int) remain;
				byte[] buf = new byte[bufferSize];
				if(raf.read(buf, 0, bufferSize) != -1) out.write(buf, 0, bufferSize);
				remain -= bufferSize;
			}
			
			raf.seek(entry.getOffset() + entry.getLength());
			
			remain = lengthAfter;
			while(remain > 0) {
				int bufferSize = remain > maxBufferSize ? maxBufferSize : (int) remain;
				byte[] buf = new byte[bufferSize];
				if(raf.read(buf, 0, bufferSize) != -1) out.write(buf, 0, bufferSize);
				remain -= bufferSize;
			}
			
			raf.close();
			out.close();
			
			if(!ArchivePart.delete(source)) return false;
			entries.remove(entry);
			
			for(ArchiveEntry en : entries) {
				if(en.getPart() != entry.getPart()) continue;
				if(en.getOffset() > entry.getOffset()) en.setOffset(en.getOffset() - entry.getLength());
			}
			
			sortEntries();
			
			updateHeaderFile();
			
			return target.renameTo(source);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private void sortEntries() {
		Collections.sort(entries, new Comparator<ArchiveEntry>() {

			@Override
			public int compare(ArchiveEntry e1, ArchiveEntry e2) {
				if(e1.getPart() != e2.getPart()) return e1.getPart() > e2.getPart() ? 1 : -1;
				return e1.getOffset() > e2.getOffset() ? 1 : -1;
			}
		});
	}

	public static ArchivePartFile parseFromBaseFile(File baseFile) throws APParseException {
		return parseFromBaseFile(baseFile, null);
	}

	public static ArchivePartFile parseFromBaseFile(File baseFile, String password) throws APParseException {
		return new APParser(baseFile).setPassword(password).parse();
	}

}
