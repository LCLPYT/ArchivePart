package test.test.archivepart.factory.readers;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import test.test.archivepart.crypto.CipherUtils;
import test.test.archivepart.crypto.XorInputStream;
import test.test.archivepart.factory.APParser;
import test.test.archivepart.factory.APReader;
import test.test.archivepart.model.APParseException;
import test.test.archivepart.model.ArchiveEntry;
import test.test.archivepart.model.ArchivePartFile;

public class APReaderV2 implements APReader{

	public static final byte[] CHECK_BYTES = "QVYZAulENKob2m7W".getBytes();

	@Override
	public ArchivePartFile read(DataInputStream in, APParser parser) throws Exception {
		boolean encrypted = in.readBoolean();
		long maxPartSize = in.readLong();

		String password = parser.getPassword();
		if(encrypted && password == null) throw new APParseException("This ArchivePart file is encrypted but no password was supplied.");

		int checkLength = in.readInt();
		byte[] checkRaw = new byte[checkLength];
		in.read(checkRaw);

		final String wrongPasswordText = "Wrong password! Please try again.";
		byte[] check = encrypted ? CipherUtils.translate(checkRaw, password) : checkRaw;
		if(!Arrays.equals(check, CHECK_BYTES)) throw new APParseException(wrongPasswordText);

		int contentLength = in.readInt();
		byte[] content = new byte[contentLength];
		in.read(content, 0, contentLength);

		List<ArchiveEntry> entries = new ArrayList<>();
		try {
			InputStream base = new ByteArrayInputStream(content);
			InputStream cipherBase = encrypted ? new XorInputStream(base, password) : base;
			DataInputStream input = new DataInputStream(cipherBase);

			//reading from back transformed stream

			int size = input.readInt();

			for (int i = 0; i < size; i++) {
				String file = input.readUTF();
				int part = input.readInt();
				long offset = input.readLong();
				long length = input.readLong();

				entries.add(new ArchiveEntry(file, part, offset, length));
			}

			input.close();
		} catch (Exception e) {
			if(encrypted) {
				APParseException ex = new APParseException("This file is encrypted. Make sure to pass the right parameters.");
				ex.addSuppressed(e);
				throw ex;
			} else throw e;
		}

		return new ArchivePartFile(parser.getFile(), encrypted ? password : null, entries, maxPartSize);
	}

}
