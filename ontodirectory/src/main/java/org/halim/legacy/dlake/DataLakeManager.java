package org.halim.legacy.dlake;

import org.halim.legacy.pd.CrashReporter;
import org.halim.legacy.OntoDirectoryException;
import org.halim.legacy.pd.CircularFifoQueue;
import org.halim.legacy.pd.OntoDirectoryService;
import org.halim.legacy.pd.OntologyReadingService;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class DataLakeManager implements OntoDirectoryService.DataLakeService {

public static final String lakePathSuffix = ".DATA_LAKE",
	  configFileSuffix = ".odConfig.bin",
	  importsPathSuffix = "imports",
	  exportsPathSuffix = "exports",
	  hierarchyPathSuffix = ".ontologyClasses.bin",
	  elementsPathSuffix = ".identities.bin",
	  bugReportsSuffix = "bugReports",
	  breadcrumbsSuffix = ".breadcrumbs.log";

public static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

public final Path managerPath;
public boolean isCorrupted = false;

private final ReentrantReadWriteLock ontoLock = new ReentrantReadWriteLock();
private final AtomicBoolean dirtyFlag = new AtomicBoolean(false);

public OntologyHierarchyFast ontologyHierarchy;
private final OntologyStorageService storageService;
private final List<DataLakeServiceListener> listeners = new ArrayList<>();

private final CircularFifoQueue<String> userActionLedger = new CircularFifoQueue<>(1000);

public Path getLakePath() { return this.managerPath.resolve(lakePathSuffix); }
public Path getLakeConfig() { return this.managerPath.resolve(configFileSuffix); }
public Path getLakeImports() { return this.managerPath.resolve(importsPathSuffix); }
public Path getLakeExports() { return this.managerPath.resolve(exportsPathSuffix); }
public Path getLakeHierarchy() { return this.managerPath.resolve(hierarchyPathSuffix); }
public Path getLakeElements() { return this.managerPath.resolve(elementsPathSuffix); }
public Path getBugReportsDir() { return org.halim.legacy.pd.CrashReporter.getUniversalBugReportsDir(); }
public Path getBreadcrumbsFile() { return getBugReportsDir().resolve(breadcrumbsSuffix); }

private final OntologyReadingService.OntologyManagingService rawHierarchyManager;
private final ThreadSafeReadingService safeReadingService;
private final ThreadSafeManagingService safeManagingService;

public DataLakeManager(@NotNull Path managerPath, @NotNull OntologyStorageService storageService) {
	this.managerPath = managerPath;
	this.storageService = storageService;
	
	if (!Files.exists(getLakeConfig())) {
		createLake();
	} else {
		try {
			ontologyHierarchy = new OntologyHierarchyFast();
			storageService.loadOntologyHierarchyFromFile(getLakeHierarchy(), ontologyHierarchy);
			ontologyHierarchy.fileInterfaces = storageService.loadOntologyElementsFromFile(getLakeElements());
			
			for (FileInterface fi : ontologyHierarchy.fileInterfaces) {
				if (fi != null && fi.diskName != null) {
					fi.actualFile = getLakePath().resolve(fi.diskName);
				}
			}
			
			ontologyHierarchy.onLoad();
			logSystemEvent("LAKE_HYDRATED", "SUCCESS", "INFO");
		} catch (OntoDirectoryException.StorageFileLockedError | OntoDirectoryException.StorageFileCorruptedError e) {
			logSystemEvent("LAKE_HYDRATED", "FATAL_CORRUPTION", "FATAL");
			isCorrupted = true;
			ontologyHierarchy = new OntologyHierarchyFast();
		} catch (IOException e) {
			throw new OntoDirectoryException("Failed to hydrate Data Lake from disk: " + e.getMessage());
		}
	}
	
	initBreadcrumbs();
	this.rawHierarchyManager = ontologyHierarchy.new OntologyHierarchyManager();
	this.safeReadingService = new ThreadSafeReadingService(rawHierarchyManager);
	this.safeManagingService = new ThreadSafeManagingService(rawHierarchyManager);
}

public boolean isDirty() { return dirtyFlag.get(); }

private void createLake() {
	try {
		Files.createDirectories(getLakePath());
		Files.createFile(getLakeConfig());
		Files.createDirectories(getLakeImports());
		Files.createDirectories(getLakeExports());
		Files.createDirectories(getBugReportsDir());
	} catch (IOException e) {
		throw new OntoDirectoryException("Failed to build directory structure: " + e.getMessage());
	}
	ontologyHierarchy = new OntologyHierarchyFast();
	dirtyFlag.set(true);
	saveChanges();
	logSystemEvent("LAKE_CREATED", "SUCCESS", "INFO");
}

@Override
public void executeVacuum() {
	String reqId = generateCorrelationId();
	logUserIntent(reqId, "VACUUM_DB", "ALL");
	ontoLock.writeLock().lock();
	try {
		if (isCorrupted) {
			logUserCommit(reqId, "ABORTED_CORRUPTED", "ALL", "WARN");
			return;
		}
		ontologyHierarchy.vacuumDatabase(getLakePath());
		dirtyFlag.set(true);
		logUserCommit(reqId, "SUCCESS", "ALL", "INFO");
		notifyListeners();
		saveChanges();
	} catch (Exception ex) {
		logUserCommit(reqId, "FATAL_" + ex.getClass().getSimpleName(), "ALL", "FATAL");
		generateDiagnosticDump(ex);
		throw new OntoDirectoryException("Vacuum Failed: " + ex.getMessage());
	} finally {
		ontoLock.writeLock().unlock();
	}
}

@Override
public Path generateDiagnosticDump(Throwable ex) {
	ontoLock.readLock().lock();
	try {
		flushBreadcrumbsToDiskSync();
		return CrashReporter.generateCrashDump(ex, this);
	} finally {
		ontoLock.readLock().unlock();
	}
}

private void initBreadcrumbs() {
	try {
		if (!Files.exists(getBugReportsDir())) Files.createDirectories(getBugReportsDir());
		if (Files.exists(getBreadcrumbsFile())) {
			List<String> lines = Files.readAllLines(getBreadcrumbsFile(), StandardCharsets.UTF_8);
			int start = Math.max(0, lines.size() - CrashReporter.MAX_BREADCRUMBS);
			for (int i = start; i < lines.size(); i++) {
				String line = lines.get(i);
				if (!line.contains("[SYS]")) userActionLedger.add(line);
			}
		}
	} catch (IOException ignored) {}
}

private String generateCorrelationId() { return UUID.randomUUID().toString().substring(0, 8).toUpperCase(); }

private void logUserIntent(String reqId, String intent, String target) {
	String entry = String.format("%s | [LEVEL: INFO] [REQ_ID: %s] [INTENT: %s] [TARGET: %s] [STATUS: PENDING]",
		  simpleDateFormat.format(new Date()), reqId, intent, target);
	userActionLedger.add(entry);
}

private void logUserCommit(String reqId, String status, String target, String level) {
	String entry = String.format("%s | [LEVEL: %s] [REQ_ID: %s] [COMMIT: %s] [TARGET: %s]",
		  simpleDateFormat.format(new Date()), level, reqId, status, target);
	userActionLedger.add(entry);
}

private void logSystemEvent(String action, String status, String level) {
	String entry = String.format("%s | [LEVEL: %s] [SYS] [ACTION: %s] [STATUS: %s]\n",
		  simpleDateFormat.format(new Date()), level, action, status);
	CompletableFuture.runAsync(() -> {
		try {
			if (!Files.exists(getBugReportsDir())) Files.createDirectories(getBugReportsDir());
			Files.writeString(getBreadcrumbsFile(), entry, StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
		} catch (IOException ignored) {}
	});
}

private synchronized void flushBreadcrumbsToDiskSync() {
	try {
		if (!Files.exists(getBugReportsDir())) Files.createDirectories(getBugReportsDir());
		List<String> outputLines = new ArrayList<>(userActionLedger);
		if (!outputLines.isEmpty()) {
			Files.write(getBreadcrumbsFile(), outputLines, StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
		}
	} catch (IOException ignored) {}
}

@Override public void logActivity(String action) { logUserIntent(generateCorrelationId(), "GENERIC_ACTIVITY", action); }
public Iterable<String> getBreadcrumbs() { return userActionLedger; }

@Override
public void saveChanges() {
	ontoLock.writeLock().lock();
	try {
		if (isCorrupted) {
			logSystemEvent("AUTO_SAVE", "ABORTED_CORRUPTED", "WARN");
			return;
		}
		if (Files.exists(getLakeHierarchy())) Files.copy(getLakeHierarchy(), managerPath.resolve(hierarchyPathSuffix + ".bak"), StandardCopyOption.REPLACE_EXISTING);
		if (Files.exists(getLakeElements())) Files.copy(getLakeElements(), managerPath.resolve(elementsPathSuffix + ".bak"), StandardCopyOption.REPLACE_EXISTING);
		
		storageService.saveOntologyHierarchy(getLakeHierarchy(), ontologyHierarchy);
		storageService.saveOntologyElements(getLakeElements(), ontologyHierarchy.fileInterfaces);
		dirtyFlag.set(false);
		
		logSystemEvent("AUTO_SAVE", "SUCCESS", "INFO");
		flushBreadcrumbsToDiskSync();
	} catch (IOException e) {
		logSystemEvent("AUTO_SAVE", "FATAL_IO_EXCEPTION", "FATAL");
		throw new OntoDirectoryException("Failed to save Lake state: " + e.getMessage());
	} finally {
		ontoLock.writeLock().unlock();
	}
}

@Override public void importFiles() {
	importFiles(getLakeImports());
	System.out.println("Import files successful");
	String reqId = generateCorrelationId();
	logUserIntent(reqId, "CLEAR_UP_AFTER_IMPORT", getLakeImports().toString());
	try {
		if (isCorrupted) throw new OntoDirectoryException("Cannot import to a corrupted or locked Data Lake.");
		
		Files.walkFileTree(getLakeImports(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exp) throws IOException {
				if (dir.equals(getLakeImports())) return FileVisitResult.CONTINUE;
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
			
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}
		});
		
		notifyListeners();
		logUserCommit(reqId, "SUCCESS", getLakeImports().toString(), "INFO");
	} catch (Exception ex) {
		logUserCommit(reqId, "FATAL_" + ex.getClass().getSimpleName(), getLakeImports().toString(), "FATAL");
		generateDiagnosticDump(ex);
		throw new OntoDirectoryException("Clear up Failure: " + ex.getMessage());
	}
}

@Override
public void importFiles(Path sourceDirectory) {
	System.out.println("Import files requested for " + sourceDirectory.toString() + "successfully!");
	String reqId = generateCorrelationId();
	logUserIntent(reqId, "BULK_IMPORT", sourceDirectory.toString());
	ontoLock.writeLock().lock();
	dirtyFlag.set(true);
	try {
		if (isCorrupted) throw new OntoDirectoryException("Cannot import to a corrupted or locked Data Lake.");
		if (!Files.exists(sourceDirectory)) {
			if (sourceDirectory.equals(getLakeImports())) Files.createDirectories(sourceDirectory);
			return;
		}
		
		String temporalClassName = "imported_at_" + simpleDateFormat.format(new Date());
		int temporalTagId = rawHierarchyManager.createOntologyClassRaw(temporalClassName, null, null);
		
		Map<Path, Integer> dirToTagMap = new HashMap<>();
		dirToTagMap.put(sourceDirectory, temporalTagId);
		
		Files.walkFileTree(sourceDirectory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
				if (dir.equals(sourceDirectory)) return FileVisitResult.CONTINUE;
				Integer parentTagId = dirToTagMap.get(dir.getParent());
				if (parentTagId == null) parentTagId = temporalTagId; // Fallback to root temporal
				int newTagId = rawHierarchyManager.createOntologyClassRaw(dir.getFileName().toString(), List.of(parentTagId), null);
				dirToTagMap.put(dir, newTagId);
				return FileVisitResult.CONTINUE;
			}
			
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Integer parentTagId = dirToTagMap.get(file.getParent());
				if (parentTagId == null) parentTagId = temporalTagId;
				processSingleImportInternal(file, rawHierarchyManager.getClassFromIdentity(parentTagId), rawHierarchyManager);
				return FileVisitResult.CONTINUE;
			}
		});
		
		notifyListeners();
		logUserCommit(reqId, "SUCCESS", sourceDirectory.toString(), "INFO");
	} catch (Exception ex) {
		logUserCommit(reqId, "FATAL_" + ex.getClass().getSimpleName(), sourceDirectory.toString(), "FATAL");
		generateDiagnosticDump(ex);
		throw new OntoDirectoryException("Import Failure: " + ex.getMessage());
	} finally {
		ontoLock.writeLock().unlock();
	}
}

