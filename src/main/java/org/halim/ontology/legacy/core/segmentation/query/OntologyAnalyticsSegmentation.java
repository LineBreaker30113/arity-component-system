package org.halim.ontology.legacy.core.segmentation.query;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.halim.ontology.legacy.core.segmentation.command.OntologyClassificationSegmentation;
import org.halim.ontology.legacy.core.segmentation.command.OntologyHierarchySegmentation;
import org.halim.ontology.legacy.core.segmentation.helper.OntologyCommandable;
import org.halim.ontology.legacy.core.segmentation.helper.OntologyQueryable;
import org.halim.ontology.legacy.core.segmentation.helper.OntologyQueryable.BypassNonQueryConcerns;

import java.util.Set;

/**
 * Capable of things that already other services can do because must contain everything about analytics.
 *
 ***/
public interface OntologyAnalyticsSegmentation extends OntologyHierarchySegmentation.OntologyHierarchyChangeListener,
      OntologyQueryable.ClassFamily, OntologyQueryable.ClassHierarchy, OntologyQueryable.BaseClassAnalytics,
      OntologyQueryable.ClassificationConnection
{

@BypassNonQueryConcerns
IntArrayList getDomainElements(int classIdentity);

@BypassNonQueryConcerns
Set<Integer> getParentsAsSet(int classIdentity);
@BypassNonQueryConcerns
Set<Integer> getChildrenAsSet(int classIdentity);
@BypassNonQueryConcerns
Set<Integer> getHeritageAsSet(int classIdentity);
@BypassNonQueryConcerns
Set<Integer> getAncestorAsSet(int classIdentity);
@BypassNonQueryConcerns
Set<Integer> getSpecificallySelfElementsAsSet(int classIdentity);
@BypassNonQueryConcerns
Set<Integer> getDomainElementsAsSet(int classIdentity);

/** This is relative to the siblings. */
@BypassNonQueryConcerns
int getUniquestChild(int classIdentity);

/** This is relative to the siblings. */
@BypassNonQueryConcerns
int[] getChildrenByUniqueness(int classIdentity);

/** This is relative to the other parents (a class may have more than one). */
@BypassNonQueryConcerns
int getUniquestParent(int classIdentity);

/** This is relative to the other parents (a class may have more than one). */
@BypassNonQueryConcerns
int[] getParentsByUniqueness(int classIdentity);

@FunctionalInterface
interface DownwardUniquenessMeasure {
    int[] childClassesByUniqueness(int classIdentity, OntologyAnalyticsSegmentation oas, OntologyClassificationSegmentation ocm);
}
@FunctionalInterface
interface UpwardUniquenessMeasure {
    int[] parentClassesByUniqueness(int classIdentity, OntologyAnalyticsSegmentation oas, OntologyClassificationSegmentation ocm);
}

@OntologyCommandable.BypassNonCommandConcerns
void setDownwardUniquenessMeasure(DownwardUniquenessMeasure measure);
@BypassNonQueryConcerns
DownwardUniquenessMeasure getDownwardUniquenessMeasure();
@OntologyCommandable.BypassNonCommandConcerns
void resetDownwardUniquenessMeasure();

@OntologyCommandable.BypassNonCommandConcerns
void setUpwardUniquenessMeasure(UpwardUniquenessMeasure measure);
@BypassNonQueryConcerns
UpwardUniquenessMeasure getUpwardUniquenessMeasure();
@OntologyCommandable.BypassNonCommandConcerns
void resetUpwardUniquenessMeasure();

/** Whether {@link #getUniquestPath}/{@link #getUniquestPathLength} walk upward through parents
 *  (using {@link #getUpwardUniquenessMeasure()}) instead of downward through children
 *  (using {@link #getDownwardUniquenessMeasure()}). Defaults to {@code false} (downward). */
@BypassNonQueryConcerns
boolean isUniquenessCalculationDirectionUpward();
@OntologyCommandable.BypassNonCommandConcerns
void setUniquenessCalculationDirectionUpward(boolean upward);

/** Walks from classIdentity using whichever measure is currently set, in whichever direction
 *  {@link #isUniquenessCalculationDirectionUpward()} currently specifies. */
@BypassNonQueryConcerns
int getUniquestPathLength(int classIdentity);
@BypassNonQueryConcerns
int[] getUniquestPath(int classIdentity);

/** Always walks upward through parents via the built-in longest-ancestry measure, regardless of
 *  {@link #isUniquenessCalculationDirectionUpward()} or whatever custom measure is currently set. */
@BypassNonQueryConcerns
int getDefaultUniquestPathLength(int classIdentity);
@BypassNonQueryConcerns
int[] getDefaultUniquestPath(int classIdentity);

@BypassNonQueryConcerns
int getDeepestPathLength(int classIdentity);
@BypassNonQueryConcerns
int[] getDeepestPath(int classIdentity);


}
