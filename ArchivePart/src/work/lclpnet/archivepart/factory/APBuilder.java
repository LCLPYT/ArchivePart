package work.lclpnet.archivepart.factory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;

import work.lclpnet.archivepart.crypto.CipherUtils;
import work.lclpnet.archivepart.crypto.XorOutputStream;
import work.lclpnet.archivepart.factory.readers.APReaderV1;
import work.lclpnet.archivepart.model.ArchiveEntry;
import work.lclpnet.archivepart.model.ArchivePartFile;
import work.lclpnet.archivepart.model.Triplet;
import work.lclpnet.archivepart.ArchivePart;

public class APBuilder {

	private File root, output;
	private long maxPartSize = (long) Math.pow(1024D, 3D);
	private String password = null;

	private int currentPart = 0;
	private long currentLength = 0L;
	private OutputStream currentOut = null;
	private boolean oldVersionDeletedSuccessfully = false;

	public APBuilder(File root, File output) {
		if(root == null || !root.exists() || !root.isDirectory()) throw new IllegalArgumentException("The file \"" + root.getAbsolutePath() + "\" is not a valid root directory.");
		if(output == null) output = new File(root.getParentFile(), root.getName() + ".ap");

		this.root = root;
		this.output = output;
	}

	public APBuilder setMaxPartSize(long maxPartSize) {
		this.maxPartSize = maxPartSize;
		return this;
	}

	public long getMaxPartSize() {
		return maxPartSize;
	}

	public APBuilder setPassword(String password) {
		this.password = password;
		return this;
	}

	public String getPassword() {
		return password;
	}

	public synchronized ArchivePartFile build() {
		if(!deleteOldVersion(FilenameUtils.getExtension(output.getName()))) {
			System.err.println("Error, couldn't delete old version of output file. ArchivePart will not finish the build.");
			return null;
		}

		ArchivePart.log("Building archive parts...");
		if(password != null) ArchivePart.log("Using XOR encryption...");

		ArchivePartFile apFile = new ArchivePartFile(output, password, maxPartSize);
		if(!recursive(root, root.getAbsolutePath(), apFile)) {
			System.err.println("Error, there has been an error. ArchivePart will not finish the build.");
			return null;
		}

		ArchivePart.log("Archive parts have been built.");

		ArchivePart.log("Writing summary...");

		BuilderState state = new BuilderState(currentOut, currentLength, currentPart, maxPartSize, password);
		if(!writeSummary(state, apFile)) return null;

		ArchivePart.log("An ArchivePart has been built.");

		return apFile;
	}

	private boolean deleteOldVersion(String ext) {
		oldVersionDeletedSuccessfully = output.exists() ? ArchivePart.delete(output) : true;

		final Pattern p = Pattern.compile(FilenameUtils.getBaseName(output.getName()) + "\\.[0-9]+\\." + ext + "(\\.tmp)?");

		Arrays.stream(output.getParentFile().listFiles())
		    .filter(f -> (f.isFile() && p.matcher(f.getName()).matches()))
		    .forEach(f -> oldVersionDeletedSuccessfully |= ArchivePart.delete(f));
		
		return oldVersionDeletedSuccessfully;
	}

	private boolean recursive(File parent, String rootPath, ArchivePartFile apFile) {
		File[] children = parent.listFiles();
		if(children == null) return true;

		for(File f : children) {
			if(f.isDirectory()) {
				if(!recursive(f, rootPath, apFile)) return false;
				else continue;
			}

			String path = f.getAbsolutePath().substring(rootPath.length()).replace(File.separatorChar, '/');
			if(!apFile.addToArchive(f, path)) return false;
		}
		return true;
	}

