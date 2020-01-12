package test.test.archivepart.factory;

import java.io.DataInputStream;

import test.test.archivepart.model.ArchivePartFile;

public interface APReader {

	ArchivePartFile read(DataInputStream in, APParser parser) throws Exception;
	
}