private void processSingleImportInternal(Path physicalFile, OntologyClass destinationClass, OntologyReadingService.OntologyManagingService oms) throws IOException {
	int insertIndex = ontologyHierarchy.fileInterfaces.indexOf(null);
	if (insertIndex == -1) insertIndex = ontologyHierarchy.fileInterfaces.size();
	
	FileInterface fi = new FileInterface();
	fi.identity = insertIndex;
	fi.actualName = physicalFile.getFileName().toString();
	fi.diskName = FileInterface.getDiskNameFor(insertIndex);
	fi.tagsByIdentity = new ArrayList<>();
	fi.actualFile = getLakePath().resolve(fi.diskName);
	
	try {
		Files.copy(physicalFile, fi.actualFile, StandardCopyOption.REPLACE_EXISTING);
	} catch (IOException e) {
		Files.copy(physicalFile, fi.actualFile, StandardCopyOption.REPLACE_EXISTING);
	}
	
	if (insertIndex == ontologyHierarchy.fileInterfaces.size()) {
		ontologyHierarchy.fileInterfaces.add(fi);
	} else {
		ontologyHierarchy.fileInterfaces.set(insertIndex, fi);
	}
	
	if (destinationClass != null) {
		oms.addElementRaw(destinationClass.identityNumber, fi);
	} else {
		oms.addElementRaw(0, fi);
	}
}