	public static boolean writeSummary(BuilderState state, ArchivePartFile apFile) {
		try {
			if(state.currentOut != null) state.currentOut.close();

			boolean encrypted = state.password != null;

			File f = new File(apFile.getBaseFile().getParent(), apFile.getBaseName() + "." + apFile.getExtension());

			DataOutputStream out = new DataOutputStream(new FileOutputStream(f));
			out.writeInt(ArchivePart.VERSION);
			out.writeBoolean(encrypted);
			out.writeLong(apFile.getMaxPartSize());

			final byte[] match = APReaderV1.CHECK_BYTES;
			byte[] checkBytes = encrypted ? CipherUtils.translate(match, state.password) : APReaderV1.CHECK_BYTES;
			out.writeInt(checkBytes.length);
			out.write(checkBytes, 0, checkBytes.length);

			//From here, the data is first written into a temp stream. 
			//Any transformations are applied first and afterwards written into the main stream. 
			ByteArrayOutputStream dummyByteOut = new ByteArrayOutputStream();

			//If the file should be encrypted, this will implement a CipherOutputStream.
			OutputStream cipherBase = encrypted ? new XorOutputStream(dummyByteOut, state.password) : dummyByteOut;

			//The DataOutputStream used to write data types.
			DataOutputStream dummyOut = new DataOutputStream(cipherBase);

			//Writing raw data
			List<ArchiveEntry> entries = apFile.getEntries();
			dummyOut.writeInt(entries.size());

			for (int i = 0; i < entries.size(); i++) {
				ArchiveEntry e = entries.get(i);
				if(e == null) continue;

				dummyOut.writeUTF(e.getFile());
				dummyOut.writeInt(e.getPart());
				dummyOut.writeLong(e.getOffset());
				dummyOut.writeLong(e.getLength());
			}

			//writing to the temp stream is now finished
			dummyOut.close();

			//writing bytes from temp stream to main stream.
			byte[] byteArray = dummyByteOut.toByteArray();
			out.writeInt(byteArray.length);
			out.write(byteArray, 0, byteArray.length);

			out.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static boolean write(BuilderState state, File f, String path, ArchivePartFile apFile) {
		final long length = f.length();
		if(length > state.maxPartSize) {
			System.err.println("Error, \"" + f.getAbsolutePath() + "\" is bigger than the specified maximumPartSize (" + state.maxPartSize + " bytes). This program is not yet designed to store files bigger than the maximum part size.");
			return true;
		}

		try {
			if(state.currentOut == null) { //For the first
				Triplet<File, Integer, Long> result = APBuilder.getNextOutputFile(state, apFile.getBaseFile());
				state.currentLength = result.c;
				state.currentPart = result.b;

				File out = result.a;
				if(out == null) return false;

				state.currentOut = new BufferedOutputStream(APBuilder.openOutputStream(out));
			}

			if(state.currentLength + length > state.maxPartSize) {
				state.currentOut.close();

				Triplet<File, Integer, Long> result = APBuilder.getNextOutputFile(state, apFile.getBaseFile());
				state.currentLength = result.c;
				state.currentPart = result.b;

				File nextOut = result.a;
				if(nextOut == null) return false;

				state.currentOut = new BufferedOutputStream(APBuilder.openOutputStream(nextOut));
			}

			BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));

			byte[] buffer = new byte[8192];
			int read;
			int offset = 0;
			String keyBefore = state.password != null ? new String(state.password) : null;
			while ((read = in.read(buffer)) != -1) {
				byte[] write;
				if(state.password != null) {
					keyBefore = APBuilder.shift(keyBefore, -offset);
					write = CipherUtils.translate(buffer, keyBefore);
					offset = write.length % keyBefore.length();
				} else write = buffer;

				state.currentOut.write(write, 0, read);
			}

			in.close();

			apFile.addEntry(new ArchiveEntry(path, state.currentPart, state.currentLength, length));
			state.currentLength += length;

			return true;
		} catch (IOException | GeneralSecurityException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static String shift(String s, int offset) {
		offset = offset % s.length();
		if(offset == 0) return s;
		if(offset < 0) offset = s.length() + offset;

		return s.substring(s.length() - offset, s.length()) + s.substring(0, s.length() - offset);
	}

	public static Triplet<File, Integer, Long> getNextOutputFile(BuilderState state, File main) {
		state.currentPart++;
		state.currentLength = 0L;

		File nextOut = new File(main.getParentFile(), FilenameUtils.getBaseName(main.getName()) + "." + state.currentPart + "." + "ap");

		ArchivePart.log("Now writing into \"" + nextOut.getAbsolutePath() + "\" ...");

		return Triplet.of(nextOut, state.currentPart, state.currentLength);
	}

	public static OutputStream openOutputStream(File f) throws GeneralSecurityException, IOException {
		return new FileOutputStream(f, false);
	}

	public static OutputStream openOutputStream(File f, boolean append) throws GeneralSecurityException, IOException {
		return new FileOutputStream(f, append);
	}

}
