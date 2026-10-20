package org.halim.nStructure.archive.core;

import org.halim.nStructure.mhexagon.CircularFifoQueue;
import org.halim.nStructure.mhexagon.OntoDirectoryService;
import org.halim.nStructure.mhexagon.OntologyClassHandle;
import org.halim.nStructure.mhexagon.OntologyElementHandle;
import org.halim.nStructure.mhexagon.OntologyReadingService;
import org.halim.nStructure.ontology.core.OntologyEngine;
import org.halim.nStructure.ontology.core.segmentation.query.OntologyElementFilter;
import org.halim.nStructure.ontology.view.implementations.DefaultOntologyService;
import org.halim.nStructure.ontology.view.OntologyService;
import org.halim.nStructure.ontology.view.implementations.persistence.OntologyRestoreSession;
import org.halim.nStructure.ontology.view.implementations.persistence.OntologyStorageSession;
import org.halim.legacy.dlake.DataLakeManager;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Per-lake composition root + adapter bridging the ontology engine to both
 * {@code mhexagon.OntologyReadingService.OntologyManagingService} (ontology operations) and
 * {@code mhexagon.OntoDirectoryService.DataLakeService} (lake lifecycle: load/save/import/export) --
 * the nStructure-engine port of legacy's {@code DataLakeManager}, using
 * {@link OntologyStorageSession}/{@link OntologyRestoreSession} in place of
 * {@code OntologyStorageV0}'s binary format, and {@link FileMetadataCatalog} in place of
 * {@code FileInterface.diskName}/{@code actualFile} (the ontology engine itself has no file-path
 * concept -- see {@code OntologyElementHandle}).
 * <p>
 * Vacuum is intentionally unimplemented ({@link #executeVacuum()} throws) -- the ontology engine
 * has no compaction/renumbering support yet, so the feature is hidden in the UI rather than ported.
 */
public class ArchiveManager implements OntologyReadingService.OntologyManagingService, OntoDirectoryService.DataLakeService {

public static final String LAKE_PATH_SUFFIX = ".DATA_LAKE",
      CONFIG_FILE_SUFFIX = ".odConfig.bin",
      IMPORTS_PATH_SUFFIX = "imports",
      EXPORTS_PATH_SUFFIX = "exports",
      CLASS_CATALOG_SUFFIX = ".classCatalog.bin",
      ELEMENT_CATALOG_SUFFIX = ".elementCatalog.bin",
      BIDIRECTIONAL_INDEXER_SUFFIX = ".bidirectionalIndexer.bin",
      HIERARCHY_SUFFIX = ".hierarchy.bin",
      FILE_METADATA_SUFFIX = ".fileMetadata.bin",
      BUG_REPORTS_SUFFIX = "bugReports";

private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

public final Path managerPath;
private final OntologyService ontologyService;
private FileMetadataCatalog fileMetadata;

private final ReentrantReadWriteLock fileLock = new ReentrantReadWriteLock();
private volatile boolean dirty = false;

private final List<DataLakeServiceListener> lakeListeners = new ArrayList<>();
private final List<OntologyServiceListener> ontologyListeners = new ArrayList<>();
private final CircularFifoQueue<String> activityLedger = new CircularFifoQueue<>(1000);
private int domainIdentity = 0;

public ArchiveManager(Path managerPath) {
    this.managerPath = managerPath;

    if (looksLikeLegacyLake(managerPath)) {
        throw new LakeFormatMismatchException(
              "Directory already holds a legacy-format Data Lake (found "
              + DataLakeManager.hierarchyPathSuffix + " or " + DataLakeManager.elementsPathSuffix
              + "): " + managerPath + " -- open it with the legacy app, not nStructure.");
    }

    this.ontologyService = new DefaultOntologyService(new OntologyEngine());

    if (!Files.exists(getLakeConfig())) {
        createLake();
    } else {
        loadLake();
    }
}

private static boolean looksLikeLegacyLake(Path managerPath) {
    return Files.exists(managerPath.resolve(DataLakeManager.hierarchyPathSuffix))
          || Files.exists(managerPath.resolve(DataLakeManager.elementsPathSuffix));
}

// --- Path helpers ---

public Path getLakePath() { return managerPath.resolve(LAKE_PATH_SUFFIX); }
public Path getLakeConfig() { return managerPath.resolve(CONFIG_FILE_SUFFIX); }
public Path getLakeImports() { return managerPath.resolve(IMPORTS_PATH_SUFFIX); }
public Path getLakeExports() { return managerPath.resolve(EXPORTS_PATH_SUFFIX); }
public Path getClassCatalogPath() { return managerPath.resolve(CLASS_CATALOG_SUFFIX); }
public Path getElementCatalogPath() { return managerPath.resolve(ELEMENT_CATALOG_SUFFIX); }
public Path getBidirectionalIndexerPath() { return managerPath.resolve(BIDIRECTIONAL_INDEXER_SUFFIX); }
public Path getHierarchyPath() { return managerPath.resolve(HIERARCHY_SUFFIX); }
public Path getFileMetadataPath() { return managerPath.resolve(FILE_METADATA_SUFFIX); }
public Path getBugReportsDir() { return managerPath.resolve(BUG_REPORTS_SUFFIX); }

public boolean isDirty() { return dirty; }
public Iterable<String> getActivityLedger() { return activityLedger; }

// --- Lifecycle ---

private void createLake() {
    try {
        Files.createDirectories(getLakePath());
        Files.createFile(getLakeConfig());
        Files.createDirectories(getLakeImports());
        Files.createDirectories(getLakeExports());
        Files.createDirectories(getBugReportsDir());
    } catch (IOException e) {
        throw new UncheckedIOException("Failed to build directory structure: " + e.getMessage(), e);
    }
    this.fileMetadata = new FileMetadataCatalog();
    dirty = true;
    saveChanges();
    logActivity("LAKE_CREATED");
}

private void loadLake() {
    try (
          DataInputStream elementCatalog = openIn(getElementCatalogPath());
          DataInputStream classCatalog = openIn(getClassCatalogPath());
          DataInputStream bidirectional = openIn(getBidirectionalIndexerPath());
          DataInputStream hierarchy = openIn(getHierarchyPath());
          DataInputStream metadata = openIn(getFileMetadataPath())
    ) {
        OntologyRestoreSession session = ontologyService.beginLoading();
        session.loadElementCatalog(elementCatalog);
        session.loadClassCatalog(classCatalog);
        session.loadBidirectionalIndexer(bidirectional);
        session.loadHierarchy(hierarchy);
        session.finalizeLoading();
        this.fileMetadata = FileMetadataCatalog.loadFrom(metadata);
        logActivity("LAKE_HYDRATED");
    } catch (IOException e) {
        throw new UncheckedIOException("Failed to hydrate Data Lake from disk: " + e.getMessage(), e);
    }
}

private static DataInputStream openIn(Path path) throws IOException {
    return new DataInputStream(new BufferedInputStream(Files.newInputStream(path)));
}

@Override
public void saveChanges() {
    fileLock.writeLock().lock();
    try (
          DataOutputStream elementCatalog = openOut(getElementCatalogPath());
          DataOutputStream classCatalog = openOut(getClassCatalogPath());
          DataOutputStream bidirectional = openOut(getBidirectionalIndexerPath());
          DataOutputStream hierarchy = openOut(getHierarchyPath());
          DataOutputStream metadata = openOut(getFileMetadataPath())
    ) {
        OntologyStorageSession session = ontologyService.beginStoring();
        session.storeElementCatalog(elementCatalog);
        session.storeClassCatalog(classCatalog);
        session.storeBidirectionalIndexer(bidirectional);
        session.storeHierarchy(hierarchy);
        session.finalizeStorage();
        fileMetadata.saveTo(metadata);
        dirty = false;
        logActivity("SAVE_CHANGES");
    } catch (IOException e) {
        throw new UncheckedIOException("Failed to save Lake state: " + e.getMessage(), e);
    } finally {
        fileLock.writeLock().unlock();
    }
}

private static DataOutputStream openOut(Path path) throws IOException {
    return new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)));
}

