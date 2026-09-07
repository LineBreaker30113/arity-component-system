package org.halim.ontology.incubation.structurer.catalogs;

import java.util.ArrayList;

/**
 * The chunk list every array-backed catalog keeps, and the growth policy over it. {@link #data} is a
 * list of blocks, so the identity space runs past what one Java array can index without ever asking
 * the allocator for one huge run.
 * <p>
 * {@code A} is the <em>array</em> type - {@code long[]}, {@code double[]}, {@code byte[]} - not the
 * element type, and that is what makes one copy of this logic serve every width without boxing
 * anything. Nothing here ever touches an individual element: chunks are allocated, measured, copied
 * and cleared whole, through the four hooks below, and reading and writing a value stays in the
 * subclass where the primitive type is known. Java cannot yet be generic over primitives, but it
 * does not have to be to share this much.
 * <p>
 * Where an identity lands is not this class's business either - {@link #chunking} owns the split,
 * and swapping it changes the block length without a line here moving. What this class owns is
 * growth, and there are three decisions in it:
 * <ul>
 *   <li>Only the last chunk is ever short, so it is the only one that ever has to grow. It grows by
 *       copying rather than being replaced, since it holds real values.</li>
 *   <li>It grows geometrically rather than to exactly what was asked. Copying the whole tail on
 *       every write would make filling one chunk quadratic, and with a large chunk length that
 *       quadratic runs for a very long time.</li>
 *   <li>Every chunk below the tail is full-length forever, so a chunk is only ever allocated whole
 *       once a later chunk exists - which is to say once the catalog genuinely holds that many
 *       elements, and the memory is the data rather than overhead.</li>
 * </ul>
 * {@link #lastIndex} is the highest identity currently addressable, not a count, and an empty
 * catalog is -1. It is a promise about addressability rather than about allocation: the arrays
 * usually reach further than it does, and those extra slots are not the caller's.
 * */
public abstract class ChunkedCatalogBase<A> {

/** Where an identity sits. Shared and stateless, so one instance serves any number of catalogs. */
public LongChunkInt chunking;
public ArrayList<A> data = new ArrayList<>();
/** The highest identity currently addressable, not a count. An empty catalog is -1. */
public long lastIndex = -1;

protected ChunkedCatalogBase(LongChunkInt chunking) {
	this.chunking = chunking;
}


/** A fresh block of {@code length} elements, zeroed the way a new array is. */
protected abstract A allocate(int length);

/** How long a block is. {@code Array.getLength} would do it reflectively; this does not. */
protected abstract int lengthOf(A chunk);

/** The block grown or shrunk to {@code length}, keeping what overlaps. */
protected abstract A copyOf(A chunk, int length);

/** Zeroes {@code [from, to)} of a block. */
protected abstract void clear(A chunk, int from, int to);


public boolean ensureCapacity(long capacity) {
	if(capacity <= lastIndex) { truncate(capacity); return false; }
	int lastChunk = chunking.chunkOf(capacity);
	// The tail is the only chunk allowed to be short, so it is the only one that has to grow -
	// copied rather than dropped, since dropping it would drop everything already written in it.
	if(!data.isEmpty()) {
		int at = data.size() - 1;
		A tail = data.get(at);
		int want = chunking.lengthOf(at, capacity);
		if(lengthOf(tail) < want) { data.set(at, copyOf(tail, grown(lengthOf(tail), want))); }
	}
	while(data.size() <= lastChunk) {
		data.addLast(allocate(grown(0, chunking.lengthOf(data.size(), capacity))));
	}
	lastIndex = capacity;
	return true;
}

/**
 * Lowers the ceiling to {@code capacity} and gives back what sat above it. Every chunk entirely past
 * the new tail is dropped outright, which is where the memory actually is - a chunk is a block of
 * {@link LongChunkInt#fullLength()} elements and nothing smaller is worth freeing.
 * <p>
 * The surviving tail is shrunk only when it is more than twice the length still needed, and then
 * with headroom left on it. Shrinking to exactly what is needed would copy the whole tail back on
 * the very next write, so a catalog oscillating around one capacity would pay a full copy each way;
 * the slack is what keeps that from happening.
 * <p>
 * Whatever survives above the new ceiling is cleared. A dropped chunk comes back zeroed when the
 * catalog grows again, so a tail that was merely shortened has to as well - otherwise the same
 * identity reads back a stale value on one side of a chunk boundary and zero on the other.
 * */
public void truncate(long capacity) {
	int keep = chunking.chunkCount(capacity);
	while(data.size() > keep) { data.removeLast(); }
	if(!data.isEmpty()) {
		int at = data.size() - 1;
		A tail = data.get(at);
		int want = chunking.lengthOf(at, capacity);
		if(lengthOf(tail) / 2 > want) {
			tail = copyOf(tail, grown(want, want));
			data.set(at, tail);
		}
		clear(tail, want, lengthOf(tail));
	}
	// The empty catalog keeps exactly one encoding, the way an empty range does.
	lastIndex = Math.max(capacity, -1);
}

/**
 * Tail growth, geometric rather than exact. Half again over the request amortises it, and the full
 * chunk length is the ceiling because past that a new chunk takes over.
 * */
protected int grown(int current, int want) {
	return Math.min(chunking.fullLength(), Math.max(want, current + (current >> 1) + 8));
}


}
