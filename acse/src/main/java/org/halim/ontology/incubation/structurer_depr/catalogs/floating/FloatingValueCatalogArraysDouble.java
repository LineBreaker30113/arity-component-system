package org.halim.ontology.incubation.structurer_depr.catalogs.floating;

import org.halim.ontology.incubation.structurer_depr.catalogs.ChunkedCatalogBase;
import org.halim.ontology.incubation.structurer_depr.catalogs.LongChunkInt;
import org.halim.ontology.incubation.structurer_depr.catalogs.FloatingValueCatalog;

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



/**
 * The range copied chunk by chunk rather than element by element. The chunk split is resolved once
 * per block instead of once per identity, and inside a block the copy is a run - so the cost of
 * chunking, which the per-element path pays on every read, is amortised over the whole run.
 * */
@Override
public void copyDoublesInto(long from, long to, double[] dest, int offset) {
	for(long identity = from; identity < to; ) {
		double[] block = data.get(chunking.chunkOf(identity));
		int start = chunking.offsetOf(identity);
		int run = (int) Math.min(to - identity, block.length - start);
		if(run <= 0) { throw new IndexOutOfBoundsException("identity " + identity + " is past this catalog"); }
		System.arraycopy(block, start, dest, offset, run);
		identity += run;
		offset += run;
	}
}

}
