package org.halim.ontology.incubation.structurer.catalogs;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

import java.util.ArrayList;
import java.util.Arrays;

public interface DoublefCatalog {

double getData(long identity);


class DoublefCatalogListerDouble implements DoublefCatalog {
	public DoubleArrayList data = new DoubleArrayList();
	
	@Override
	public double getData(long identity) {
		return data.getDouble((int) identity);
	}
	
}

/**
 * The chunked double catalog: {@link #data} is a list of blocks, so the identity space runs past what
 * one Java array can index without ever asking the allocator for one huge run.
 * <p>
 * Where an identity lands is not this class's business - {@link #chunking} owns the split, and
 * swapping it changes the block length without a line here moving. What this class owns is growth.
 * <p>
 * Only the last chunk is ever short. {@link #ensureCapacity} grows it by copying rather than
 * replacing it, since the tail holds real values, and grows it geometrically rather than to exactly
 * what was asked - copying the whole tail on every write would make filling one chunk quadratic.
 * Every chunk below the tail is full-length forever, so a chunk is only ever allocated whole once a
 * later chunk exists, which is to say once the catalog genuinely holds that many elements.
 * */
class DoublefCatalogArraysDouble implements DoublefCatalog {
	
	/** Where an identity sits. Shared and stateless, so one instance serves any number of catalogs. */
	public LongChunkInt chunking;
	public ArrayList<double[]> data = new ArrayList<>();
	/** The highest identity currently addressable, not a count. An empty catalog is -1. */
	public long lastIndex = -1;
	
	public DoublefCatalogArraysDouble() {
		this(new LongChunkInt.LongChunkIntPowerOfTwo(22));
	}
	public DoublefCatalogArraysDouble(LongChunkInt chunking) {
		this.chunking = chunking;
	}
	
	@Override
	public double getData(long identity) {
		return data.get(chunking.chunkOf(identity))[chunking.offsetOf(identity)];
	}
	
	public boolean ensureCapacity(long capacity) {
		if(capacity <= lastIndex) { truncate(capacity); return false; }
		int lastChunk = chunking.chunkOf(capacity);
		// The tail is the only chunk allowed to be short, so it is the only one that has to grow -
		// copied rather than dropped, since dropping it would drop everything already written in it.
		if(!data.isEmpty()) {
			int at = data.size() - 1;
			double[] tail = data.get(at);
			int want = chunking.lengthOf(at, capacity);
			if(tail.length < want) { data.set(at, Arrays.copyOf(tail, grown(tail.length, want))); }
		}
		while(data.size() <= lastChunk) {
			data.addLast(new double[grown(0, chunking.lengthOf(data.size(), capacity))]);
		}
		lastIndex = capacity;
		return true;
	}
	
	/**
	 * Lowers the ceiling to {@code capacity} and gives back what sat above it. Every chunk entirely
	 * past the new tail is dropped outright, which is where the memory actually is - a chunk is a
	 * block of {@link LongChunkInt#fullLength()} elements and nothing smaller is worth freeing.
	 * <p>
	 * The surviving tail is shrunk only when it is more than twice the length still needed, and then
	 * with headroom left on it. Shrinking to exactly what is needed would copy the whole tail back on
	 * the very next write, so a catalog oscillating around one capacity would pay a full copy each
	 * way; the slack is what keeps that from happening.
	 * <p>
	 * Whatever survives above the new ceiling is cleared. A dropped chunk comes back zeroed when the
	 * catalog grows again, so a tail that was merely shortened has to as well - otherwise the same
	 * identity reads back a stale value on one side of a chunk boundary and zero on the other.
	 * */
	void truncate(long capacity) {
		int keep = chunking.chunkCount(capacity);
		while(data.size() > keep) { data.removeLast(); }
		if(!data.isEmpty()) {
			int at = data.size() - 1;
			double[] tail = data.get(at);
			int want = chunking.lengthOf(at, capacity);
			if(tail.length / 2 > want) {
				tail = Arrays.copyOf(tail, grown(want, want));
				data.set(at, tail);
			}
			Arrays.fill(tail, want, tail.length, 0);
		}
		// The empty catalog keeps exactly one encoding, the way an empty range does.
		lastIndex = Math.max(capacity, -1);
	}
	
	/**
	 * Tail growth, geometric rather than exact. Growing to precisely what was asked walks the whole
	 * tail again on every write, which turns filling one chunk into quadratic copying; half again
	 * over the request amortises it, and the full chunk length is the ceiling because past that a
	 * new chunk takes over.
	 * */
	int grown(int current, int want) {
		return Math.min(chunking.fullLength(), Math.max(want, current + (current >> 1) + 8));
	}
	
	public boolean setData(long identity, double value) {
		boolean changed = false;
		if(identity > lastIndex) { ensureCapacity(identity); changed = true; }
		setDataRaw(identity, value);
		return changed;
	}
	
