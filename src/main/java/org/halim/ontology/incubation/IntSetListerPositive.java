package org.halim.ontology.incubation;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A set of ints stored positively: {@code inclusionList} holds the elements themselves, sorted
 * ascending with no duplicates. The mirror image of {@link IntSetListerNegative}, which lists what
 * the set leaves out instead - same {@link OE_IntSet} contract over either, so the pair is meant to be
 * swapped for one another as a set's density changes.
 * <p>
 * Memory is proportional to the number of elements, so this is the representation to reach for when
 * a set is sparse: elements scattered across the whole int range cost the same here as elements
 * packed together, and there is no gap to pay for. A dense set is where it loses - a full range of
 * n costs n ints here against one pair of them for the negative form.
 * <p>
 * Costs: {@code get} is a straight array read, membership is a binary search, and {@code add} and
 * {@code remove} pay a shift on top of that search to keep the list packed and sorted.
 * <p>
 * One invariant holds at all times, and everything below leans on it: {@code inclusionList} is
 * sorted ascending with no duplicates. An empty set is an empty list.
 * <p>
 * Not multi thread safe, and every {@link OE_IntSet} handed out by {@link #getSet()} is a view over
 * the same state - mutating through one is visible through all of them.
 * */
public class IntSetListerPositive {


IntArrayList inclusionList;

/** The smallest element, or 0 for an empty set - which then reads as the empty range [0, 0). */
int getFirst() { return isEmpty() ? 0 : inclusionList.getInt(0); }

/** Exclusive like the intended java behavior. */
int getEnd() { return isEmpty() ? 0 : inclusionList.getInt(inclusionList.size() - 1) + 1; }

/** The empty set, which is where a sparse one naturally starts. */
public IntSetListerPositive() {
	inclusionList = new IntArrayList();
}

/** Prefilled with [firstIndex, lastIndex], both ends inclusive, to mirror the negative form. */
public IntSetListerPositive(int firstIndex, int lastIndex) {
	int length = lastIndex - firstIndex + 1;
	inclusionList = new IntArrayList(Math.max(length, 0));
	if(length <= 0) { return; }
	int[] range = new int[length];
	for(int k = 0; k < length; k++) { range[k] = firstIndex + k; }
	inclusionList.addElements(0, range, 0, length);
}
public IntSetListerPositive(int lastIndex) {
	this(0, lastIndex);
}


OE_IntSet getSet() {
	return new OE_IntSet() {
		@Override
		public int get(int i) {
			int size = count();
			if(i < 0 || i >= size) { throw new IndexOutOfBoundsException("index " + i + ", size " + size); }
			return elementAt(i);
		}
		
		@Override
		public boolean add(int i) {
			return addValue(i);
		}
		@Override
		public boolean remove(int i) {
			return removeValue(i);
		}
		
		/** A member exactly when the sorted element list holds it. */
		@Override
		public boolean contains(int i) {
			return inclusionSlot(i) >= 0;
		}
		
		@Override
		public int size() {
			return count();
		}
		
		@Override
		public IntListerIterator iterator() {
			return new Cursor();
		}
		
		@Override
		public List<Integer> getIntList() {
			return IntSetListerPositive.this.toList();
		}
		
		@Override
		public IntArrayList toList() {
			return IntSetListerPositive.this.toIntArrayList();
		}
	};
}


/**
 * Walks the iteration order. The list is already in that order, so the cursor is an index into it
 * and nothing has to be translated.
 * */
private final class Cursor implements OE_IntSet.IntListerIterator {
	
	int cursor;
	int lastReturned = -1;
	
	@Override
	public boolean hasNext() { return cursor < count(); }
	
	@Override
	public boolean hasPrevious() { return cursor > 0; }
	
	@Override
	public int next() {
		if(!hasNext()) { throw new NoSuchElementException(); }
		lastReturned = cursor;
		return elementAt(cursor++);
	}
	
	@Override
	public int previous() {
		if(!hasPrevious()) { throw new NoSuchElementException(); }
		lastReturned = --cursor;
		return elementAt(lastReturned);
	}
	
	@Override
	public void remove() {
		if(lastReturned < 0) { throw new IllegalStateException("no element to remove"); }
		inclusionList.removeInt(lastReturned);
		// Everything past the removed element slid down one index. Coming from next() the cursor
		// sat after it and has to follow; coming from previous() it sat on it and already points
		// at the right successor.
		if(lastReturned < cursor) { cursor--; }
		lastReturned = -1;
	}
}


boolean isEmpty() {
	return count() == 0;
}

int count() {
	return inclusionList.size();
}

/** The i-th element, with i already known to be in range. The list is the iteration order. */
int elementAt(int i) {
	return inclusionList.getInt(i);
}

/**
 * Position of {@code value} in {@code inclusionList}, or {@code -(insertion point) - 1} when it is
 * not a member - the same encoding {@code Arrays.binarySearch} uses.
 * */
int inclusionSlot(int value) {
	int low = 0, high = inclusionList.size() - 1;
	while(low <= high) {
		int mid = (low + high) >>> 1;
		int element = inclusionList.getInt(mid);
		if(element < value) { low = mid + 1; }
		else if(element > value) { high = mid - 1; }
		else { return mid; }
	}
	return -(low + 1);
}

boolean addValue(int value) {
	// Nothing here cares where the value lands relative to the rest, which is the whole point of
	// the positive form: no range to widen, no gap to pay for.
	int slot = inclusionSlot(value);
	if(slot >= 0) { return false; }
	inclusionList.add(-slot - 1, value);
	return true;
}

boolean removeValue(int value) {
	int slot = inclusionSlot(value);
	if(slot < 0) { return false; }
	inclusionList.removeInt(slot);
	return true;
}

/** Snapshots the set in ascending order, detached from the storage the way the negative form is. */
List<Integer> toList() {
	List<Integer> elements = new ArrayList<>(count());
	for(int i = 0; i < inclusionList.size(); i++) { elements.add(inclusionList.getInt(i)); }
	return Collections.unmodifiableList(elements);
}
/** Snapshots the set in ascending order, detached from the storage the way the negative form is. */
IntArrayList toIntArrayList() {
	return inclusionList.clone();
}

@Override
public String toString() {
	return getClass().getSimpleName() + inclusionList;
}


}
