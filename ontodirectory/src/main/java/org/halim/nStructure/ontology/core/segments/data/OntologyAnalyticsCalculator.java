package org.halim.nStructure.ontology.core.segments.data;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.halim.nStructure.ontology.core.segmentation.command.OntologyClassificationSegmentation;
import org.halim.nStructure.ontology.core.segmentation.command.OntologyHierarchySegmentation;
import org.halim.nStructure.ontology.core.segmentation.query.OntologyAnalyticsSegmentation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Stateless implementation of {@link OntologyAnalyticsSegmentation}: every method recomputes its answer
 * fresh from the composed hierarchy/classification services on each call, and the
 * {@link OntologyHierarchySegmentation.OntologyHierarchyChangeListener} callbacks are no-ops.
 * <p>
 * A second, caching implementation that uses those listener callbacks to invalidate memoized
 * results is planned separately; this class is intentionally the simpler of the two.
 * <p>
 * The default {@link DownwardUniquenessMeasure} ranks children by smallest domain size first
 * (fewest elements under it, self plus descendants) — the smaller a child's domain, the more unique.
 * The default {@link UpwardUniquenessMeasure} ranks parents by longest remaining ancestry first
 * (the parent that continues the longest chain toward root) — the same "longest path" idea
 * {@link #getDeepestPath} uses downward, mirrored upward through parents instead of children.
 */
public class OntologyAnalyticsCalculator implements OntologyAnalyticsSegmentation {

private static final DownwardUniquenessMeasure DEFAULT_DOWNWARD_MEASURE = (classIdentity, oas, ocm) -> {
    int[] kids = oas.getChildren(classIdentity).toIntArray();
    Integer[] boxed = new Integer[kids.length];
    for (int i = 0; i < kids.length; i++) boxed[i] = kids[i];
    Arrays.sort(boxed, Comparator.comparingInt(oas::getDomainElementCount));
    int[] result = new int[boxed.length];
    for (int i = 0; i < boxed.length; i++) result[i] = boxed[i];
    return result;
};

private static final UpwardUniquenessMeasure DEFAULT_UPWARD_MEASURE = (classIdentity, oas, ocm) -> {
    int[] parents = oas.getParents(classIdentity).toIntArray();
    Integer[] boxed = new Integer[parents.length];
    for (int i = 0; i < parents.length; i++) boxed[i] = parents[i];
    Arrays.sort(boxed, Comparator.comparingInt((Integer p) -> ancestryHeight(p, oas)).reversed());
    int[] result = new int[boxed.length];
    for (int i = 0; i < boxed.length; i++) result[i] = boxed[i];
    return result;
};

/** Length of the longest parent chain above classIdentity. Class 0 is root and its own parent
 *  sentinel (see OntologyHierarchyLister), so it's the fixed base case rather than a loop. */
private static int ancestryHeight(int classIdentity, OntologyAnalyticsSegmentation oas) {
    if (classIdentity == 0) return 0;
    int max = 0;
    for (int parent : oas.getParents(classIdentity)) {
        max = Math.max(max, 1 + ancestryHeight(parent, oas));
    }
    return max;
}

private final OntologyHierarchySegmentation hierarchy;
private final OntologyClassificationSegmentation classification;
private DownwardUniquenessMeasure currentDownwardMeasure = DEFAULT_DOWNWARD_MEASURE;
private UpwardUniquenessMeasure currentUpwardMeasure = DEFAULT_UPWARD_MEASURE;
private boolean uniquenessCalculationDirectionUpward = false;

public OntologyAnalyticsCalculator(OntologyHierarchySegmentation hierarchy, OntologyClassificationSegmentation classification) {
    this.hierarchy = hierarchy;
    this.classification = classification;
}

// --- ClassFamily / ClassHierarchy: pass-through to the hierarchy service ---

@Override public boolean isParent(int classIdentity, int parentIdentity) { return hierarchy.isParent(classIdentity, parentIdentity); }
@Override public int getParentCount(int classIdentity) { return hierarchy.getParentCount(classIdentity); }
@Override public int getChildrenCount(int classIdentity) { return hierarchy.getChildrenCount(classIdentity); }
@Override public IntArrayList getParents(int classIdentity) { return hierarchy.getParents(classIdentity); }
@Override public IntArrayList getChildren(int classIdentity) { return hierarchy.getChildren(classIdentity); }
@Override public int getHeritageCount(int classIdentity) { return hierarchy.getHeritageCount(classIdentity); }
@Override public int getAncestorCount(int classIdentity) { return hierarchy.getAncestorCount(classIdentity); }
@Override public IntArrayList getHeritage(int classIdentity) { return hierarchy.getHeritage(classIdentity); }
@Override public IntArrayList getAncestors(int classIdentity) { return hierarchy.getAncestors(classIdentity); }
@Override public boolean isAncestor(int classIdentity, int doubtedIdentity) { return hierarchy.isAncestor(classIdentity, doubtedIdentity); }
@Override public boolean isHeritage(int classIdentity, int doubtedIdentity) { return hierarchy.isHeritage(classIdentity, doubtedIdentity); }

// --- BaseClassification / BaseElementData / ClassificationConnection: pass-through to classification ---

@Override public int getElementsCount(int classIdentity) { return classification.getElementsCount(classIdentity); }
@Override public IntArrayList getElementsOf(int classIdentity) { return classification.getElementsOf(classIdentity); }
@Override public int getClassesCount(int elementIdentity) { return classification.getClassesCount(elementIdentity); }
@Override public IntArrayList getClassesOf(int elementIdentity) { return classification.getClassesOf(elementIdentity); }
@Override public BitSet getClassesOfAsBitSet(int elementIdentity) { return classification.getClassesOfAsBitSet(elementIdentity); }
@Override public ArrayList<IntArrayList> getClassesForElements() { return classification.getClassesForElements(); }
@Override public ArrayList<IntArrayList> getElementsForClasses() { return classification.getElementsForClasses(); }
@Override public boolean isConnected(int classIdentity, int elementIdentity) { return classification.isConnected(classIdentity, elementIdentity); }

// --- Domain: self plus every descendant class's elements (matches OntologyClassCatalog#getDomainElementCount) ---

@Override
public int getDomainElementCount(int classIdentity) {
    BitSet seen = new BitSet();
    for (int e : classification.getElementsOf(classIdentity)) seen.set(e);
    for (int descendant : hierarchy.getHeritage(classIdentity)) {
        for (int e : classification.getElementsOf(descendant)) seen.set(e);
    }
    return seen.cardinality();
}

@Override
public IntArrayList getDomainElements(int classIdentity) {
    BitSet seen = new BitSet();
    IntArrayList result = new IntArrayList();
    for (int e : classification.getElementsOf(classIdentity)) if (!seen.get(e)) { seen.set(e); result.add(e); }
    for (int descendant : hierarchy.getHeritage(classIdentity)) {
        for (int e : classification.getElementsOf(descendant)) if (!seen.get(e)) { seen.set(e); result.add(e); }
    }
    return result;
}

// --- Set-returning views ---

@Override public Set<Integer> getParentsAsSet(int classIdentity) { return toSet(hierarchy.getParents(classIdentity)); }
@Override public Set<Integer> getChildrenAsSet(int classIdentity) { return toSet(hierarchy.getChildren(classIdentity)); }
@Override public Set<Integer> getHeritageAsSet(int classIdentity) { return toSet(hierarchy.getHeritage(classIdentity)); }
@Override public Set<Integer> getAncestorAsSet(int classIdentity) { return toSet(hierarchy.getAncestors(classIdentity)); }
@Override public Set<Integer> getSpecificallySelfElementsAsSet(int classIdentity) { return toSet(classification.getElementsOf(classIdentity)); }
@Override public Set<Integer> getDomainElementsAsSet(int classIdentity) { return toSet(getDomainElements(classIdentity)); }

private static Set<Integer> toSet(IntArrayList values) {
    Set<Integer> set = new HashSet<>(values.size());
    for (int v : values) set.add(v);
    return set;
}

// --- Children / parent uniqueness ---

@Override
public int getUniquestChild(int classIdentity) {
    int[] ranked = getChildrenByUniqueness(classIdentity);
    if (ranked.length == 0) throw new NoSuchElementException("Class " + classIdentity + " has no children.");
    return ranked[0];
}

@Override
public int[] getChildrenByUniqueness(int classIdentity) {
    return currentDownwardMeasure.childClassesByUniqueness(classIdentity, this, classification);
}

@Override
public int getUniquestParent(int classIdentity) {
    int[] ranked = getParentsByUniqueness(classIdentity);
    if (ranked.length == 0) throw new NoSuchElementException("Class " + classIdentity + " has no parents.");
    return ranked[0];
}

@Override
public int[] getParentsByUniqueness(int classIdentity) {
    return currentUpwardMeasure.parentClassesByUniqueness(classIdentity, this, classification);
}

@Override
public void setDownwardUniquenessMeasure(DownwardUniquenessMeasure measure) { this.currentDownwardMeasure = measure; }

@Override
public DownwardUniquenessMeasure getDownwardUniquenessMeasure() { return currentDownwardMeasure; }

@Override
public void resetDownwardUniquenessMeasure() { this.currentDownwardMeasure = DEFAULT_DOWNWARD_MEASURE; }

@Override
public void setUpwardUniquenessMeasure(UpwardUniquenessMeasure measure) { this.currentUpwardMeasure = measure; }

@Override
public UpwardUniquenessMeasure getUpwardUniquenessMeasure() { return currentUpwardMeasure; }

@Override
public void resetUpwardUniquenessMeasure() { this.currentUpwardMeasure = DEFAULT_UPWARD_MEASURE; }

@Override
public boolean isUniquenessCalculationDirectionUpward() { return uniquenessCalculationDirectionUpward; }

@Override
public void setUniquenessCalculationDirectionUpward(boolean upward) { this.uniquenessCalculationDirectionUpward = upward; }

// --- Paths: uniquest (current measure + current direction), default-uniquest (always upward,
//     always the built-in longest-ancestry measure), deepest (always downward).
//     "Length" is edge count (path.length - 1), not node count. ---

@Override
public int getUniquestPathLength(int classIdentity) { return getUniquestPath(classIdentity).length - 1; }

@Override
public int[] getUniquestPath(int classIdentity) {
    return uniquenessCalculationDirectionUpward
          ? walkUpwardPath(classIdentity, currentUpwardMeasure)
          : walkDownwardPath(classIdentity, currentDownwardMeasure);
}

@Override
public int getDefaultUniquestPathLength(int classIdentity) { return getDefaultUniquestPath(classIdentity).length - 1; }

@Override
public int[] getDefaultUniquestPath(int classIdentity) { return walkUpwardPath(classIdentity, DEFAULT_UPWARD_MEASURE); }

private int[] walkDownwardPath(int classIdentity, DownwardUniquenessMeasure measure) {
    IntArrayList path = new IntArrayList();
    path.add(classIdentity);
    int current = classIdentity;
    while (hierarchy.getChildrenCount(current) > 0) {
        int[] ranked = measure.childClassesByUniqueness(current, this, classification);
        current = ranked[0];
        path.add(current);
    }
    return path.toIntArray();
}

/** Class 0 is root and its own parent sentinel (see OntologyHierarchyLister), so this stops on
 *  reaching identity 0 rather than on an empty parent list — mirrors OntologyElementCatalog's
 *  root-to-leaf walk. */
private int[] walkUpwardPath(int classIdentity, UpwardUniquenessMeasure measure) {
    IntArrayList path = new IntArrayList();
    path.add(classIdentity);
    int current = classIdentity;
    while (current != 0) {
        int[] ranked = measure.parentClassesByUniqueness(current, this, classification);
        current = ranked[0];
        path.add(current);
    }
    return path.toIntArray();
}

@Override
public int getDeepestPathLength(int classIdentity) { return getDeepestPath(classIdentity).length - 1; }

@Override
public int[] getDeepestPath(int classIdentity) {
    IntArrayList path = new IntArrayList();
    path.add(classIdentity);
    int current = classIdentity;
    while (true) {
        IntArrayList children = hierarchy.getChildren(current);
        if (children.isEmpty()) break;
        int deepestChild = children.getInt(0);
        int deepestDepth = deepestSubtreeDepth(deepestChild);
        for (int i = 1; i < children.size(); i++) {
            int candidate = children.getInt(i);
            int depth = deepestSubtreeDepth(candidate);
            if (depth > deepestDepth) { deepestChild = candidate; deepestDepth = depth; }
        }
        current = deepestChild;
        path.add(current);
    }
    return path.toIntArray();
}

private int deepestSubtreeDepth(int classIdentity) {
    int max = 0;
    for (int child : hierarchy.getChildren(classIdentity)) {
        max = Math.max(max, 1 + deepestSubtreeDepth(child));
    }
    return max;
}

// --- Hierarchy change listener: no-ops for this stateless implementation. A future caching
//     implementation would use these to invalidate memoized results for the affected class(es). ---

@Override public void priorClassParentAdded(int classIdentity, int parentIdentity) { }
@Override public void postClassParentAdded(int classIdentity, int parentIdentity) { }
@Override public void priorClassParentRemoved(int classIdentity, int parentIdentity) { }
@Override public void postClassParentRemoved(int classIdentity, int parentIdentity) { }

}
