package org.halim.nStructure.ontology.core.segmentation.command;

import org.halim.nStructure.ontology.core.segmentation.helper.OntologyTransaction;
import org.halim.nStructure.ontology.core.segmentation.helper.OntologyCommandable;
import org.halim.nStructure.ontology.core.segmentation.helper.OntologyQueryable;

/** Responsible for Managing the Hierarchy of Ontology Classes. */
public interface OntologyHierarchySegmentation extends
      OntologyCommandable.ClassFamily, OntologyCommandable.ClassHierarchy, OntologyQueryable.BaseInplaceClass,
      OntologyQueryable.ClassFamily, OntologyQueryable.ClassHierarchy {

interface OntologyHierarchyChangeListener {
    void priorClassParentAdded(int classIdentity, int parentIdentity);
    void postClassParentAdded(int classIdentity, int parentIdentity);
    void priorClassParentRemoved(int classIdentity, int parentIdentity);
    void postClassParentRemoved(int classIdentity, int parentIdentity);
}

@OntologyTransaction(action = "ADD_HIERARCHY_LISTENER")
void addOntologyHierarchyChangeListener(OntologyHierarchyChangeListener ohcl);

/**
 * Registers {@code classIdentity} as a hierarchy node, growing internal storage up to and
 * including it if needed, anchoring every newly created slot directly to root as a childless leaf.
 * If {@code classIdentity} already has a slot (a tombstoned id being reused by the classification
 * manager), this is a no-op: the invariant that a retired class's hierarchy slot is left
 * root-anchored and childless is the responsibility of whoever retires it (a composed
 * {@code deleteClass} operation), not this method. Intended to be called once, right after minting
 * an id via the classification manager's {@code newClass()}, so the two id spaces stay aligned.
 */
@OntologyTransaction(action = "REGISTER_CLASS")
void registerClass(int classIdentity);

/**
 * If true:
 * When adding parent P to class C, if P is ancestor of C, then doesn't add P,
 * if not and P is descendant of some parents of C, those classes will be removed.
 ***/
@OntologyTransaction(action = "SET_AUTO_TIGHTEN_POLICY")
void setAutoTightenParents(boolean autoTightenRequired);
/**
 * If true:
 * When adding parent P to class C, if P is ancestor of C, then doesn't add P,
 * if not and P is descendant of some parents of C, those classes will be removed.
 ***/
@OntologyQueryable.BypassNonQueryConcerns
boolean getAutoTightenParentsPolicy();



}
