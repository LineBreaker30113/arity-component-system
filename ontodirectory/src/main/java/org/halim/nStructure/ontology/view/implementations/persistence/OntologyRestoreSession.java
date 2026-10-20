package org.halim.nStructure.ontology.view.implementations.persistence;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.halim.nStructure.ontology.core.EngineLockHandle;
import org.halim.nStructure.ontology.view.OntologyService;
import org.halim.nStructure.ontology.view.command.OntologyClassificationManagementService;
import org.halim.nStructure.ontology.view.command.OntologyHierarchyManagementService;
import org.halim.nStructure.ontology.view.query.OntologyDataQueryService;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


/** This is what must be returned when "beginLoading" function is called -- the restore-side
 *  counterpart of {@link OntologyStorageSession}, reading the exact format it writes. */
public class OntologyRestoreSession {

    private final OntologyService ontologyService;
    private final EngineLockHandle lockHandle;

    public OntologyRestoreSession(OntologyService ontologyService) {
        this.ontologyService = ontologyService;
        this.lockHandle = ontologyService.beginExclusiveSession();
    }

    private boolean isPendingClassCatalog = true, isPendingElementCatalog = true, isPendingBidirectionalIndexer = true, isPendingHierarchy = true;

    // Must match OntologyStorageSession exactly -- these streams are read by the format it writes.
    private static final byte[] MAGIC_BYTES = { 0x08, 3, 0, 1, 1, 3, 0x7f, 0x16, 0 };
    private static final int FORMAT_VERSION = 1;

    private static final int CONTENT_ELEMENT_CATALOG = 1;
    private static final int CONTENT_CLASS_CATALOG = 2;
    private static final int CONTENT_BIDIRECTIONAL_INDEXER = 3;
    private static final int CONTENT_HIERARCHY = 4;

    /** Reads and validates the header {@code OntologyStorageSession#writeHeader} wrote: magic bytes,
     *  format version, and the expected block kind. Throws if any don't match, so a foreign stream
     *  or the wrong block kind is caught before any body data is parsed. */
    private void readHeader(DataInputStream in, int expectedContentType) throws IOException {
        byte[] magic = new byte[MAGIC_BYTES.length];
        in.readFully(magic);
        if (!Arrays.equals(magic, MAGIC_BYTES)) {
            throw new OntologyStorageException("Magic byte mismatch -- not an ontology storage stream");
        }
        int version = in.readInt();
        if (version != FORMAT_VERSION) throw new OntologyStorageException("Unsupported storage format version: " + version);
        int contentType = in.readInt();
        if (contentType != expectedContentType) {
            throw new OntologyStorageException("Unexpected block kind: expected " + expectedContentType + " but stream contains " + contentType);
        }
    }

    public void loadElementCatalog(DataInputStream elementCatalog) {
        OntologyClassificationManagementService classificationCmd = ontologyService.getClassificationManagementService();
        try {
            readHeader(elementCatalog, CONTENT_ELEMENT_CATALOG);
            int lastIndex = elementCatalog.readInt();
            for (int i = 0; i <= lastIndex; i++) {
                int len = elementCatalog.readInt();
                if (len == -1) continue; // tombstone: leave unregistered
                byte[] nameBytes = new byte[len];
                elementCatalog.readFully(nameBytes);
                classificationCmd.restoreElement(i);
                classificationCmd.renameElement(i, new String(nameBytes, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new OntologyStorageException("Failed to read element catalog", e);
        }
        isPendingElementCatalog = false;
    }

    public void loadClassCatalog(DataInputStream classCatalog) {
        OntologyHierarchyManagementService hierarchyCmd = ontologyService.getHierarchyManagementService();
        try {
            readHeader(classCatalog, CONTENT_CLASS_CATALOG);
            int lastIndex = classCatalog.readInt();
            for (int i = 0; i <= lastIndex; i++) {
                int len = classCatalog.readInt();
                if (len == -1) continue; // tombstone
                byte[] nameBytes = new byte[len];
                classCatalog.readFully(nameBytes);
                hierarchyCmd.restoreClass(i, new String(nameBytes, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new OntologyStorageException("Failed to read class catalog", e);
        }
        isPendingClassCatalog = false;
    }

    /** Requires {@link #loadElementCatalog} and {@link #loadClassCatalog} to have already run --
     *  the tagged elements/classes must exist before a connection can reference them. */
    public void loadBidirectionalIndexer(DataInputStream bidirectionalIndexer) {
        OntologyClassificationManagementService classificationCmd = ontologyService.getClassificationManagementService();
        try {
            readHeader(bidirectionalIndexer, CONTENT_BIDIRECTIONAL_INDEXER);
            int lastElementIndex = bidirectionalIndexer.readInt();
            int lastClassIndex = bidirectionalIndexer.readInt();
            int rowBytes = (lastClassIndex + 8) / 8;
            for (int e = 0; e <= lastElementIndex; e++) {
                byte[] row = new byte[rowBytes];
                bidirectionalIndexer.readFully(row);
                IntArrayList classIds = new IntArrayList();
                for (int c = 0; c <= lastClassIndex; c++) {
                    if ((row[c / 8] & (1 << (c % 8))) != 0) classIds.add(c);
                }
                if (!classIds.isEmpty()) classificationCmd.tagElementToClasses(classIds.toIntArray(), e);
            }
        } catch (IOException e) {
            throw new OntologyStorageException("Failed to read bidirectional indexer", e);
        }
        isPendingBidirectionalIndexer = false;
    }

    /** Requires {@link #loadClassCatalog} to have already run. Root (index 0) is skipped -- its
     *  hierarchy state is fixed at construction, nothing to restore. */
    public void loadHierarchy(DataInputStream ontology) {
        OntologyHierarchyManagementService hierarchyCmd = ontologyService.getHierarchyManagementService();
        OntologyDataQueryService data = ontologyService.getOntologyDataService();
        try {
            readHeader(ontology, CONTENT_HIERARCHY);
            int count = ontology.readInt();
            for (int i = 0; i < count; i++) {
                int first = ontology.readInt();
                int[] parentIds;
                if (first >= 0) {
                    parentIds = new int[]{first};
                } else {
                    int parentCount = -first;
                    parentIds = new int[parentCount];
                    for (int p = 0; p < parentCount; p++) parentIds[p] = ontology.readInt();
                }
                if (i == 0) continue;
                for (int parentId : parentIds) {
                    if (!data.getParents(i).contains(parentId)) hierarchyCmd.addParent(i, parentId);
                }
            }
        } catch (IOException e) {
            throw new OntologyStorageException("Failed to read hierarchy", e);
        }
        isPendingHierarchy = false;
    }

    public void finalizeLoading() {
        if (isPendingClassCatalog || isPendingElementCatalog || isPendingBidirectionalIndexer || isPendingHierarchy)
            throw new IllegalStateException("Haven't loaded all components");
        finalizeLoadingForced();
    }

    public void finalizeLoadingForced() {
        lockHandle.close();
    }

}
