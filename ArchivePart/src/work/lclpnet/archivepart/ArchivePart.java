package work.lclpnet.archivepart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import work.lclpnet.archivepart.factory.APBuilder;
import work.lclpnet.archivepart.factory.APParser;
import work.lclpnet.archivepart.model.APParseException;
import work.lclpnet.archivepart.model.ArchiveEntry;
import work.lclpnet.archivepart.model.ArchivePartFile;

@Command(name = "java -jar ArchivePart.jar", mixinStandardHelpOptions = true, version = "ArchivePart 2.1", description = "Main command for archive part.")
public class ArchivePart implements Callable<Integer>{

	public static final int VERSION = 2;
	private static ArchivePart instance = null;

	public static void main(String[] args) {
		instance = new ArchivePart();
		System.exit(new CommandLine(instance).execute(args));
	}

	public static boolean delete(File file) {
		if (file == null) return true;

		if (file.isFile()) {
			return deleteSingle(file);
		}

		File[] files = file.listFiles();
		if (files == null) return deleteSingle(file);

		for (File f : files) {
			if (f.isDirectory()) delete(f);
			else deleteSingle(f);
		}

		if(file.listFiles() == null || file.listFiles().length <= 0) return deleteSingle(file);
		return false;
	}

	private static boolean deleteSingle(File file) {
		if (!file.delete()) {
			System.err.println("Unable to delete file \"" + file.getAbsolutePath() + "\".");
			return false;
		}
		return true;
	}

	public static ArchivePart getInstance() {
		return instance;
	}

	public static void log(String s) {
		ArchivePart instance = getInstance();
		if(instance == null) System.out.println(s);
		else if(!instance.silent) System.out.println(s);
	}

	@Parameters(index = "0", paramLabel = "action", description = "Actions: [${COMPLETION-CANDIDATES}]")
	APAction action;

	@Option(names = {"-i", "--input"}, description = "The input file to invoke with the specified action.")
	File input = null;

	@Option(names = {"-o", "--output"}, description = "The output file to invoke with the specified action.")
	File output = null;

	@Option(names = {"-mps", "--max-part-size"}, description = "The maximum file size of a part, in MB.", showDefaultValue = Visibility.ALWAYS)
	Integer maxPartSize = 500;

	@Option(names = {"-p", "--password"}, description = "Optional password to encrypt the file.")
	String password = null;

	@Option(names = {"-s", "--silent"}, description = "Only prints errors.", showDefaultValue = Visibility.ALWAYS)
	boolean silent = false;

	@Option(names = {"--conflict-strategy"}, description = "Strategy how to handle file conflicts. Available: [${COMPLETION-CANDIDATES}]")
	ConflictStrategy conflictStrategy = ConflictStrategy.SKIP_WARN;

	@Option(names = {"--path"}, description = "Path inside the archive.")
	String path = null;

	@Option(names = {"--dir-mode"}, description = "Only used by the add action. If set and the input file is a directory, it will add the whole folder to the root instead of just the contents (false).")
	boolean dirMode = false;

	@Option(names = {"-l", "--list"}, description = "Use list mode.")
	boolean listMode = false;

	@Option(names = {"--prefix", "--prefix-mode"}, description = "If set, and the option --path is also set, the path will be interpreted as prefix.")
	boolean prefixMode = false;
	
	@Override
	public Integer call() throws Exception {
		switch (action) {

		case BUILD: return build();
		case EXTRACT: return extract();
		case ADD: return add();
		case REMOVE: return remove();
		case ANALYSE: return analyse();

		default:
			return -1;
		}
	}

	private Integer analyse() {
		if(input == null) {
			System.err.println("For the analyse action you need to specify:");
			if(input == null) System.err.println("apHeaderFile (-i, --input): The input file to invoke with the specified action.");
			return 1;
		}

		try {
			ArchivePartFile apf = new APParser(input)
					.setPassword(password)
					.parse();

			if(apf == null) return 1;

			printInfo(apf);

			if(listMode) {
				log("Entries:");
				apf.getEntries().forEach(e -> log(e.toString()));
			}

			return 0;
		} catch (APParseException e) {
			e.printStackTrace();
			return 1;
		}
	}

	private Integer remove() {
		if(input == null || path == null) {
			System.err.println("For the analyse action you need to specify:");
			if(input == null) System.err.println("apHeaderFile (-i, --input): The input file to invoke with the specified action.");
			if(path == null) System.err.println("entryPath (--path): The path of the entry to remove inside the archive.");
			return 1;
		}

		try {
			ArchivePartFile apf = new APParser(input)
					.setPassword(password)
					.parse();

			if(apf == null) return 1;

			printInfo(apf);

			if(prefixMode) {
				removeMany(apf);
			} else {
				ArchiveEntry e = apf.getArchiveEntryByFile(path);
				if(e == null) {
					if(apf.getArchiveEntriesByPrefix(path.endsWith("/") ? path : path + "/").size() > 0) {
						prefixMode = true;
						removeMany(apf);
						return 0;
					}
					System.err.println("This archive does not contain an entry with path \"" + path + "\"!");
					return 1;
				}
				
				boolean success = apf.removeEntry(e);
				System.out.println((success ? "Successfully removed " : "Failed to remove ") + "'" + path + "' from the archive.");
				return success ? 0 : 1;
			}

			return 0;
		} catch (APParseException e) {
			e.printStackTrace();
			return 1;
		}
	}

