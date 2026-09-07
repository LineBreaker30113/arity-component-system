package org.halim.ontology.legacy.core.segments.manager;

import org.halim.ontology.legacy.core.segmentation.command.OntologyClassificationSegmentation;
import org.halim.ontology.legacy.core.segmentation.query.OntologyFilteringServiceSegmentation;
import org.halim.ontology.legacy.core.segmentation.query.OntologyClassFilter;
import org.halim.ontology.legacy.core.segmentation.query.OntologyElementFilter;
import org.halim.ontology.legacy.core.segmentation.command.OntologyHierarchySegmentation;
import org.halim.ontology.legacy.core.segmentation.command.OntologyInplaceCommandingSegmentation;

import java.util.BitSet;

/**
 * Composes the hierarchy and classification services to drive the read-only, callback-style
 * iteration {@link OntologyInplaceCommandingSegmentation} exposes. "Self" elements are those directly classified
 * to a class; "domain" elements are self plus every descendant class's elements (matching
 * {@code OntologyClassCatalog#getDomainElementCount}'s definition exactly).
 */
public class OntologyInplaceWalker implements OntologyInplaceCommandingSegmentation {

private final OntologyHierarchySegmentation hierarchy;
private final OntologyClassificationSegmentation classification;
private final OntologyFilteringServiceSegmentation filtering;

public OntologyInplaceWalker(OntologyHierarchySegmentation hierarchy, OntologyClassificationSegmentation classification,
                             OntologyFilteringServiceSegmentation filtering) {
    this.hierarchy = hierarchy;
    this.classification = classification;
    this.filtering = filtering;
}

// --- Parent/children: already implemented by the hierarchy service ---

@Override
public void queryEachParent(int classIdentity, OntologyClassQuery cq) { hierarchy.queryEachParent(classIdentity, cq); }

@Override
public void queryEachChildren(int classIdentity, OntologyClassQuery cq) { hierarchy.queryEachChildren(classIdentity, cq); }

// --- Ancestor / heritage: reuse the hierarchy's already-deduplicated list traversals ---

@Override
public void forEachAncestor(int classIdentity, OntologyClassQuery cq) {
    for (int a : hierarchy.getAncestors(classIdentity)) cq.query(a);
}

@Override
public void forEachHeritage(int classIdentity, OntologyClassQuery cq) {
    for (int h : hierarchy.getHeritage(classIdentity)) cq.query(h);
}

@Override
public void forEachAncestor(int classIdentity, OntologyClassAction ca) { forEachAncestor(classIdentity, (OntologyClassQuery) ca::act); }

@Override
public void forEachHeritage(int classIdentity, OntologyClassAction ca) { forEachHeritage(classIdentity, (OntologyClassQuery) ca::act); }

// --- Element iteration: self vs domain ---

@Override
public void forEachSpecificallySelfElement(int classIdentity, OntologyElementQuery eq) {
    for (int e : classification.getElementsOf(classIdentity)) eq.query(e);
}

@Override
public void forEachElementUnderDomain(int classIdentity, OntologyElementQuery eq) {
    BitSet seen = new BitSet();
    for (int e : classification.getElementsOf(classIdentity)) if (!seen.get(e)) { seen.set(e); eq.query(e); }
    for (int descendant : hierarchy.getHeritage(classIdentity)) {
        for (int e : classification.getElementsOf(descendant)) if (!seen.get(e)) { seen.set(e); eq.query(e); }
    }
}

@Override
public void forEachSpecificallySelfElement(int classIdentity, OntologyElementAction ea) {
    forEachSpecificallySelfElement(classIdentity, (OntologyElementQuery) ea::act);
}

@Override
public void forEachElementUnderDomain(int classIdentity, OntologyElementAction ea) {
    forEachElementUnderDomain(classIdentity, (OntologyElementQuery) ea::act);
}

// --- Class / element filters: OntologyClassFilter/OntologyElementFilter are plain data, the
//     filtering service does the actual resolution ---

@Override
public void forEachClassSatisfyingFilter(OntologyClassFilter ocf, OntologyClassQuery cq) {
    filtering.forEachClassSatisfying(ocf, cq);
}

@Override
public void forEachClassSatisfyingFilter(OntologyClassFilter ocf, OntologyClassAction ca) {
    forEachClassSatisfyingFilter(ocf, (OntologyClassQuery) ca::act);
}

@Override
public void forEachElementSatisfyingFilter(OntologyElementFilter oef, OntologyElementQuery eq) {
    filtering.forEachElementSatisfying(oef, eq);
}

@Override
public void forEachElementSatisfyingFilter(OntologyElementFilter oef, OntologyElementAction ea) {
    forEachElementSatisfyingFilter(oef, (OntologyElementQuery) ea::act);
}

}
