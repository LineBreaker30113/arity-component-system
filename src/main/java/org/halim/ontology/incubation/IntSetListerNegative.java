package org.halim.ontology.incubation;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A set of ints stored negatively: the contiguous range [firstIndex, lastIndex], both ends
 * inclusive, minus the holes punched in it by {@code exclusionList}. Memory is proportional to
 * the number of holes, not to the number of elements, so this is the representation to reach for
 * when a set is dense - a full range costs one pair of ints.
 * <p>
 * The flip side is that sparsity is expensive. Adding an element far outside the range widens the
 * range to reach it and turns every integer it swallowed along the way into a hole, so one add can
 * cost the whole gap.
 * <p>
 * Two invariants hold at all times, and everything below leans on them:
 * <ul>
 *   <li>{@code exclusionList} is sorted ascending with no duplicates, and every hole lies strictly
 *       between the bounds;</li>
 *   <li>neither bound is itself a hole, so the first element is always {@code firstIndex} and the
 *       last is always {@code lastIndex}. An empty set is {@code firstIndex > lastIndex}.</li>
 * </ul>
 * Not multi thread safe, and every {@link OE_IntSet} handed out by {@link #getSet()} is a view over
 * the same state - mutating through one is visible through all of them.
 *
 * Simple, took me few minutes, but better than RoaringBitset for this specific use.
 * */
public class IntSetListerNegative {


IntArrayList exclusionList;
int firstIndex, lastIndex;

/** The smallest element. For an empty set this meets getEnd(), leaving an empty range. */
int getFirst() { return firstIndex; }

/** Exclusive like the intended java behavior. */
int getEnd() { return lastIndex + 1; }

public IntSetListerNegative(int firstIndex, int lastIndex) {
	this.firstIndex = firstIndex;
	// count() reads the span straight off the bounds, so a backwards range would report a negative
	// size and isEmpty() would sail right past it. Pinning the floor at firstIndex - 1 leaves the
	// empty set exactly one encoding.
	this.lastIndex = Math.max(lastIndex, firstIndex - 1);
	exclusionList = new IntArrayList();
}
public IntSetListerNegative(int lastIndex) {
	this.firstIndex = 0;
	this.lastIndex = Math.max(lastIndex, -1);
	exclusionList = new IntArrayList();
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
		
		/** Inside the bounds and not punched out - two comparisons and one binary search. */
		@Override
		public boolean contains(int i) {
			return i >= firstIndex && i <= lastIndex && exclusionSlot(i) < 0;
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
			return IntSetListerNegative.this.toList();
		}
		
		@Override
		public IntArrayList toList() {
			return IntSetListerNegative.this.toIntArrayList();
		}
	};
}


/**
 * Walks the iteration order, and is the one place that turns an element index back into a value.
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
		removeValue(elementAt(lastReturned));
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
	return lastIndex - firstIndex + 1 - exclusionList.size();
}

/**
 * The i-th element, with i already known to be in range.
 * <p>
 * {@code exclusionList.getInt(p) - firstIndex - p} is the number of elements sitting before that
 * hole, and it never decreases, so a binary search finds the first hole the i-th element does not
 * have to jump over. The count of holes below that point is exactly how far the element is pushed
 * up from its dense position.
 * */
int elementAt(int i) {
	int low = 0, high = exclusionList.size();
	while(low < high) {
		int mid = (low + high) >>> 1;
		if(exclusionList.getInt(mid) - firstIndex - mid > i) { high = mid; }
		else { low = mid + 1; }
	}
	return firstIndex + i + low;
}

/**
 * Position of {@code value} in {@code exclusionList}, or {@code -(insertion point) - 1} when it is
 * not a hole - the same encoding {@code Arrays.binarySearch} uses.
 * */
int exclusionSlot(int value) {
	int low = 0, high = exclusionList.size() - 1;
	while(low <= high) {
		int mid = (low + high) >>> 1;
		int hole = exclusionList.getInt(mid);
		if(hole < value) { low = mid + 1; }
		else if(hole > value) { high = mid - 1; }
		else { return mid; }
	}
	return -(low + 1);
}

boolean addValue(int value) {
	if(isEmpty()) {
		firstIndex = lastIndex = value;
		exclusionList.clear();
		return true;
	}
	if(value < firstIndex) {
		// Widening the range swallows the gap wholesale, so every integer in it that is not the
		// new element has to be punched out again.
		punchGap(0, value + 1, firstIndex - 1);
		firstIndex = value;
		return true;
	}
	if(value > lastIndex) {
		punchGap(exclusionList.size(), lastIndex + 1, value - 1);
		lastIndex = value;
		return true;
	}
	// Inside the range, so the value is a member unless a hole says otherwise; filling that hole
	// cannot break either invariant, since the bounds are untouched.
	int slot = exclusionSlot(value);
	if(slot < 0) { return false; }
	exclusionList.removeInt(slot);
	return true;
}

/** Inserts [from, to] as holes at {@code at}, which is where they belong in ascending order. */
private void punchGap(int at, int from, int to) {
	int length = to - from + 1;
	if(length <= 0) { return; }
	int[] gap = new int[length];
	for(int k = 0; k < length; k++) { gap[k] = from + k; }
	exclusionList.addElements(at, gap, 0, length);
}

boolean removeValue(int value) {
	if(isEmpty() || value < firstIndex || value > lastIndex) { return false; }
	// A bound is never a hole, so removing one shrinks the range instead - and then drags it past
	// any holes the shrink just exposed, to restore that invariant.
	if(value == firstIndex) {
		firstIndex++;
		int peel = 0;
		while(peel < exclusionList.size() && exclusionList.getInt(peel) == firstIndex + peel) { peel++; }
		if(peel > 0) {
			exclusionList.removeElements(0, peel);
			firstIndex += peel;
		}
		if(isEmpty()) { exclusionList.clear(); }
		return true;
	}
	if(value == lastIndex) {
		lastIndex--;
		int peel = 0;
		while(peel < exclusionList.size() && exclusionList.getInt(exclusionList.size() - 1 - peel) == lastIndex - peel) { peel++; }
		if(peel > 0) {
			exclusionList.removeElements(exclusionList.size() - peel, exclusionList.size());
			lastIndex -= peel;
		}
		if(isEmpty()) { exclusionList.clear(); }
		return true;
	}
	int slot = exclusionSlot(value);
	if(slot >= 0) { return false; }
	exclusionList.add(-slot - 1, value);
	return true;
}

/** Materialises the set in ascending order. Linear in the size of the set, not of the storage. */
List<Integer> toList() {
	List<Integer> elements = new ArrayList<>(count());
	int hole = 0;
	for(int value = firstIndex; value <= lastIndex; value++) {
		if(hole < exclusionList.size() && exclusionList.getInt(hole) == value) { hole++; continue; }
		elements.add(value);
	}
	return Collections.unmodifiableList(elements);
}
/** Materialises the set in ascending order. Linear in the size of the set, not of the storage. */
IntArrayList toIntArrayList() {
	IntArrayList elements = new IntArrayList(count());
	int hole = 0;
	for(int value = firstIndex; value <= lastIndex; value++) {
		if(hole < exclusionList.size() && exclusionList.getInt(hole) == value) { hole++; continue; }
		elements.add(value);
	}
	return elements;
}

@Override
public String toString() {
	return isEmpty()
			? getClass().getSimpleName() + "[]"
			: getClass().getSimpleName() + "[" + firstIndex + ".." + lastIndex + " minus " + exclusionList + "]";
}


}