@Override public void exportFiles(OntologyFilter filter) { exportFiles(filter, getLakeExports()); }

@Override
public void exportFiles(OntologyFilter filter, Path destinationFolder) {
	String reqId = generateCorrelationId();
	logUserIntent(reqId, "EXPORT_DOMAIN", destinationFolder.toString());
	ontoLock.readLock().lock();
	try {
		List<FileInterface> matchedFiles = rawHierarchyManager.getOntologyElements(filter);
		exportSpecificFilesInternal(matchedFiles, destinationFolder);
		logUserCommit(reqId, "SUCCESS", destinationFolder.toString(), "INFO");
	} catch (Exception ex) {
		logUserCommit(reqId, "FATAL_" + ex.getClass().getSimpleName(), destinationFolder.toString(), "FATAL");
		generateDiagnosticDump(ex);
		throw new OntoDirectoryException("Export Failure: " + ex.getMessage());
	} finally {
		ontoLock.readLock().unlock();
	}
}

@Override
public void exportSpecificFiles(List<FileInterface> files, Path destinationFolder) {
	String reqId = generateCorrelationId();
	logUserIntent(reqId, "EXPORT_SPECIFIC", destinationFolder.toString());
	ontoLock.readLock().lock();
	try {
		exportSpecificFilesInternal(files, destinationFolder);
		logUserCommit(reqId, "SUCCESS", destinationFolder.toString(), "INFO");
	} catch (Exception ex) {
		logUserCommit(reqId, "FATAL_" + ex.getClass().getSimpleName(), destinationFolder.toString(), "FATAL");
		generateDiagnosticDump(ex);
		throw new OntoDirectoryException("Export Failure: " + ex.getMessage());
	} finally {
		ontoLock.readLock().unlock();
	}
}