	private void removeMany(ArchivePartFile apf) {
		List<ArchiveEntry> entries = apf.getArchiveEntriesByPrefix(path);
		removed = 0;

		System.out.println("Found " + entries.size() + " entries in path \"" + path + "\", removing them...");

		entries.forEach(e -> {
			if(apf.removeEntry(e)) removed++;
		});

		System.out.println("Done. Removed " + removed + " files.");
	}

	private Integer add() {
		if(input == null || output == null) {
			System.err.println("For the add action you need to specify:");
			if(input == null) System.err.println("file (-i, --input): The input file to be added to the ArchivePart.");
			if(output == null) System.err.println("outputApHeaderFile (-o, --output): The ArchivePart header file to add the input file to.");
			return 1;
		}

		try {
			ArchivePartFile apf = new APParser(output)
					.setPassword(password)
					.parse();

			if(apf == null) return 1;

			printInfo(apf);

			log("Adding \"" + input.getAbsolutePath() + "\" to the archive...");

			boolean multiple = input.isDirectory() && input.listFiles() != null && input.listFiles().length > 0;
			boolean override = conflictStrategy == ConflictStrategy.OVERRIDE;

			boolean success;
			if(multiple && dirMode) success = apf.addFolderToArchive(input, override);
			else success = path != null && !multiple ? apf.addToArchive(input, path, override) : apf.addToArchive(input, override);

			log(multiple ? "Files were " + (success ? "" : "not ") + "added successfully." : "File was " + (success ? "" : "not ") + "added successfully.");

			return success ? 0 : 1;
		} catch (APParseException e) {
			e.printStackTrace();
			return 1;
		}
	}

	private Integer extract() {
		if(input == null || output == null) {
			System.err.println("For the extract action you need to specify:");
			if(input == null) System.err.println("apHeaderFile (-i, --input): The input file to invoke with the specified action.");
			if(output == null) System.err.println("outputDir (-o, --output): The output dir to invoke with the specified action.");
			return 1;
		}

		try {
			ArchivePartFile apf = new APParser(input)
					.setPassword(password)
					.parse();

			if(apf == null) return 1;

			printInfo(apf);

			if(path != null) {
				ArchiveEntry e = apf.getArchiveEntryByFile(path);
				if(e == null) {
					System.err.println("This ArchivePart does not contain an entry with path \"" + path + "\"!");
					return 1;
				}

				extractEntry(apf, e);
			} else {
				apf.getEntries().forEach(e -> extractEntry(apf, e));
			}

			return 0;
		} catch (APParseException e) {
			e.printStackTrace();
			return 1;
		}
	}

	private void extractEntry(ArchivePartFile apf, ArchiveEntry e) {
		File f = new File(output, e.getFile().substring(1));
		if(f.exists()) {
			if(conflictStrategy == ConflictStrategy.SKIP_WARN) {
				log("\"" + e.getFile() + "\" already exists.");
				return;
			}
			else if(conflictStrategy == ConflictStrategy.SKIP) return;
		}
		if(!f.getParentFile().exists()) f.getParentFile().mkdirs();

		log("Extracting " + e.getFile() + "...");

		try (InputStream in = apf.getInputStream(e); OutputStream out = new FileOutputStream(f)) {
			IOUtils.copy(in, out);
		} catch (IOException | GeneralSecurityException ex) {
			ex.printStackTrace();
		}
	}

	private Integer build() {
		if(input == null) {
			System.err.println("For the build action you need to specify:");
			if(input == null) System.err.println("rootFile (-i, --input): The file to invoke with the specified action.");
			return 1;
		}

		ArchivePartFile apf = new APBuilder(input, output)
				.setPassword(password)
				.setMaxPartSize(maxPartSize * (long) Math.pow(1024D, 2D))
				.build();

		return apf != null ? 0 : 1;
	}

	private void printInfo(ArchivePartFile apf) {
		log("ArchivePart parsed informations:");
		log("Encrypted: " + (apf.getPassword() != null));
		log("Max Part Size: " + (apf.getMaxPartSize() / (long) Math.pow(1024D, 2D)) + " MB");
		log("Part files: " + apf.getHighestPartFileNumber());
		log("Files included: " + apf.getEntries().size());
		if(action != APAction.ANALYSE || listMode) log("");
	}

	private int removed = 0;

	enum ConflictStrategy {
		OVERRIDE,
		SKIP,
		SKIP_WARN;

		@Override
		public String toString() {
			return super.toString().toLowerCase();
		}
	}

	enum APAction {
		BUILD,
		EXTRACT,
		ADD,
		REMOVE,
		ANALYSE;

		@Override
		public String toString() {
			return super.toString().toLowerCase();
		}
	}

}