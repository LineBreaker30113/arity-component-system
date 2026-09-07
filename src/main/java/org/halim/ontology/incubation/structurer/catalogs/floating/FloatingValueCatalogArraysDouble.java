package org.halim.ontology.incubation.structurer.catalogs.floating;

import org.halim.ontology.incubation.structurer.catalogs.AnyCatalogBase;
import org.halim.ontology.incubation.structurer.catalogs.ChunkedCatalogBase;
import org.halim.ontology.incubation.structurer.catalogs.LongChunkInt;
import org.halim.ontology.incubation.structurer.catalogs.FloatingValueCatalog;

import java.util.Arrays;

/**
 * The chunked double catalog: one double a slot, 8 bytes an identity.
 * <p>
 * Growth, truncation and the chunk list itself live in {@link ChunkedCatalogBase} - everything
 * here is the four array hooks and the two accessors, which is all that actually depends on the
 * stored type.
 * */
public class FloatingValueCatalogArraysDouble extends ChunkedCatalogBase<double[]> implements FloatingValueCatalog {

/** The chunked double catalog: one double a slot, 8 bytes an identity. */
@Override
public double getEstimatedBytesPerIdentity() { return 8; }

public FloatingValueCatalogArraysDouble() {
	this(new LongChunkInt.LongChunkIntPowerOfTwo(22));
}
public FloatingValueCatalogArraysDouble(LongChunkInt chunking) {
	super(chunking);
}


@Override
protected double[] allocate(int length) { return new double[length]; }
@Override
protected int lengthOf(double[] chunk) { return chunk.length; }
@Override
protected double[] copyOf(double[] chunk, int length) { return Arrays.copyOf(chunk, length); }
@Override
protected void clear(double[] chunk, int from, int to) { Arrays.fill(chunk, from, to, (double) 0); }


@Override
public double getData(long identity) {
	return data.get(chunking.chunkOf(identity))[chunking.offsetOf(identity)];
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
