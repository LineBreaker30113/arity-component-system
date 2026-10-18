package org.halim.ontology.incubation;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.List;


/**
 * For now, not multi thread safe.
 * Doesn't know how the elements are represented.
 * <p>
 * Two families of operation live here. {@link #get} and {@link #iterator} are the <em>list</em>
 * view - positions, in ascending order - which is what a navigation UI pages through. The rest are
 * the <em>set</em> algebra, which is what closure over a formal context is built from: computing a
 * concept is nothing but repeated intersection, so an int set without these cannot participate.
 * <p>
 * Everything below {@link #contains} is a default written once against {@code contains} and
 * {@code get}, so a new representation only has to supply those two to gain the whole algebra.
 * A representation that can do better than the generic path - a bitmap intersecting word by word,
 * a complement form reasoning over its holes - should override.
 * */
public interface ACS_IntSet {

int get(int i);
boolean add(int i);
boolean remove(int i);

int size();

/** Membership. Expected to be logarithmic or better; the whole algebra below is built on it. */
boolean contains(int i);

IntListerIterator iterator();


interface IntListerIterator {
	boolean hasNext();
	boolean hasPrevious();
	int next();
	int previous();
	void remove();
}


default boolean isEmpty() { return size() == 0; }

/**
 * How many elements the two sets share, without materialising them.
 * <p>
 * Walks the smaller side and probes the larger, which is the only ordering that stays cheap when
 * the two differ in size - and in a classification index they nearly always do, since a query
 * result is small and the class it is tested against is large.
 * */
default int intersectionCount(ACS_IntSet other) {
	ACS_IntSet small = size() <= other.size() ? this : other;
	ACS_IntSet large = small == this ? other : this;
	int shared = 0;
	for(int i = 0, n = small.size(); i < n; i++) { if(large.contains(small.get(i))) { shared++; } }
	return shared;
}

/** The shared elements, ascending. Both sides enumerate ascending, so the result already is. */
default IntArrayList intersect(ACS_IntSet other) {
	ACS_IntSet small = size() <= other.size() ? this : other;
	ACS_IntSet large = small == this ? other : this;
	IntArrayList shared = new IntArrayList();
	for(int i = 0, n = small.size(); i < n; i++) {
		int value = small.get(i);
		if(large.contains(value)) { shared.add(value); }
	}
	return shared;
}

/** What this set holds and {@code other} does not, ascending. */
default IntArrayList difference(ACS_IntSet other) {
	IntArrayList only = new IntArrayList();
	for(int i = 0, n = size(); i < n; i++) {
		int value = get(i);
		if(!other.contains(value)) { only.add(value); }
	}
	return only;
}

/** Drops everything {@code other} does not hold. True when this set changed. */
default boolean retainAll(ACS_IntSet other) {
	// Collect first: removing during the walk would shift the positions underneath it.
	IntArrayList doomed = difference(other);
	for(int i = 0; i < doomed.size(); i++) { remove(doomed.getInt(i)); }
	return !doomed.isEmpty();
}

/** Drops everything {@code other} holds. True when this set changed. */
default boolean removeAll(ACS_IntSet other) {
	boolean changed = false;
	// Drive from whichever side is smaller; removing by value needs no position bookkeeping.
	if(other.size() <= size()) {
		for(int i = 0, n = other.size(); i < n; i++) { changed |= remove(other.get(i)); }
		return changed;
	}
	IntArrayList doomed = intersect(other);
	for(int i = 0; i < doomed.size(); i++) { changed |= remove(doomed.getInt(i)); }
	return changed;
}

/** Adds everything {@code other} holds. True when this set changed. */
default boolean addAll(ACS_IntSet other) {
	boolean changed = false;
	for(int i = 0, n = other.size(); i < n; i++) { changed |= add(other.get(i)); }
	return changed;
}

/** Whether every element of {@code other} is in this set. */
default boolean containsAll(ACS_IntSet other) {
	if(other.size() > size()) { return false; }
	for(int i = 0, n = other.size(); i < n; i++) { if(!contains(other.get(i))) { return false; } }
	return true;
}


	
	List<Integer> getIntList();
	IntArrayList toList();


}
