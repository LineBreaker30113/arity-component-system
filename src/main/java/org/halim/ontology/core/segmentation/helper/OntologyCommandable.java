package org.halim.ontology.core.segmentation.helper;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Interfaces for commanding a cluster of data types are listed here. */
public interface OntologyCommandable {


@Retention(RetentionPolicy.RUNTIME)
// Pre-apply your base annotation here:
@BypassCrossCuttingConcerns({
	BypassCrossCuttingConcerns.CrossCuttingConcern.LOCKING_W,
	BypassCrossCuttingConcerns.CrossCuttingConcern.UNDO_REDO
})
@interface BypassNonCommandConcerns { }


/** Direct Parents or Children of classes. */
interface ClassFamily extends OntologyQueryable.ClassFamily {
	
	
	/** Important, this method meant to be safe and for every change, all the related nodes must be updated for metadata (like heritage size). */
	@OntologyTransaction(action = "ADD_PARENT")
	void addParent(int classIdentity, int parentIdentity);
	/** Important, this method meant to be safe and for every change, all the related nodes must be updated for metadata (like heritage size). */
	@OntologyTransaction(action = "REMOVE_PARENT")
	void removeParent(int classIdentity, int parentIdentity);

	/**
	 * Resets the parent hierarchy, anchoring this class directly to the root.
	 */
	@OntologyTransaction(action = "CLEAR_CHILDREN")
	default void clearChildren(int classIdentity) { for(int ci : getChildren(classIdentity)) removeChildren(classIdentity, ci); }

	@OntologyTransaction(action = "ADD_CHILDREN")
	default void addChildren(int classIdentity, int childIdentity) { addParent(childIdentity, classIdentity); }
	@OntologyTransaction(action = "REMOVE_CHILDREN")
	default void removeChildren(int classIdentity, int childIdentity) { removeParent(childIdentity, classIdentity); }
}

interface ClassHierarchy extends OntologyCommandable.ClassFamily {

	/**
	 * Resets the parent hierarchy, anchoring this class directly to the root.
	 */
	@OntologyTransaction(action = "CLEAR_PARENT")
	void clearParents(int classIdentity);

}

interface BaseInplaceClass {
	
	@FunctionalInterface
	interface OntologyClassAction { void act(int classIdentity); }
	
	@BypassNonCommandConcerns
	@OntologyTransaction(action = "COMMAND_EACH_PARENT")
	void queryEachParent(int classIdentity, OntologyQueryable.BaseInplaceClass.OntologyClassQuery cq);
	@BypassNonCommandConcerns
	@OntologyTransaction(action = "COMMAND_EACH_CHILDREN")
	void queryEachChildren(int classIdentity, OntologyQueryable.BaseInplaceClass.OntologyClassQuery cq);
	
}

interface BaseInplaceHierarchy extends BaseInplaceClass {
	
	@BypassNonCommandConcerns
	@OntologyTransaction(action = "COMMAND_EACH_ANCESTOR")
	void forEachAncestor(int classIdentity, OntologyClassAction ca);
	@BypassNonCommandConcerns
	@OntologyTransaction(action = "COMMAND_EACH_HERITAGE")
	void forEachHeritage(int classIdentity, OntologyClassAction ca);

}


interface Classification {

}



}
