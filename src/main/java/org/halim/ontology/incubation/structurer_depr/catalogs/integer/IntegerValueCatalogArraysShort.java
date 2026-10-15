package org.halim.ontology.incubation.structurer_depr.catalogs.integer;

import org.halim.ontology.incubation.structurer_depr.ArityModule;
import org.halim.ontology.incubation.structurer_depr.catalogs.ChunkedCatalogBase;
import org.halim.ontology.incubation.structurer_depr.catalogs.LongChunkInt;
import org.halim.ontology.incubation.structurer_depr.catalogs.IntegerValueCatalog.IntSizedIntegerValueCatalog;

import java.util.Arrays;

/**
 * The chunked short catalog: one short a slot, 2 bytes an identity.
 * <p>
 * Growth, truncation and the chunk list itself live in {@link ChunkedCatalogBase} - everything
 * here is the four array hooks and the two accessors, which is all that actually depends on the
 * stored type.
 * */
public class IntegerValueCatalogArraysShort extends ChunkedCatalogBase<short[]> implements IntSizedIntegerValueCatalog {

/** The chunked short catalog: one short a slot, 2 bytes an identity. */
@Override
public double getEstimatedBytesPerIdentity() { return 2; }

public IntegerValueCatalogArraysShort() {
	this(new LongChunkInt.LongChunkIntPowerOfTwo(22));
}
public IntegerValueCatalogArraysShort(LongChunkInt chunking) {
	super(chunking);
}


@Override
protected short[] allocate(int length) { return new short[length]; }
@Override
protected int lengthOf(short[] chunk) { return chunk.length; }
@Override
protected short[] copyOf(short[] chunk, int length) { return Arrays.copyOf(chunk, length); }
@Override
protected void clear(short[] chunk, int from, int to) { Arrays.fill(chunk, from, to, (short) 0); }


@Override
public int getIntData(long identity) {
	return data.get(chunking.chunkOf(identity))[chunking.offsetOf(identity)];
}

public boolean setData(long identity, short value) {
	boolean changed = false;
	if(identity > lastIndex) { ensureCapacity(identity); changed = true; }
	setDataRaw(identity, value);
	return changed;
}

/**
 * The same write from the wide side, so a caller holding long values does not have to narrow them
 * itself. Narrowing to a short wraps rather than saturating, and a wrapped value is
 * indistinguishable from a real one, so this refuses instead - which is the signal that the catalog
 * wanted was a wider one.
 * */
public boolean setData(long identity, long value) {
	return setData(identity, ArityModule.exactShort(value));
}

/** Skips the growth check, so the caller carries the invariant that the slot already exists. */
public void setDataRaw(long identity, short value) {
	data.get(chunking.chunkOf(identity))[chunking.offsetOf(identity)] = value;
}



/**
 * The range copied chunk by chunk rather than element by element. The chunk split is resolved once
 * per block instead of once per identity, and inside a block the copy is a run - so the cost of
 * chunking, which the per-element path pays on every read, is amortised over the whole run.
 * */
@Override
public void copyIntsInto(long from, long to, int[] dest, int offset) {
	for(long identity = from; identity < to; ) {
		short[] block = data.get(chunking.chunkOf(identity));
		int start = chunking.offsetOf(identity);
		int run = (int) Math.min(to - identity, block.length - start);
		if(run <= 0) { throw new IndexOutOfBoundsException("identity " + identity + " is past this catalog"); }
		for(int i = 0; i < run; i++) { dest[offset + i] = block[start + i]; }
		identity += run;
		offset += run;
	}
}

}
