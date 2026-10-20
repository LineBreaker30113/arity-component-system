package org.halim.nStructure.ontology.view.command;

/**
 * Client-facing element and class-connection commands. Normalizes an invariant legacy applied
 * inconsistently: an element never ends up with zero class connections — {@link #createElement}
 * with no classes given, {@link #untagElement}/{@link #untagElements} that empty an element's
 * connections, and {@code OntologyHierarchyManagementService#deleteClass}'s cleanup all reanchor to
 * root the same way. Batch operations use {@code core}'s bulk overloads internally, so one
 * user-facing batch action produces exactly one undo-stack entry.
 */
public interface OntologyClassificationManagementService {

    /** Creates a new element and tags it to each of {@code classIdentities}; if none given, tags it
     *  to root instead, so it never starts out with zero class connections. */
    int createElement(String name, int... classIdentities);

    void deleteElement(int elementIdentity);
    /** Batches {@link #deleteElement} for several elements into one atomic, one-undo-entry operation. */
    void deleteElements(int[] elementIdentities);

    void tagElement(int classIdentity, int elementIdentity);
    void tagElements(int classIdentity, int[] elementIdentities);
    void tagElementToClasses(int[] classIdentities, int elementIdentity);

    /** Breaks the connection; if that leaves the element with zero class connections, reanchors it
     *  to root. */
    void untagElement(int classIdentity, int elementIdentity);
    /** Batches {@link #untagElement} for several elements into one atomic, one-undo-entry operation
     *  (each element individually reanchored to root if it's newly connectionless). */
    void untagElements(int classIdentity, int[] elementIdentities);
    void untagElementFromClasses(int[] classIdentities, int elementIdentity);

    void renameElement(int elementIdentity, String newName);

    /** Ensures an element exists at {@code elementIdentity} (growing storage as needed), for
     *  restoring saved data where the id is already fixed by what was saved. No-op if it already
     *  exists. Unlike {@link #createElement}, does not mint a new id, tag it to anything, or
     *  participate in undo/redo. */
    void restoreElement(int elementIdentity);

}