@Override
public void executeVacuum() {
    throw new UnsupportedOperationException(
          "Vacuum is not implemented yet -- the ontology engine has no compaction support (see core.segments, which only ever grow)");
}

@Override
public Path generateDiagnosticDump(Throwable ex) {
    fileLock.readLock().lock();
    try {
        Path dumpFile = getBugReportsDir().resolve("crash_dump_" + System.currentTimeMillis() + ".txt");
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(dumpFile))) {
            writer.println("ONTO DIRECTORY (nStructure) CRASH DUMP");
            writer.println("Timestamp: " + new Date());
            writer.println("Lake: " + managerPath);
            writer.println("\n--- ACTIVITY LEDGER ---");
            for (String entry : activityLedger) writer.println(entry);
            writer.println("\n--- EXCEPTION ---");
            if (ex != null) ex.printStackTrace(writer);
        }
        return dumpFile;
    } catch (IOException e) {
        throw new UncheckedIOException(e);
    } finally {
        fileLock.readLock().unlock();
    }
}

@Override
public void logActivity(String action) {
    activityLedger.add(TIMESTAMP_FORMAT.format(new Date()) + " | " + action);
}

// --- Import ---

@Override
public void importFiles() {
    try {
        importFiles(getLakeImports());
        Files.walkFileTree(getLakeImports(), new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult postVisitDirectory(Path dir, IOException exp) throws IOException {
                if (!dir.equals(getLakeImports())) Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
        });
        notifyLakeListeners();
        logActivity("CLEAR_UP_AFTER_IMPORT");
    } catch (IOException e) {
        throw new UncheckedIOException("Import clean-up failed: " + e.getMessage(), e);
    }
}

