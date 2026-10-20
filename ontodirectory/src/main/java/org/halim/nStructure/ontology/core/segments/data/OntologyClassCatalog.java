package org.halim.nStructure.ontology.core.segments.data;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.halim.nStructure.ontology.core.segmentation.command.AnyCatalogMutableBase;
import org.halim.nStructure.ontology.core.segmentation.command.OntologyClassificationSegmentation;
import org.halim.nStructure.ontology.core.segmentation.command.OntologyHierarchySegmentation;
import org.halim.nStructure.ontology.core.segmentation.query.AnyCatalogBase;
import org.halim.nStructure.ontology.core.segmentation.query.OntologyClassCatalogSegmentation;

import java.util.BitSet;

/**
 * Layers {@link OntologyClassCatalogSegmentation}'s query surface on top of the hierarchy and
 * classification engines (whose ClassFamily/BaseClassAnalytics methods it already implements) and
 * on top of a composed {@link AnyCatalogBase}/{@link AnyCatalogMutableBase} store for class display
 * names and any other per-class detail. That store defaults to an engine-owned
 * {@link OntologyCatalogDetailStore}, but a client can supply its own via the 3-arg constructor.
 */
public class OntologyClassCatalog implements OntologyClassCatalogSegmentation {

private final OntologyHierarchySegmentation hierarchy;
private final OntologyClassificationSegmentation classification;
private final AnyCatalogBase details;
private final AnyCatalogMutableBase detailsMutator;

public OntologyClassCatalog(OntologyHierarchySegmentation hierarchy, OntologyClassificationSegmentation classification) {
    this(hierarchy, classification, new OntologyCatalogDetailStore());
}

public <S extends AnyCatalogBase & AnyCatalogMutableBase> OntologyClassCatalog(
      OntologyHierarchySegmentation hierarchy, OntologyClassificationSegmentation classification, S details) {
    this.hierarchy = hierarchy;
    this.classification = classification;
    this.details = details;
    this.detailsMutator = details;
}

@Override
public String getName(int classIdentity) {
    return (String) details.getDetailObject(classIdentity, NAME_DETAIL);
}

@Override
public void setName(int classIdentity, String newName) {
    detailsMutator.setDetailObject(classIdentity, NAME_DETAIL, newName);
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
public boolean isParent(int classIdentity, int parentIdentity) {
    return hierarchy.isParent(classIdentity, parentIdentity);
}

@Override
public int getParentCount(int classIdentity) {
    return hierarchy.getParentCount(classIdentity);
}

@Override
public int getChildrenCount(int classIdentity) {
    return hierarchy.getChildrenCount(classIdentity);
}

@Override
public IntArrayList getParents(int classIdentity) {
    return hierarchy.getParents(classIdentity);
}

@Override
public IntArrayList getChildren(int classIdentity) {
    return hierarchy.getChildren(classIdentity);
}

@Override
public int getElementsCount(int classIdentity) {
    return classification.getElementsCount(classIdentity);
}

@Override
public IntArrayList getElementsOf(int classIdentity) {
    return classification.getElementsOf(classIdentity);
}

@Override
public int getDomainElementCount(int classIdentity) {
    BitSet seen = new BitSet();
    for (int e : classification.getElementsOf(classIdentity)) seen.set(e);
    for (int descendant : hierarchy.getHeritage(classIdentity)) {
        for (int e : classification.getElementsOf(descendant)) seen.set(e);
    }
    return seen.cardinality();
}

}
