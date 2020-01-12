package work.lclpnet.archivepart.factory;

import java.io.DataInputStream;

import work.lclpnet.archivepart.model.ArchivePartFile;

public interface APReader {

	ArchivePartFile read(DataInputStream in, APParser parser) throws Exception;
	
}
