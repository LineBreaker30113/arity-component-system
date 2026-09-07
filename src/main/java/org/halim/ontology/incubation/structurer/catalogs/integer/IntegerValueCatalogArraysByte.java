package org.halim.ontology.incubation.structurer.catalogs.integer;

import org.halim.ontology.incubation.structurer.catalogs.AnyCatalogBase;
import org.halim.ontology.incubation.structurer.catalogs.ChunkedCatalogBase;
import org.halim.ontology.incubation.structurer.catalogs.LongChunkInt;
import org.halim.ontology.incubation.structurer.catalogs.IntegerValueCatalog.IntSizedIntegerValueCatalog;

import java.util.Arrays;

/**
 * The chunked byte catalog: one byte a slot, 1 byte an identity.
 * <p>
 * Growth, truncation and the chunk list itself live in {@link ChunkedCatalogBase} - everything
 * here is the four array hooks and the two accessors, which is all that actually depends on the
 * stored type.
 * */
public class IntegerValueCatalogArraysByte extends ChunkedCatalogBase<byte[]> implements IntSizedIntegerValueCatalog {

/** The chunked byte catalog: one byte a slot, 1 byte an identity. */
@Override
public double getEstimatedBytesPerIdentity() { return 1; }

public IntegerValueCatalogArraysByte() {
	this(new LongChunkInt.LongChunkIntPowerOfTwo(22));
}
public IntegerValueCatalogArraysByte(LongChunkInt chunking) {
	super(chunking);
}


@Override
protected byte[] allocate(int length) { return new byte[length]; }
@Override
protected int lengthOf(byte[] chunk) { return chunk.length; }
@Override
protected byte[] copyOf(byte[] chunk, int length) { return Arrays.copyOf(chunk, length); }
@Override
protected void clear(byte[] chunk, int from, int to) { Arrays.fill(chunk, from, to, (byte) 0); }


@Override
public int getIntData(long identity) {
	return data.get(chunking.chunkOf(identity))[chunking.offsetOf(identity)];
}

public boolean setData(long identity, byte value) {
	boolean changed = false;
	if(identity > lastIndex) { ensureCapacity(identity); changed = true; }
	setDataRaw(identity, value);
	return changed;
}

/**
 * The same write from the wide side, so a caller holding long values does not have to narrow them
 * itself. Narrowing to a byte wraps rather than saturating, and a wrapped value is
 * indistinguishable from a real one, so this refuses instead - which is the signal that the catalog
 * wanted was a wider one.
 * */
public boolean setData(long identity, long value) {
	return setData(identity, AnyCatalogBase.exactByte(value));
}

/** Skips the growth check, so the caller carries the invariant that the slot already exists. */
public void setDataRaw(long identity, byte value) {
	data.get(chunking.chunkOf(identity))[chunking.offsetOf(identity)] = value;
}


}
