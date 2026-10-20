package org.halim.nStructure.ontology.view.command;

import org.halim.nStructure.ontology.core.segmentation.query.OntologyElementFilter;

/**
 * Client-facing hierarchy commands. {@link #createClass}/{@link #deleteClass}/{@link #deleteClasses}
 * and {@link #materializeFilterAsClass} are genuinely new composed operations that don't exist as a
 * single call anywhere in {@code core} — each runs as one atomic, one-undo-entry transaction (see
 * {@code core.OntologyServiceFactory#runAtomically}), coordinating the classification and hierarchy
 * segments together. {@link #addParent}/{@link #removeParent}/{@link #renameClass} are thin
 * pass-throughs to already-atomic single core operations.
 */
public interface OntologyHierarchyManagementService {

    /**
     * Creates a new class: mints an id, registers it in the hierarchy (anchored to root), names it,
     * then reparents it under each of {@code parentClassIdentities} in turn (if none given, it stays
     * anchored to root).
     */
    int createClass(String name, int... parentClassIdentities);

    /**
     * Deletes a class: reparents each of its children (to root, or to their other surviving parents,
     * whichever {@code OntologyHierarchySegmentation.removeParent}'s existing reanchoring already
     * produces), clears its own parent edges, retires its classification id, and re-anchors to root
     * any element that this deletion left with zero remaining class connections.
     *
     * @throws IllegalArgumentException if {@code classIdentity} is the root class (id 0)
     */
    void deleteClass(int classIdentity);

    /** Batches {@link #deleteClass} for several classes into one atomic, one-undo-entry operation. */
    void deleteClasses(int[] classIdentities);

    void addParent(int classIdentity, int parentIdentity);
    void removeParent(int classIdentity, int parentIdentity);
    void renameClass(int classIdentity, String newName);

    /**
     * Creates a new class (see {@link #createClass}) and bulk-tags every element matched by
     * {@code filter} into it — the ontology-engine analogue of legacy's
     * {@code OntologyHierarchySegmentation.filterToClass}: materializing a dynamic filter result as a
     * persistent, named class.
     */
    int materializeFilterAsClass(OntologyElementFilter filter, String className, int... parentClassIdentities);

    /** Ensures a class exists at {@code classIdentity} (registering both its classification and
     *  hierarchy slots if needed, root-anchored) and sets its name, for restoring saved data where
     *  the id is already fixed by what was saved. Unlike {@link #createClass}, does not mint a new
     *  id, reparent it, or participate in undo/redo. */
    void restoreClass(int classIdentity, String name);

}
