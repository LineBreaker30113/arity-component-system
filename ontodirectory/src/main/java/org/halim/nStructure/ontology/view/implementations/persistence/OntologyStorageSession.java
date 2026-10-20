package org.halim.nStructure.ontology.view.implementations.persistence;

import org.halim.nStructure.ontology.core.EngineLockHandle;
import org.halim.nStructure.ontology.view.OntologyService;
import org.halim.nStructure.ontology.view.query.OntologyDataQueryService;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;


/** This is what must be returned when "beginStoring" function is called. */
public class OntologyStorageSession {

    private final OntologyService ontologyService;
    private final EngineLockHandle lockHandle;

    public OntologyStorageSession(OntologyService ontologyService) {
        this.ontologyService = ontologyService;
        this.lockHandle = ontologyService.beginExclusiveSession();
    }

    private boolean isPendingClassCatalog = true, isPendingElementCatalog = true, isPendingBidirectionalIndexer = true, isPendingHierarchy = true;

    // Same signature org.halim.legacy.dlake.OntologyStorageV0 uses ("Version 0").
    private static final byte[] MAGIC_BYTES = { 0x08, 3, 0, 1, 1, 3, 0x7f, 0x16, 0 };
    private static final int FORMAT_VERSION = 1;

    private static final int CONTENT_ELEMENT_CATALOG = 1;
    private static final int CONTENT_CLASS_CATALOG = 2;
    private static final int CONTENT_BIDIRECTIONAL_INDEXER = 3;
    private static final int CONTENT_HIERARCHY = 4;

    /** Every stream starts with this header: the magic bytes (identifies the stream as ours), the
     *  format version, and which of the four block kinds follows -- so a reader can fail fast on a
     *  foreign/corrupted stream or a block kind it wasn't expecting, before parsing any body data. */
    private void writeHeader(DataOutputStream out, int contentType) throws IOException {
        out.write(MAGIC_BYTES);
        out.writeInt(FORMAT_VERSION);
        out.writeInt(contentType);
    }

    /**
     * Writes a catalog stream: {@code lastIndex}, followed by one chunk per index from 0 to
     * {@code lastIndex} inclusive -- {@code -1} if that index is tombstoned, otherwise the UTF-8
     * byte length of its name followed by the name bytes (empty string if no name was ever set).
     * Shared by {@link #storeElementCatalog} and {@link #storeClassCatalog}, which only differ in
     * which identity space (element vs. class) they read from.
     */
    private void writeCatalog(DataOutputStream out, int lastIndex, IntPredicate tombstoned, IntFunction<String> nameOf) throws IOException {
        out.writeInt(lastIndex);
        for (int i = 0; i <= lastIndex; i++) {
            if (tombstoned.test(i)) {
                out.writeInt(-1);
                continue;
            }
            String name = nameOf.apply(i);
            byte[] nameBytes = (name != null ? name : "").getBytes(StandardCharsets.UTF_8);
            out.writeInt(nameBytes.length);
            out.write(nameBytes);
        }
    }

    public void storeElementCatalog(DataOutputStream elementCatalog) {
        OntologyDataQueryService data = ontologyService.getOntologyDataService();
        try {
            writeHeader(elementCatalog, CONTENT_ELEMENT_CATALOG);
            writeCatalog(elementCatalog, data.getLastElementIndex(), data::isElementTombstoned, data::getElementName);
        } catch (IOException e) {
            throw new OntologyStorageException("Failed to write element catalog", e);
        }
        isPendingElementCatalog = false;
    }

    public void storeClassCatalog(DataOutputStream classCatalog) {
        OntologyDataQueryService data = ontologyService.getOntologyDataService();
        try {
            writeHeader(classCatalog, CONTENT_CLASS_CATALOG);
            writeCatalog(classCatalog, data.getLastClassIndex(), data::isClassTombstoned, data::getClassName);
        } catch (IOException e) {
            throw new OntologyStorageException("Failed to write class catalog", e);
        }
        isPendingClassCatalog = false;
    }

    /**
     * Writes the class<->element connections as a dense bit matrix: {@code lastElementIndex},
     * {@code lastClassIndex}, then one fixed-width row per element (0 to lastElementIndex
     * inclusive), each {@code ceil((lastClassIndex + 1) / 8)} bytes, bit {@code c} of the row set
     * iff the element is tagged with class {@code c}. A tombstoned element's row is all zero bits,
     * keeping every row the same width and every index positionally aligned with the catalogs.
     */
    public void storeBidirectionalIndexer(DataOutputStream bidirectionalIndexer) {
        OntologyDataQueryService data = ontologyService.getOntologyDataService();
        int lastElementIndex = data.getLastElementIndex();
        int lastClassIndex = data.getLastClassIndex();
        int rowBytes = (lastClassIndex + 8) / 8; // ceil((lastClassIndex + 1) / 8)

        try {
            writeHeader(bidirectionalIndexer, CONTENT_BIDIRECTIONAL_INDEXER);
            bidirectionalIndexer.writeInt(lastElementIndex);
            bidirectionalIndexer.writeInt(lastClassIndex);
            for (int e = 0; e <= lastElementIndex; e++) {
                byte[] row = new byte[rowBytes];
                if (!data.isElementTombstoned(e)) {
                    for (int classId : data.getClassesOf(e)) {
                        row[classId / 8] |= (byte) (1 << (classId % 8));
                    }
                }
                bidirectionalIndexer.write(row);
            }
        } catch (IOException e) {
            throw new OntologyStorageException("Failed to write bidirectional indexer", e);
        }
        isPendingBidirectionalIndexer = false;
    }

    /**
     * Writes the parent-hierarchy edges: class count, then one entry per class from 0 to
     * count-1 -- if it has exactly one parent, that parent's index directly (always >= 0); otherwise
     * the negative of its parent count, followed by that many parent indices. The sign of the first
     * int read disambiguates the two cases, since a valid index is never negative. Root's single
     * "parent" is itself (index 0), matching {@code OntologyHierarchyLister}'s self-referential
     * sentinel. Tombstoned classes still have a well-defined (root-anchored) entry here -- the class
     * catalog, stored first, is the authoritative source of which indices are actually tombstoned.
     */
    public void storeHierarchy(DataOutputStream ontology) {
        OntologyDataQueryService data = ontologyService.getOntologyDataService();
        int count = data.getLastClassIndex() + 1;
        try {
            writeHeader(ontology, CONTENT_HIERARCHY);
            ontology.writeInt(count);
            for (int i = 0; i < count; i++) {
                int parentCount = data.getParentCount(i);
                if (parentCount == 1) {
                    ontology.writeInt(data.getParents(i).getInt(0));
                } else {
                    ontology.writeInt(-parentCount);
                    for (int parentId : data.getParents(i)) {
                        ontology.writeInt(parentId);
                    }
                }
            }
        } catch (IOException e) {
            throw new OntologyStorageException("Failed to write hierarchy", e);
        }
        isPendingHierarchy = false;
    }

    public void finalizeStorage() {
        if(isPendingClassCatalog || isPendingElementCatalog || isPendingBidirectionalIndexer || isPendingHierarchy)
            throw  new IllegalStateException("Haven't stored all components");
        finalizeStorageForced();
    }

    public void finalizeStorageForced() {
        lockHandle.close();
    }

}
