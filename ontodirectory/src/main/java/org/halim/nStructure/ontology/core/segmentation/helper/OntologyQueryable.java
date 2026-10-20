package org.halim.nStructure.ontology.core.segmentation.helper;


import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.BitSet;

/** Interfaces for querying a cluster of data types are listed here. */
public interface OntologyQueryable {

@Retention(RetentionPolicy.RUNTIME)
// Pre-apply your base annotation here:
@BypassCrossCuttingConcerns({
	BypassCrossCuttingConcerns.CrossCuttingConcern.LOCKING_W,
	BypassCrossCuttingConcerns.CrossCuttingConcern.UNDO_REDO
})
@interface BypassNonQueryConcerns { }
@Retention(RetentionPolicy.RUNTIME)
// Pre-apply your base annotation here:
@BypassCrossCuttingConcerns({
	BypassCrossCuttingConcerns.CrossCuttingConcern.LOCKING_W,
	BypassCrossCuttingConcerns.CrossCuttingConcern.UNDO_REDO,
	BypassCrossCuttingConcerns.CrossCuttingConcern.LOGGING
})
@interface CheapQuery { }


/** Direct Parents or Children of classes. */
interface ClassFamily {
	
	@BypassNonQueryConcerns
	boolean isParent(int classIdentity, int parentIdentity);
	
	@BypassNonQueryConcerns
	default boolean isChildren(int classIdentity, int childIdentity) { return isParent(childIdentity, classIdentity); }
	
	@BypassNonQueryConcerns
	int getParentCount(int classIdentity);
	
	@BypassNonQueryConcerns
	int getChildrenCount(int classIdentity);
	
	@BypassNonQueryConcerns
	IntArrayList getParents(int classIdentity);
	
	@BypassNonQueryConcerns
	IntArrayList getChildren(int classIdentity);
	
}

interface ClassHierarchy extends ClassFamily {

	@BypassNonQueryConcerns
	int getHeritageCount(int classIdentity);
	@BypassNonQueryConcerns
	int getAncestorCount(int classIdentity);

	@BypassNonQueryConcerns
	IntArrayList getHeritage(int classIdentity);
	@BypassNonQueryConcerns
	IntArrayList getAncestors(int classIdentity);


	/** * Determines if the current class is a subclass (descendant) of the provided candidate.
	 * This operation performs a recursive Depth-First Search (DFS) upwards through the parent hierarchy.
	 * @return {@code true} if this class is identical to or descends from the candidate; {@code false} otherwise.
	 */
	@BypassNonQueryConcerns
	boolean isAncestor(int classIdentity, int doubtedIdentity);

	/** * Determines if the provided candidate is a subclass (descendant) of the current class, i.e.
	 * whether the candidate belongs to this class's heritage.
	 * This operation performs a recursive Depth-First Search (DFS) downwards through the children hierarchy.
	 * @return {@code true} if the candidate is identical to or descends from this class; {@code false} otherwise.
	 */
	@BypassNonQueryConcerns
	boolean isHeritage(int classIdentity, int doubtedIdentity);


}

interface BaseInplaceClass {
	
	@FunctionalInterface
	interface OntologyClassQuery { void query(int classIdentity); }
	
	@BypassNonQueryConcerns
	void queryEachParent(int classIdentity, OntologyClassQuery cq);
	@BypassNonQueryConcerns
	void queryEachChildren(int classIdentity, OntologyClassQuery cq);
	
}

interface BaseInplaceHierarchy extends BaseInplaceClass {
	
	@BypassNonQueryConcerns
	void forEachAncestor(int classIdentity, OntologyClassQuery classQuery);
	@BypassNonQueryConcerns
	void forEachHeritage(int classIdentity, OntologyClassQuery cq);
	
}

interface BaseClassification {
	
	/** Same as getting its elements from the indexer. */
	@BypassNonQueryConcerns
	int getElementsCount(int classIdentity);
	
	@BypassNonQueryConcerns
	IntArrayList getElementsOf(int classIdentity);
	
}

interface BaseElementData {

	@BypassNonQueryConcerns
	int getClassesCount(int elementIdentity);

	@BypassNonQueryConcerns
	IntArrayList getClassesOf(int elementIdentity);

	@BypassNonQueryConcerns
	BitSet getClassesOfAsBitSet(int elementIdentity);

}


interface ClassificationConnection extends BaseClassification, BaseElementData {
	
	// --- Not Secured Queries for usage by  Engine (Direct copy of data for speed, still read-only intended) ---
	
	@BypassNonQueryConcerns
	ArrayList<IntArrayList> getClassesForElements();
	
	@BypassNonQueryConcerns
	ArrayList<IntArrayList> getElementsForClasses();
	
	
	@BypassNonQueryConcerns
	boolean isConnected(int classIdentity, int elementIdentity);
	
	
}


interface BaseClassAnalytics extends BaseClassification {
	
	/** Count of all the elements that belong to it, or it's descendant classes (its Domain Of). */
	@BypassNonQueryConcerns
	int getDomainElementCount(int classIdentity);
	
}

	
	
}