private void exportSpecificFilesInternal(List<FileInterface> matchedFiles, Path destinationFolder) throws IOException {
	if (!Files.exists(destinationFolder)) Files.createDirectories(destinationFolder);
	for (FileInterface file : matchedFiles) {
		if (file != null && file.actualFile != null) {
			Path targetPath = destinationFolder.resolve(file.actualName);
			Files.copy(file.actualFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}

@Override public OntologyReadingService getOntologyReadingService() { return safeReadingService; }
@Override public OntologyReadingService.OntologyManagingService getOntologyManagingService() { return safeManagingService; }
@Override public Path getRootPath() { return managerPath; }

@Override
public void addDataLakeServiceListener(DataLakeServiceListener listener) {
	if (!listeners.contains(listener)) listeners.add(listener);
}

private void notifyListeners() {
	for (DataLakeServiceListener listener : listeners) listener.onChange();
}

private class ThreadSafeReadingService implements OntologyReadingService {
	protected final OntologyReadingService delegate;
	public ThreadSafeReadingService(OntologyReadingService delegate) { this.delegate = delegate; }
	
	@Override public OntologyClass getRootOntologyClass() { ontoLock.readLock().lock(); try { return delegate.getRootOntologyClass(); } finally { ontoLock.readLock().unlock(); } }
	@Override public OntologyClass getClassFromIdentity(int identity) { ontoLock.readLock().lock(); try { return delegate.getClassFromIdentity(identity); } finally { ontoLock.readLock().unlock(); } }
	@Override public int getDomainIdentity() { ontoLock.readLock().lock(); try { return delegate.getDomainIdentity(); } finally { ontoLock.readLock().unlock(); } }
	@Override public void setDomain(OntologyClass domain) { ontoLock.writeLock().lock(); try { delegate.setDomain(domain); } finally { ontoLock.writeLock().unlock(); } }
	@Override public ArrayList<FileInterface> getOntologyElements(int classIdentity) { ontoLock.readLock().lock(); try { return delegate.getOntologyElements(classIdentity); } finally { ontoLock.readLock().unlock(); } }
	@Override public ArrayList<FileInterface> getAllOntologyElements(int classIdentity) { ontoLock.readLock().lock(); try { return delegate.getAllOntologyElements(classIdentity); } finally { ontoLock.readLock().unlock(); } }
	@Override public boolean isElementForFilter(OntologyClass oc, FileInterface fi) { ontoLock.readLock().lock(); try { return delegate.isElementForFilter(oc, fi); } finally { ontoLock.readLock().unlock(); } }
	@Override public boolean isDescendentForFilter(OntologyClass oc, FileInterface fi) { ontoLock.readLock().lock(); try { return delegate.isDescendentForFilter(oc, fi); } finally { ontoLock.readLock().unlock(); } }
	@Override public List<FileInterface> getOntologyElements(@NotNull OntologyFilter filter) { ontoLock.readLock().lock(); try { return delegate.getOntologyElements(filter); } finally { ontoLock.readLock().unlock(); } }
	@Override public void addOntologyServiceListener(OntologyServiceListener listener) { ontoLock.writeLock().lock(); try { delegate.addOntologyServiceListener(listener); } finally { ontoLock.writeLock().unlock(); } }
}

@FunctionalInterface private interface DomainOperation<T> { T execute() throws Exception; }

private class ThreadSafeManagingService extends ThreadSafeReadingService implements OntologyReadingService.OntologyManagingService {
	private final OntologyReadingService.OntologyManagingService delegate;
	
	public ThreadSafeManagingService(OntologyReadingService.OntologyManagingService delegate) {
		super(delegate);
		this.delegate = delegate;
	}
	
	private void markDirty() { dirtyFlag.set(true); }
	
	private <T> T executeLogged(String actionName, String targetInfo, DomainOperation<T> operation) {
		String reqId = generateCorrelationId();
		logUserIntent(reqId, actionName, targetInfo);
		ontoLock.writeLock().lock();
		try {
			T result = operation.execute();
			markDirty();
			logUserCommit(reqId, "SUCCESS", targetInfo, "INFO");
			return result;
		} catch (Exception ex) {
			logUserCommit(reqId, "FATAL_" + ex.getClass().getSimpleName(), targetInfo, "FATAL");
			generateDiagnosticDump(ex);
			throw new OntoDirectoryException("Domain Operation Failed: " + actionName + " -> " + ex.getMessage());
		} finally {
			ontoLock.writeLock().unlock();
		}
	}
	
	@Override public int filterToClass(OntologyFilter f, String n) { return executeLogged("FILTER_TO_CLASS", n, () -> delegate.filterToClass(f, n)); }
	@Override public void copyContentsTo(int s, int t, boolean p, boolean c, boolean f) { executeLogged("COPY_CONTENTS", "SRC:" + s + "->TRG:" + t, () -> { delegate.copyContentsTo(s, t, p, c, f); return null; }); }
	@Override public int createOntologyClass(String n, List<Integer> p, List<Integer> c) { return executeLogged("CREATE_CLASS", n, () -> delegate.createOntologyClass(n, p, c)); }
	@Override public void removeOntologyClass(int i) { executeLogged("REMOVE_CLASS", "CLASS_ID:" + i, () -> { delegate.removeOntologyClass(i); return null; }); }
	@Override public void restoreOntologyClass(int i, OntologyClass s, ArrayList<FileInterface> f) { executeLogged("RESTORE_CLASS", "CLASS_ID:" + i, () -> { delegate.restoreOntologyClass(i, s, f); return null; }); }
	@Override public void addParent(int c, int p) { executeLogged("ADD_PARENT", "CHILD:" + c + "|PARENT:" + p, () -> { delegate.addParent(c, p); return null; }); }
	@Override public void removeParent(int c, int p) { executeLogged("REMOVE_PARENT", "CHILD:" + c + "|PARENT:" + p, () -> { delegate.removeParent(c, p); return null; }); }
	@Override public void addElement(int i, FileInterface f) { executeLogged("ADD_ELEMENT", "CLASS:" + i, () -> { if (f == null) throw new OntoDirectoryException.NullGivenAsOntologyClassException("Null element."); delegate.addElement(i, f); return null; }); }
	@Override public void removeElement(int i, FileInterface f) { executeLogged("REMOVE_ELEMENT", "CLASS:" + i, () -> { if (f == null) throw new OntoDirectoryException.NullGivenAsOntologyClassException("Null element."); delegate.removeElement(i, f); return null; }); }
	@Override public void deleteElements(@NotNull List<FileInterface> selectedFiles) { executeLogged("DELETE_ELEMENT", selectedFiles.stream().map(element -> String.valueOf(element.identity)).collect(Collectors.joining(", ", "ELEMENTS: { ", " }")), () -> {
		deleteFiles(selectedFiles); delegate.deleteElements(selectedFiles); return null; }); }
	@Override public void renameOntologyClass(int i, String n) { executeLogged("RENAME_CLASS", "CLASS:" + i + "|NAME:" + n, () -> { delegate.renameOntologyClass(i, n); return null; }); }
	@Override public void renameElementActualName(@NotNull FileInterface file, String newName) { executeLogged("RENAME_FILE", "FILE_ID:" + file.identity, () -> { delegate.renameElementActualName(file, newName); return null; }); }
	@Override public void undo() { executeLogged("UNDO", "LATEST_TRANSACTION", () -> { delegate.undo(); return null; }); }
	@Override public void redo() { executeLogged("REDO", "LATEST_TRANSACTION", () -> { delegate.redo(); return null; }); }
	
	
	@Override
	public int createOntologyClassRaw(String name, List<Integer> parentIds, List<Integer> childrenIds) {
		return 0;
	}
	
	@Override
	public void addElementRaw(int classIdentity, FileInterface file) {
	
	}
	
}

private void deleteFiles(@NotNull List<FileInterface> selectedFiles) {
	selectedFiles.forEach(fi -> {
		try {
			System.out.println(fi.actualFile.toString());
			Files.delete(fi.actualFile);
		} catch (Exception ex) {
			logUserCommit(generateCorrelationId(), "FATAL_" + ex.getClass().getSimpleName(), fi.diskName, "FATAL");
			generateDiagnosticDump(ex);
	}});
}

}