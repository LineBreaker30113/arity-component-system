package org.halim.ontology.incubation.structurer.catalogs.integer;

import org.halim.ontology.incubation.structurer.catalogs.AnyCatalogBase;
import org.halim.ontology.incubation.structurer.catalogs.ChunkedCatalogBase;
import org.halim.ontology.incubation.structurer.catalogs.LongChunkInt;
import org.halim.ontology.incubation.structurer.catalogs.IntegerValueCatalog;

import java.util.Arrays;

/**
 * The chunked long catalog: one long a slot, 8 bytes an identity.
 * <p>
 * Growth, truncation and the chunk list itself live in {@link ChunkedCatalogBase} - everything
 * here is the four array hooks and the two accessors, which is all that actually depends on the
 * stored type.
 * */
public class IntegerValueCatalogArraysLong extends ChunkedCatalogBase<long[]> implements IntegerValueCatalog {

/** The chunked long catalog: one long a slot, 8 bytes an identity. */
@Override
public double getEstimatedBytesPerIdentity() { return 8; }

public IntegerValueCatalogArraysLong() {
	this(new LongChunkInt.LongChunkIntPowerOfTwo(22));
}
public IntegerValueCatalogArraysLong(LongChunkInt chunking) {
	super(chunking);
}


@Override
protected long[] allocate(int length) { return new long[length]; }
@Override
protected int lengthOf(long[] chunk) { return chunk.length; }
@Override
protected long[] copyOf(long[] chunk, int length) { return Arrays.copyOf(chunk, length); }
@Override
protected void clear(long[] chunk, int from, int to) { Arrays.fill(chunk, from, to, (long) 0); }


@Override
public long getData(long identity) {
	return data.get(chunking.chunkOf(identity))[chunking.offsetOf(identity)];
}

public boolean setData(long identity, long value) {
	boolean changed = false;
	if(identity > lastIndex) { ensureCapacity(identity); changed = true; }
	setDataRaw(identity, value);
	return changed;
}

/** Skips the growth check, so the caller carries the invariant that the slot already exists. */
public void setDataRaw(long identity, long value) {
	data.get(chunking.chunkOf(identity))[chunking.offsetOf(identity)] = value;
}


}