	/** Skips the growth check, so the caller carries the invariant that the slot already exists. */
	public void setDataRaw(long identity, double value) {
		data.get(chunking.chunkOf(identity))[chunking.offsetOf(identity)] = value;
	}
	
}


/**
 * The same catalog stored in floats. Every value still leaves through {@link #getData} as a double -
 * the widening is exact, so nothing downstream has to know - but the storage is half the size, which
 * is the whole point: a catalog is one value per element, so the width chosen here is multiplied by
 * the entity count and by nothing else.
 * <p>
 * What is paid for that is precision, not range: a float carries about seven decimal digits against
 * a double's sixteen. Writing goes through a narrowing cast that rounds rather than throwing, since
 * unlike a truncated identity a rounded measurement is still the value it claims to be. A catalog of
 * scores, weights or coordinates is the case for this variant; a catalog of exact sums is not.
 * */
class DoublefCatalogListerFloat implements DoublefCatalog {
	public FloatArrayList data = new FloatArrayList();
	
	@Override
	public double getData(long identity) {
		return data.getFloat((int) identity);
	}
	
}

/**
 * The chunked float catalog: {@link #data} is a list of blocks, so the identity space runs past what
 * one Java array can index without ever asking the allocator for one huge run.
 * <p>
 * Where an identity lands is not this class's business - {@link #chunking} owns the split, and
 * swapping it changes the block length without a line here moving. What this class owns is growth.
 * <p>
 * Only the last chunk is ever short. {@link #ensureCapacity} grows it by copying rather than
 * replacing it, since the tail holds real values, and grows it geometrically rather than to exactly
 * what was asked - copying the whole tail on every write would make filling one chunk quadratic.
 * Every chunk below the tail is full-length forever, so a chunk is only ever allocated whole once a
 * later chunk exists, which is to say once the catalog genuinely holds that many elements.
 * */
class DoublefCatalogArraysFloat implements DoublefCatalog {
	
	/** Where an identity sits. Shared and stateless, so one instance serves any number of catalogs. */
	public LongChunkInt chunking;
	public ArrayList<float[]> data = new ArrayList<>();
	/** The highest identity currently addressable, not a count. An empty catalog is -1. */
	public long lastIndex = -1;
	
	public DoublefCatalogArraysFloat() {
		this(new LongChunkInt.LongChunkIntPowerOfTwo(22));
	}
	public DoublefCatalogArraysFloat(LongChunkInt chunking) {
		this.chunking = chunking;
	}
	
	@Override
	public double getData(long identity) {
		return data.get(chunking.chunkOf(identity))[chunking.offsetOf(identity)];
	}
	
	public boolean ensureCapacity(long capacity) {
		if(capacity <= lastIndex) { truncate(capacity); return false; }
		int lastChunk = chunking.chunkOf(capacity);
		// The tail is the only chunk allowed to be short, so it is the only one that has to grow -
		// copied rather than dropped, since dropping it would drop everything already written in it.
		if(!data.isEmpty()) {
			int at = data.size() - 1;
			float[] tail = data.get(at);
			int want = chunking.lengthOf(at, capacity);
			if(tail.length < want) { data.set(at, Arrays.copyOf(tail, grown(tail.length, want))); }
		}
		while(data.size() <= lastChunk) {
			data.addLast(new float[grown(0, chunking.lengthOf(data.size(), capacity))]);
		}
		lastIndex = capacity;
		return true;
	}
	
	/**
	 * Lowers the ceiling to {@code capacity} and gives back what sat above it. Every chunk entirely
	 * past the new tail is dropped outright, which is where the memory actually is - a chunk is a
	 * block of {@link LongChunkInt#fullLength()} elements and nothing smaller is worth freeing.
	 * <p>
	 * The surviving tail is shrunk only when it is more than twice the length still needed, and then
	 * with headroom left on it. Shrinking to exactly what is needed would copy the whole tail back on
	 * the very next write, so a catalog oscillating around one capacity would pay a full copy each
	 * way; the slack is what keeps that from happening.
	 * <p>
	 * Whatever survives above the new ceiling is cleared. A dropped chunk comes back zeroed when the
	 * catalog grows again, so a tail that was merely shortened has to as well - otherwise the same
	 * identity reads back a stale value on one side of a chunk boundary and zero on the other.
	 * */
	void truncate(long capacity) {
		int keep = chunking.chunkCount(capacity);
		while(data.size() > keep) { data.removeLast(); }
		if(!data.isEmpty()) {
			int at = data.size() - 1;
			float[] tail = data.get(at);
			int want = chunking.lengthOf(at, capacity);
			if(tail.length / 2 > want) {
				tail = Arrays.copyOf(tail, grown(want, want));
				data.set(at, tail);
			}
			Arrays.fill(tail, want, tail.length, 0);
		}
		// The empty catalog keeps exactly one encoding, the way an empty range does.
		lastIndex = Math.max(capacity, -1);
	}
	
	/**
	 * Tail growth, geometric rather than exact. Growing to precisely what was asked walks the whole
	 * tail again on every write, which turns filling one chunk into quadratic copying; half again
	 * over the request amortises it, and the full chunk length is the ceiling because past that a
	 * new chunk takes over.
	 * */
	int grown(int current, int want) {
		return Math.min(chunking.fullLength(), Math.max(want, current + (current >> 1) + 8));
	}
	
	public boolean setData(long identity, double value) {
		boolean changed = false;
		if(identity > lastIndex) { ensureCapacity(identity); changed = true; }
		setDataRaw(identity, value);
		return changed;
	}
	
	/** Skips the growth check, so the caller carries the invariant that the slot already exists. */
	public void setDataRaw(long identity, double value) {
		data.get(chunking.chunkOf(identity))[chunking.offsetOf(identity)] = (float) value;
	}
	
}

}