@Override
public void importFiles(Path sourceDirectory) throws IOException {
    fileLock.writeLock().lock();
    dirty = true;
    try {
        if (!Files.exists(sourceDirectory)) {
            if (sourceDirectory.equals(getLakeImports())) Files.createDirectories(sourceDirectory);
            return;
        }

        String temporalClassName = "imported_at_" + TIMESTAMP_FORMAT.format(new Date());
        int temporalTagId = ontologyService.getHierarchyManagementService().createClass(temporalClassName);

        Map<Path, Integer> dirToTagMap = new HashMap<>();
        dirToTagMap.put(sourceDirectory, temporalTagId);

        Files.walkFileTree(sourceDirectory, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (dir.equals(sourceDirectory)) return FileVisitResult.CONTINUE;
                int parentTagId = dirToTagMap.getOrDefault(dir.getParent(), temporalTagId);
                int newTagId = ontologyService.getHierarchyManagementService().createClass(dir.getFileName().toString(), parentTagId);
                dirToTagMap.put(dir, newTagId);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                int parentTagId = dirToTagMap.getOrDefault(file.getParent(), temporalTagId);
                processSingleImport(file, parentTagId);
                return FileVisitResult.CONTINUE;
            }
        });

        notifyLakeListeners();
        logActivity("BULK_IMPORT:" + sourceDirectory);
    } finally {
        fileLock.writeLock().unlock();
    }
}

private void processSingleImport(Path physicalFile, int destinationClassId) throws IOException {
    int elementId = ontologyService.getClassificationManagementService().createElement(physicalFile.getFileName().toString(), destinationClassId);
    String diskName = FileMetadataCatalog.generateDiskName(elementId);
    Path actualFile = getLakePath().resolve(diskName);
    Files.copy(physicalFile, actualFile, StandardCopyOption.REPLACE_EXISTING);
    fileMetadata.register(elementId, diskName);
}

