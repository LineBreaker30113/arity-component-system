package org.halim.ontology.legacy.core.segments.data;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.halim.ontology.legacy.core.segmentation.command.AnyCatalogMutableBase;
import org.halim.ontology.legacy.core.segmentation.command.OntologyClassificationSegmentation;
import org.halim.ontology.legacy.core.segmentation.command.OntologyHierarchySegmentation;
import org.halim.ontology.legacy.core.segmentation.query.AnyCatalogBase;
import org.halim.ontology.legacy.core.segmentation.query.OntologyElementCatalogSegmentation;

import java.util.BitSet;

/**
 * Layers {@link OntologyElementCatalogSegmentation}'s query surface on top of the classification
 * engine (whose BaseElementData methods it already implements) and on top of a composed
 * {@link AnyCatalogBase}/{@link AnyCatalogMutableBase} store for element display names and any other
 * per-element detail. That store defaults to an engine-owned {@link OntologyCatalogDetailStore}, but
 * a client can supply its own via the 3-arg constructor. Also computes the most descriptive
 * classification path for an element.
 */
public class OntologyElementCatalog implements OntologyElementCatalogSegmentation {

private final OntologyHierarchySegmentation hierarchy;
private final OntologyClassificationSegmentation classification;
private final AnyCatalogBase details;
private final AnyCatalogMutableBase detailsMutator;

public OntologyElementCatalog(OntologyHierarchySegmentation hierarchy, OntologyClassificationSegmentation classification) {
    this(hierarchy, classification, new OntologyCatalogDetailStore());
}

public <S extends AnyCatalogBase & AnyCatalogMutableBase> OntologyElementCatalog(
      OntologyHierarchySegmentation hierarchy, OntologyClassificationSegmentation classification, S details) {
    this.hierarchy = hierarchy;
    this.classification = classification;
    this.details = details;
    this.detailsMutator = details;
}

@Override
public String getName(int elementIdentity) {
    return (String) details.getDetailObject(elementIdentity, NAME_DETAIL);
}

@Override
public void setName(int elementIdentity, String newName) {
    detailsMutator.setDetailObject(elementIdentity, NAME_DETAIL, newName);
}

@Override
public Object getDetailObject(int entryIdentity, int detailTypeIdentity) { return details.getDetailObject(entryIdentity, detailTypeIdentity); }

@Override
public long getDetailLong(int entryIdentity, int detailTypeIdentity) { return details.getDetailLong(entryIdentity, detailTypeIdentity); }

@Override
public double getDetailDouble(int entryIdentity, int detailTypeIdentity) { return details.getDetailDouble(entryIdentity, detailTypeIdentity); }

@Override
public boolean getDetailBoolean(int entryIdentity, int detailTypeIdentity) { return details.getDetailBoolean(entryIdentity, detailTypeIdentity); }

@Override
public int getClassesCount(int elementIdentity) {
    return classification.getClassesCount(elementIdentity);
}

@Override
public IntArrayList getClassesOf(int elementIdentity) {
    return classification.getClassesOf(elementIdentity);
}

@Override
public BitSet getClassesOfAsBitSet(int elementIdentity) {
    return classification.getClassesOfAsBitSet(elementIdentity);
}

/**
 * Among the element's direct classes, walks the one with the deepest ancestor chain (the most
 * specific tag) up to root via each class's first parent, and returns that chain root-to-leaf.
 */
@Override
public int[] getMostDescriptivePath(int elementIdentity) {
    IntArrayList classesOfElement = classification.getClassesOf(elementIdentity);
    if (classesOfElement.isEmpty()) return new int[0];

    int deepest = classesOfElement.getInt(0);
    int deepestDepth = hierarchy.getAncestorCount(deepest);
    for (int i = 1; i < classesOfElement.size(); i++) {
        int candidate = classesOfElement.getInt(i);
        int depth = hierarchy.getAncestorCount(candidate);
        if (depth > deepestDepth) {
            deepest = candidate;
            deepestDepth = depth;
        }
    }
    return rootToLeafPath(deepest);
}

private int[] rootToLeafPath(int classIdentity) {
    IntArrayList leafToRoot = new IntArrayList();
    int current = classIdentity;
    leafToRoot.add(current);
    while (current != 0) {
        IntArrayList parents = hierarchy.getParents(current);
        if (parents.isEmpty()) {
            throw new IllegalStateException(
                  "Class " + current + " has no parents but is not the root class (0); the hierarchy is disconnected.");
        }
        current = parents.getInt(0);
        leafToRoot.add(current);
    }
    int[] path = new int[leafToRoot.size()];
    for (int i = 0; i < path.length; i++) path[i] = leafToRoot.getInt(path.length - 1 - i);
    return path;
}

}
