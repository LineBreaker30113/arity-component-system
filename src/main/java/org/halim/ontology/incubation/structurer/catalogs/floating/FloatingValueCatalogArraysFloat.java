package org.halim.ontology.incubation.structurer.catalogs.floating;

import org.halim.ontology.incubation.structurer.catalogs.AnyCatalogBase;
import org.halim.ontology.incubation.structurer.catalogs.ChunkedCatalogBase;
import org.halim.ontology.incubation.structurer.catalogs.LongChunkInt;
import org.halim.ontology.incubation.structurer.catalogs.FloatingValueCatalog;

import java.util.Arrays;

/**
 * The chunked float catalog: one float a slot, 4 bytes an identity.
 * <p>
 * Growth, truncation and the chunk list itself live in {@link ChunkedCatalogBase} - everything
 * here is the four array hooks and the two accessors, which is all that actually depends on the
 * stored type.
 * */
public class FloatingValueCatalogArraysFloat extends ChunkedCatalogBase<float[]> implements FloatingValueCatalog {

/** The chunked float catalog: one float a slot, 4 bytes an identity. */
@Override
public double getEstimatedBytesPerIdentity() { return 4; }

public FloatingValueCatalogArraysFloat() {
	this(new LongChunkInt.LongChunkIntPowerOfTwo(22));
}
public FloatingValueCatalogArraysFloat(LongChunkInt chunking) {
	super(chunking);
}


@Override
protected float[] allocate(int length) { return new float[length]; }
@Override
protected int lengthOf(float[] chunk) { return chunk.length; }
@Override
protected float[] copyOf(float[] chunk, int length) { return Arrays.copyOf(chunk, length); }
@Override
protected void clear(float[] chunk, int from, int to) { Arrays.fill(chunk, from, to, (float) 0); }


@Override
public double getData(long identity) {
	return data.get(chunking.chunkOf(identity))[chunking.offsetOf(identity)];
}

/**
 * Writing narrows, and the narrowing rounds rather than throwing: unlike a truncated identity, a
 * rounded measurement is still the value it claims to be. A catalog of scores, weights or
 * coordinates is the case for this variant; a catalog of exact sums is not.
 * */
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
