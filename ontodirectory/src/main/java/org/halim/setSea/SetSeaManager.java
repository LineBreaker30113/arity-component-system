package org.halim.setSea;

import org.halim.legacy.dlake.DataLakeManager;
import org.halim.nStructure.archive.core.ArchiveManager;
import org.halim.nStructure.archive.core.FileMetadataCatalog;
import org.halim.nStructure.archive.core.LakeFormatMismatchException;
import org.halim.nStructure.mhexagon.CircularFifoQueue;
import org.halim.nStructure.mhexagon.OntoDirectoryService;
import org.halim.nStructure.mhexagon.OntologyClassHandle;
import org.halim.nStructure.mhexagon.OntologyElementHandle;
import org.halim.nStructure.mhexagon.OntologyReadingService;
import org.halim.nStructure.ontology.core.segmentation.query.OntologyElementFilter;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * One Set Sea, the folder the user picked: the ontology-package lake of {@link ArchiveManager} with its
 * five catalog files replaced by an ACSE store, the {@link SetSeaDatabase}, at the folder's root, and
 * {@value #SEA_PATH_SUFFIX} in place of {@code .DATA_LAKE}. The files sit in {@value #SEA_PATH_SUFFIX}
 * under the disk names {@link FileMetadataCatalog#generateDiskName} makes, kept in the database's disk
 * name row; imports, exports and bug reports are where the lake keeps them.
 * <p>
 * Each GUI action is one edit of the database, so one undo. What happens on disk is not undone: a file
 * imported stays in {@value #SEA_PATH_SUFFIX} when its import is undone, and a deleted file is gone when
 * its deletion is undone, as in the lake. Every filter is answered as the lake answers it, but Direct
 * Element Of the root: the lake keeps a file's fallback connection to the root after the file is tagged
 * again, while here the root's direct files are exactly those holding no tag. Vacuum is not offered,
 * as in the lake.
 * */
public class SetSeaManager implements OntologyReadingService.OntologyManagingService, OntoDirectoryService.DataLakeService {

public static final String SEA_PATH_SUFFIX = ".SET_SEA";

static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

public final Path managerPath;
final SetSeaDatabase database;
final SetSeaOntologyService ontologyService;
final List<DataLakeServiceListener> seaListeners = new ArrayList<>();
final List<OntologyServiceListener> ontologyListeners = new ArrayList<>();
final CircularFifoQueue<String> activityLedger = new CircularFifoQueue<>(1000);
int domainIdentity = SetSeaDatabase.ROOT;

/** Opens the sea stored in {@code managerPath}, or makes one there; refuses a folder holding a lake of either earlier format. */
public SetSeaManager(Path managerPath) {
	this.managerPath = managerPath;
	if(Files.exists(managerPath.resolve(ArchiveManager.LAKE_PATH_SUFFIX)) || Files.exists(managerPath.resolve(ArchiveManager.CONFIG_FILE_SUFFIX))
		  || Files.exists(managerPath.resolve(DataLakeManager.hierarchyPathSuffix)) || Files.exists(managerPath.resolve(DataLakeManager.elementsPathSuffix))) {
		throw new LakeFormatMismatchException("Directory already holds a Data Lake of an earlier format: " + managerPath + " -- open it with the app that made it, not Set Sea.");
	}
	try {
		if(SetSeaDatabase.isStored(managerPath)) {
			this.database = SetSeaDatabase.load(managerPath);
			logActivity("SEA_HYDRATED");
		} else {
			Files.createDirectories(getSeaPath());
			Files.createDirectories(getSeaImports());
			Files.createDirectories(getSeaExports());
			Files.createDirectories(getBugReportsDir());
			this.database = SetSeaDatabase.create(managerPath);
			logActivity("SEA_CREATED");
		}
	} catch(IOException e) {
		throw new UncheckedIOException("Failed to open the Set Sea at " + managerPath + ": " + e.getMessage(), e);
	}
	this.ontologyService = new SetSeaOntologyService(database);
}

// --- Path helpers ---

public Path getSeaPath() { return managerPath.resolve(SEA_PATH_SUFFIX); }
public Path getSeaImports() { return managerPath.resolve(ArchiveManager.IMPORTS_PATH_SUFFIX); }
public Path getSeaExports() { return managerPath.resolve(ArchiveManager.EXPORTS_PATH_SUFFIX); }
public Path getBugReportsDir() { return managerPath.resolve(ArchiveManager.BUG_REPORTS_SUFFIX); }

/** Where the file's content is, or null for an identity with no disk name. */
public Path getActualFile(int identity) {
	String diskName = database.diskName(identity);
	return diskName.isEmpty() ? null : getSeaPath().resolve(diskName);
}

public SetSeaDatabase getDatabase() { return database; }
public boolean isDirty() { return database.isDirty(); }
public Iterable<String> getActivityLedger() { return activityLedger; }

// --- Lifecycle ---

@Override
public void saveChanges() {
	try {
		List<String> warnings = database.store();
		for(String warning : warnings) logActivity("STORE_WARNING:" + warning);
		logActivity("SAVE_CHANGES");
	} catch(IOException e) {
		throw new UncheckedIOException("Failed to save the Set Sea: " + e.getMessage(), e);
	}
}

@Override
public void executeVacuum() {
	throw new UnsupportedOperationException("Vacuum is not offered for a Set Sea: identities are reused by the arities, so there is nothing to compact");
}

@Override
public Path generateDiagnosticDump(Throwable ex) {
	try {
		Files.createDirectories(getBugReportsDir());
		Path dumpFile = getBugReportsDir().resolve("crash_dump_" + System.currentTimeMillis() + ".txt");
		try(PrintWriter writer = new PrintWriter(Files.newBufferedWriter(dumpFile))) {
			writer.println("ONTO DIRECTORY (Set Sea) CRASH DUMP");
			writer.println("Timestamp: " + new Date());
			writer.println("Sea: " + managerPath);
			writer.println("\n--- ACTIVITY LEDGER ---");
			for(String entry : activityLedger) writer.println(entry);
			writer.println("\n--- EXCEPTION ---");
			if(ex != null) ex.printStackTrace(writer);
		}
		return dumpFile;
	} catch(IOException e) {
		throw new UncheckedIOException(e);
	}
}

@Override
public void logActivity(String action) {
	synchronized(activityLedger) { activityLedger.add(TIMESTAMP_FORMAT.format(new Date()) + " | " + action); }
}

// --- Import ---

/** Imports what sits in imports/, then empties it: the files are moved in, not copied. */
@Override
public void importFiles() {
	try {
		importFiles(getSeaImports());
		Files.walkFileTree(getSeaImports(), new SimpleFileVisitor<Path>() {
			@Override public FileVisitResult postVisitDirectory(Path dir, IOException exp) throws IOException {
				if(!dir.equals(getSeaImports())) Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
			@Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}
		});
		logActivity("CLEAR_UP_AFTER_IMPORT");
	} catch(IOException e) {
		throw new UncheckedIOException("Import clean-up failed: " + e.getMessage(), e);
	}
}

/** One undo for the whole import: a tag named for its time, a tag per folder below it, and each file tagged with its folder's tag. */
@Override
public void importFiles(Path sourceDirectory) throws IOException {
	if(!Files.exists(sourceDirectory)) {
		if(sourceDirectory.equals(getSeaImports())) Files.createDirectories(sourceDirectory);
		return;
	}
	try {
		database.edit("import " + sourceDirectory, () -> {
			int importTag = database.createTag("imported_at_" + TIMESTAMP_FORMAT.format(new Date()));
			Map<Path, Integer> dirToTag = new HashMap<>();
			dirToTag.put(sourceDirectory, importTag);
			try {
				Files.walkFileTree(sourceDirectory, new SimpleFileVisitor<Path>() {
					@Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
						if(dir.equals(sourceDirectory)) return FileVisitResult.CONTINUE;
						int tag = database.createTag(dir.getFileName().toString());
						database.addParent(tag, dirToTag.getOrDefault(dir.getParent(), importTag));
						dirToTag.put(dir, tag);
						return FileVisitResult.CONTINUE;
					}
					@Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						importOne(file, dirToTag.getOrDefault(file.getParent(), importTag));
						return FileVisitResult.CONTINUE;
					}
				});
			} catch(IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	} catch(UncheckedIOException e) {
		throw e.getCause();
	}
	notifySeaListeners();
	logActivity("BULK_IMPORT:" + sourceDirectory);
}

void importOne(Path physicalFile, int tag) throws IOException {
	int file = database.createFile(physicalFile.getFileName().toString());
	String diskName = FileMetadataCatalog.generateDiskName(file);
	Files.copy(physicalFile, getSeaPath().resolve(diskName), StandardCopyOption.REPLACE_EXISTING);
	database.setDiskName(file, diskName);
	database.tag(tag, file);
}

// --- Export ---

@Override
public void exportFiles(OntologyElementFilter filter) { exportFiles(filter, getSeaExports()); }

@Override
public void exportFiles(OntologyElementFilter filter, Path destinationFolder) {
	try {
		exportInternal(getOntologyElements(filter), destinationFolder);
		logActivity("EXPORT_DOMAIN:" + destinationFolder);
	} catch(IOException e) {
		throw new UncheckedIOException("Export failed: " + e.getMessage(), e);
	}
}

@Override
public void exportSpecificFiles(List<OntologyElementHandle> elements, Path destinationFolder) {
	try {
		exportInternal(elements, destinationFolder);
		logActivity("EXPORT_SPECIFIC:" + destinationFolder);
	} catch(IOException e) {
		throw new UncheckedIOException("Export failed: " + e.getMessage(), e);
	}
}

void exportInternal(List<OntologyElementHandle> elements, Path destinationFolder) throws IOException {
	if(!Files.exists(destinationFolder)) Files.createDirectories(destinationFolder);
	for(OntologyElementHandle element : elements) {
		Path actualFile = getActualFile(element.getIdentity());
		if(actualFile != null) Files.copy(actualFile, destinationFolder.resolve(element.getActualName()), StandardCopyOption.REPLACE_EXISTING);
	}
}

// --- OntologyReadingService ---

@Override
public OntologyClassHandle getRootOntologyClass() { return getClassFromIdentity(SetSeaDatabase.ROOT); }

@Override
public OntologyClassHandle getClassFromIdentity(int identity) { return new OntologyClassHandle(identity, ontologyService); }

@Override
public int getDomainIdentity() { return domainIdentity; }

@Override
public void setDomain(OntologyClassHandle domain) {
	this.domainIdentity = domain.getIdentity();
	for(OntologyServiceListener l : ontologyListeners) l.setOntologyDomain(domain);
}

@Override
public ArrayList<OntologyElementHandle> getOntologyElements(int classIdentity) { return handles(database.directFilesOf(classIdentity)); }

@Override
public ArrayList<OntologyElementHandle> getAllOntologyElements(int classIdentity) { return handles(database.domainFilesOf(classIdentity)); }

@Override
public boolean isElementForFilter(OntologyClassHandle ontologyClass, OntologyElementHandle element) {
	return database.holds(ontologyClass.getIdentity(), element.getIdentity());
}

@Override
public boolean isDescendentForFilter(OntologyClassHandle ontologyClass, OntologyElementHandle element) {
	for(int file : database.domainFilesOf(ontologyClass.getIdentity())) if(file == element.getIdentity()) return true;
	return false;
}

/**
 * Routed as the ontology package's resolver routes it: a node that prefers enumerating lists its files,
 * one that prefers testing is tested against every file, and an And enumerates its enumerating side and
 * tests the other against that. A node met twice in one resolution is listed once, through the memo.
 * */
@Override
public List<OntologyElementHandle> getOntologyElements(OntologyElementFilter filter) {
	HashMap<OntologyElementFilter, IntOpenHashSet> memo = new HashMap<>();
	return handles(filter.isEnumeratePreferred() ? enumerate(filter, memo) : kept(database.fileIdentities(), filter, memo));
}

int[] enumerate(OntologyElementFilter filter, Map<OntologyElementFilter, IntOpenHashSet> memo) {
	return switch(filter) {
		case OntologyElementFilter.DirectElementOf(int tag) -> database.directFilesOf(tag);
		case OntologyElementFilter.UnderDomain(int tag) -> database.domainFilesOf(tag);
		case OntologyElementFilter.And(var left, var right) -> left.isEnumeratePreferred()
				? kept(enumerate(left, memo), right, memo) : kept(enumerate(right, memo), left, memo);
		case OntologyElementFilter.Or(var left, var right) -> union(enumerate(left, memo), enumerate(right, memo));
		case OntologyElementFilter.Subtract(var kept, var excluded) -> {
			IntArrayList result = new IntArrayList();
			for(int file : enumerate(kept, memo)) if(!test(excluded, file, memo)) result.add(file);
			yield result.toIntArray();
		}
		case OntologyElementFilter.Custom(var implementation) -> implementation.enumerate().toIntArray();
		case OntologyElementFilter.NameContaining ignored -> throw new IllegalStateException("Name Contains never prefers enumerating; a combinator routed an enumeration into it");
	};
}

boolean test(OntologyElementFilter filter, int file, Map<OntologyElementFilter, IntOpenHashSet> memo) {
	return switch(filter) {
		case OntologyElementFilter.DirectElementOf(int tag) -> database.holds(tag, file);
		case OntologyElementFilter.UnderDomain ignored -> memo.computeIfAbsent(filter, f -> new IntOpenHashSet(enumerate(f, memo))).contains(file);
		case OntologyElementFilter.NameContaining(var query) -> {
			String name = database.fileName(file);
			yield name != null && name.toLowerCase().contains(query.toLowerCase());
		}
		case OntologyElementFilter.And(var left, var right) -> test(left, file, memo) && test(right, file, memo);
		case OntologyElementFilter.Or(var left, var right) -> test(left, file, memo) || test(right, file, memo);
		case OntologyElementFilter.Subtract(var kept, var excluded) -> test(kept, file, memo) && !test(excluded, file, memo);
		case OntologyElementFilter.Custom(var implementation) -> implementation.test(file);
	};
}

int[] kept(int[] files, OntologyElementFilter filter, Map<OntologyElementFilter, IntOpenHashSet> memo) {
	IntArrayList result = new IntArrayList();
	for(int file : files) if(test(filter, file, memo)) result.add(file);
	return result.toIntArray();
}

static int[] union(int[] left, int[] right) {
	IntOpenHashSet seen = new IntOpenHashSet();
	IntArrayList result = new IntArrayList();
	for(int file : left) if(seen.add(file)) result.add(file);
	for(int file : right) if(seen.add(file)) result.add(file);
	return result.toIntArray();
}

@Override
public void addOntologyServiceListener(OntologyServiceListener listener) { ontologyListeners.add(listener); }

ArrayList<OntologyElementHandle> handles(int[] files) {
	ArrayList<OntologyElementHandle> result = new ArrayList<>(files.length);
	for(int file : files) result.add(new OntologyElementHandle(file, ontologyService));
	return result;
}

// --- OntologyManagingService ---

@Override
public int filterToClass(OntologyElementFilter filter, String newClassName) {
	return database.edit("save filter as " + newClassName, () -> {
		List<OntologyElementHandle> matches = getOntologyElements(filter);
		int tag = database.createTag(newClassName);
		for(OntologyElementHandle element : matches) database.tag(tag, element.getIdentity());
		return tag;
	});
}

/** Best-effort, as in the lake: a parent edge the hierarchy refuses is skipped rather than ending the copy. */
@Override
public void copyContentsTo(int sourceIdentity, int targetIdentity, boolean copyParents, boolean copyChildren, boolean copyFiles) {
	database.edit("copy contents of " + sourceIdentity + " to " + targetIdentity, () -> {
		if(copyParents) for(int p : database.parentsOf(sourceIdentity)) {
			try { database.addParent(targetIdentity, p); } catch(RuntimeException ignored) { }
		}
		if(copyChildren) for(int c : database.childrenOf(sourceIdentity)) {
			try { database.addParent(c, targetIdentity); } catch(RuntimeException ignored) { }
		}
		if(copyFiles) for(int e : database.directFilesOf(sourceIdentity)) database.tag(targetIdentity, e);
	});
}

@Override
public int createOntologyClass(String name, List<Integer> parentIds, List<Integer> childrenIds) {
	return database.edit("create tag " + name, () -> {
		int tag = database.createTag(name);
		if(parentIds != null) for(int p : parentIds) if(p != SetSeaDatabase.ROOT) database.addParent(tag, p);
		if(childrenIds != null) for(int c : childrenIds) database.addParent(c, tag);
		return tag;
	});
}

@Override
public void removeOntologyClass(int classIdentity) { database.removeTag(classIdentity); }

@Override
public void addParent(int childId, int parentId) { database.addParent(childId, parentId); }

@Override
public void removeParent(int childId, int parentId) { database.removeParent(childId, parentId); }

@Override
public void addElement(int classIdentity, int elementIdentity) { database.tag(classIdentity, elementIdentity); }

@Override
public void removeElement(int classIdentity, int elementIdentity) { database.untag(classIdentity, elementIdentity); }

@Override
public void renameOntologyClass(int classIdentity, String newName) { database.renameTag(classIdentity, newName); }

@Override
public void renameElementActualName(int elementIdentity, String newName) { database.renameFile(elementIdentity, newName); }

@Override
public boolean canUndo() { return database.canUndo(); }

@Override
public boolean canRedo() { return database.canRedo(); }

@Override
public void undo() { database.undo(); }

@Override
public void redo() { database.redo(); }

/** The files on disk go first, then one edit removes them all; undoing it brings back the entries, not the files. */
@Override
public void deleteElements(List<OntologyElementHandle> selectedElements) {
	for(OntologyElementHandle element : selectedElements) {
		Path actualFile = getActualFile(element.getIdentity());
		if(actualFile != null) {
			try { Files.deleteIfExists(actualFile); } catch(IOException e) { logActivity("DELETE_FILE_FAILED:" + element.getIdentity()); }
		}
	}
	database.edit("delete " + selectedElements.size() + " files", () -> {
		for(OntologyElementHandle element : selectedElements) database.removeFile(element.getIdentity());
	});
}

// --- mhexagon.OntoDirectoryService.DataLakeService ---

@Override
public OntologyReadingService getOntologyReadingService() { return this; }

@Override
public OntologyReadingService.OntologyManagingService getOntologyManagingService() { return this; }

@Override
public Path getRootPath() { return managerPath; }

@Override
public void addDataLakeServiceListener(DataLakeServiceListener listener) {
	if(!seaListeners.contains(listener)) seaListeners.add(listener);
}

void notifySeaListeners() {
	for(DataLakeServiceListener listener : seaListeners) listener.onChange();
}

}
