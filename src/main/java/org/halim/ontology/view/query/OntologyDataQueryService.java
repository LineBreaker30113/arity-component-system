package org.halim.ontology.view.query;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * Simple read-only access to class and element data — names, direct parent/child/element
 * relationships, and per-element classification — merging {@code core}'s separate
 * {@code OntologyClassDataService}/{@code OntologyElementDataService}/classification query surfaces
 * into the one unified "just tell me about this class or element" contract a caller usually wants.
 * Thin pass-through: no logic of its own beyond delegating to the already-complete core services.
 */
public interface OntologyDataQueryService {

    String getClassName(int classIdentity);
    IntArrayList getParents(int classIdentity);
    IntArrayList getChildren(int classIdentity);
    int getParentCount(int classIdentity);
    int getChildrenCount(int classIdentity);
    IntArrayList getElementsOf(int classIdentity);
    int getElementsCount(int classIdentity);

    String getElementName(int elementIdentity);
    IntArrayList getClassesOf(int elementIdentity);
    int getClassesCount(int elementIdentity);
    int[] getMostDescriptivePath(int elementIdentity);

    boolean isConnected(int classIdentity, int elementIdentity);

    /** The highest element id ever allocated (including tombstoned ones), or -1 if none have been. */
    int getLastElementIndex();

    /** Whether {@code elementIdentity} is currently tombstoned (deleted, awaiting id reuse) or was
     *  never allocated at all -- as opposed to a live element with zero class connections. */
    boolean isElementTombstoned(int elementIdentity);

    /** The highest class id ever allocated (including tombstoned ones), or -1 if none have been. */
    int getLastClassIndex();

    /** Whether {@code classIdentity} is currently tombstoned (deleted, awaiting id reuse) or was
     *  never allocated at all -- as opposed to a live class with zero elements. */
    boolean isClassTombstoned(int classIdentity);

}