// --- Export ---

@Override
public void exportFiles(OntologyElementFilter filter) { exportFiles(filter, getLakeExports()); }

@Override
public void exportFiles(OntologyElementFilter filter, Path destinationFolder) {
    fileLock.readLock().lock();
    try {
        List<OntologyElementHandle> matches = getOntologyElements(filter);
        exportInternal(matches, destinationFolder);
        logActivity("EXPORT_DOMAIN:" + destinationFolder);
    } catch (IOException e) {
        throw new UncheckedIOException("Export failed: " + e.getMessage(), e);
    } finally {
        fileLock.readLock().unlock();
    }
}

@Override
public void exportSpecificFiles(List<OntologyElementHandle> elements, Path destinationFolder) {
    fileLock.readLock().lock();
    try {
        exportInternal(elements, destinationFolder);
        logActivity("EXPORT_SPECIFIC:" + destinationFolder);
    } catch (IOException e) {
        throw new UncheckedIOException("Export failed: " + e.getMessage(), e);
    } finally {
        fileLock.readLock().unlock();
    }
}

private void exportInternal(List<OntologyElementHandle> elements, Path destinationFolder) throws IOException {
    if (!Files.exists(destinationFolder)) Files.createDirectories(destinationFolder);
    for (OntologyElementHandle element : elements) {
        Path actualFile = fileMetadata.getActualFile(element.getIdentity(), getLakePath());
        if (actualFile != null) {
            Files.copy(actualFile, destinationFolder.resolve(element.getActualName()), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

// --- OntologyReadingService ---

@Override
public OntologyClassHandle getRootOntologyClass() { return getClassFromIdentity(0); }

@Override
public OntologyClassHandle getClassFromIdentity(int identity) { return new OntologyClassHandle(identity, ontologyService); }

@Override
public int getDomainIdentity() { return domainIdentity; }

@Override
public void setDomain(OntologyClassHandle domain) {
    this.domainIdentity = domain.getIdentity();
    for (OntologyServiceListener l : ontologyListeners) l.setOntologyDomain(domain);
}

@Override
public ArrayList<OntologyElementHandle> getOntologyElements(int classIdentity) {
    ArrayList<OntologyElementHandle> result = new ArrayList<>();
    for (int e : ontologyService.getOntologyDataService().getElementsOf(classIdentity)) {
        result.add(new OntologyElementHandle(e, ontologyService));
    }
    return result;
}

@Override
public ArrayList<OntologyElementHandle> getAllOntologyElements(int classIdentity) {
    ArrayList<OntologyElementHandle> result = new ArrayList<>();
    for (int e : ontologyService.getAnalyticsService().getDomainElements(classIdentity)) {
        result.add(new OntologyElementHandle(e, ontologyService));
    }
    return result;
}

@Override
public boolean isElementForFilter(OntologyClassHandle ontologyClass, OntologyElementHandle element) {
    return ontologyService.getOntologyDataService().isConnected(ontologyClass.getIdentity(), element.getIdentity());
}

@Override
public boolean isDescendentForFilter(OntologyClassHandle ontologyClass, OntologyElementHandle element) {
    return ontologyService.getFilteringService()
          .getElementsAsSet(new OntologyElementFilter.UnderDomain(ontologyClass.getIdentity()))
          .contains(element.getIdentity());
}

@Override
public List<OntologyElementHandle> getOntologyElements(OntologyElementFilter filter) {
    List<OntologyElementHandle> result = new ArrayList<>();
    for (int e : ontologyService.getFilteringService().getAllElements(filter)) {
        result.add(new OntologyElementHandle(e, ontologyService));
    }
    return result;
}

@Override
public void addOntologyServiceListener(OntologyServiceListener listener) { ontologyListeners.add(listener); }

// --- OntologyManagingService ---

@Override
public int filterToClass(OntologyElementFilter filter, String newClassName) {
    return ontologyService.getHierarchyManagementService().materializeFilterAsClass(filter, newClassName);
}

/** Best-effort port of legacy's bulk copy -- individual addParent failures (already-a-parent,
 *  would-cause-a-cycle) are skipped rather than aborting the whole copy. */
@Override
public void copyContentsTo(int sourceIdentity, int targetIdentity, boolean copyParents, boolean copyChildren, boolean copyFiles) {
    if (copyParents) {
        for (int p : ontologyService.getOntologyDataService().getParents(sourceIdentity)) {
            try { ontologyService.getHierarchyManagementService().addParent(targetIdentity, p); } catch (RuntimeException ignored) { }
        }
    }
    if (copyChildren) {
        for (int c : ontologyService.getOntologyDataService().getChildren(sourceIdentity)) {
            try { ontologyService.getHierarchyManagementService().addParent(c, targetIdentity); } catch (RuntimeException ignored) { }
        }
    }
    if (copyFiles) {
        for (int e : ontologyService.getOntologyDataService().getElementsOf(sourceIdentity)) {
            ontologyService.getClassificationManagementService().tagElement(targetIdentity, e);
        }
    }
}

@Override
public int createOntologyClass(String name, List<Integer> parentIds, List<Integer> childrenIds) {
    int[] parents = parentIds == null ? new int[0] : parentIds.stream().mapToInt(Integer::intValue).toArray();
    int id = ontologyService.getHierarchyManagementService().createClass(name, parents);
    if (childrenIds != null) for (int c : childrenIds) ontologyService.getHierarchyManagementService().addParent(c, id);
    return id;
}

@Override
public void removeOntologyClass(int classIdentity) { ontologyService.getHierarchyManagementService().deleteClass(classIdentity); }

@Override
public void addParent(int childId, int parentId) { ontologyService.getHierarchyManagementService().addParent(childId, parentId); }

@Override
public void removeParent(int childId, int parentId) { ontologyService.getHierarchyManagementService().removeParent(childId, parentId); }

@Override
public void addElement(int classIdentity, int elementIdentity) { ontologyService.getClassificationManagementService().tagElement(classIdentity, elementIdentity); }

@Override
public void removeElement(int classIdentity, int elementIdentity) { ontologyService.getClassificationManagementService().untagElement(classIdentity, elementIdentity); }

@Override
public void renameOntologyClass(int classIdentity, String newName) { ontologyService.getHierarchyManagementService().renameClass(classIdentity, newName); }

@Override
public void renameElementActualName(int elementIdentity, String newName) { ontologyService.getClassificationManagementService().renameElement(elementIdentity, newName); }

@Override
public boolean canUndo() { return ontologyService.canUndo(); }

@Override
public boolean canRedo() { return ontologyService.canRedo(); }

@Override
public void undo() { ontologyService.undo(); }

@Override
public void redo() { ontologyService.redo(); }

@Override
public void deleteElements(List<OntologyElementHandle> selectedElements) {
    fileLock.writeLock().lock();
    try {
        for (OntologyElementHandle element : selectedElements) {
            Path actualFile = fileMetadata.getActualFile(element.getIdentity(), getLakePath());
            if (actualFile != null) {
                try { Files.delete(actualFile); } catch (IOException e) { logActivity("DELETE_FILE_FAILED:" + element.getIdentity()); }
            }
            fileMetadata.forget(element.getIdentity());
        }
    } finally {
        fileLock.writeLock().unlock();
    }
    int[] ids = selectedElements.stream().mapToInt(OntologyElementHandle::getIdentity).toArray();
    ontologyService.getClassificationManagementService().deleteElements(ids);
    dirty = true;
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
    if (!lakeListeners.contains(listener)) lakeListeners.add(listener);
}

private void notifyLakeListeners() {
    for (DataLakeServiceListener listener : lakeListeners) listener.onChange();
}

}
