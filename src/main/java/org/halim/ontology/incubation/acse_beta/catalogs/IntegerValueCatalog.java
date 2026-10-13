package org.halim.ontology.incubation.acse_beta.catalogs;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import org.halim.ontology.incubation.acse_beta.ACSE_ArityCatalogRowBase;
import org.halim.ontology.incubation.acse_beta.ACSE_Core;
import org.halim.ontology.incubation.acse_beta.ACSE_Journal;
import org.halim.ontology.incubation.acse_beta.ACSE_ArityComponentBase;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


/**
 * A catalog of integral values. {@link #getData} is the one thing an implementation supplies; the
 * views onto it follow from that and are exact or they throw.
 * <p>
 * The stored width is the implementation's business - the int variants below answer the same
 * {@code getData} as the long ones, widening on the way out, and differ only in what
 * {@link #getEstimatedBytesPerIdentity} reports.
 * <p>
 * Unlike the floating catalogs, narrowing here is not a rounding: a long that does not fit an int
 * wraps, and a wrapped value reads back as a perfectly plausible different value. So the width a
 * catalog was built at is a promise about its contents, and {@link IntSizedIntegerValueCatalog}
 * exists to carry that promise in the type rather than in a comment.
 * */
public interface IntegerValueCatalog extends ACSE_ArityCatalogRowBase {

long getData(long identity);


@Override
default int getPreferredType() { return PREFERRED_LONGI; }

@Override
default long getLongFor(long identity) {
	return getData(identity);
}

@Override
default int getIntFor(long identity) {
	return ACSE_ArityComponentBase.exactInt(getData(identity));
}

@Override
default double getDoubleFor(long identity) {
	return ACSE_ArityComponentBase.exactDouble(getData(identity));
}


/**
 * An integral catalog whose values are known to fit in an int, which is what
 * {@link ACSE_ArityComponentBase#PREFERRED_INTEGER} means: integers can be used carefreely.
 * <p>
 * The promise is structural rather than checked. {@link #getIntData} is the stored value, so there
 * is nothing to verify on the way out - {@link #getIntFor} cannot throw here, and neither can
 * {@link #getDoubleFor}, since every int is an exact double. That is the difference the subtype
 * buys: on a plain {@link IntegerValueCatalog} both of those are conversions that may refuse a
 * value, and a caller has to be ready for it.
 * <p>
 * Both widths are usable from either side. Reading through {@code getData} widens, which is free and
 * exact; writing from a long goes through a check, since that direction is the one that can lose.
 * */
interface IntSizedIntegerValueCatalog extends IntegerValueCatalog {
	
	int getIntData(long identity);
	
	/** Widening, so the wide interface works unchanged on a narrow catalog. Always exact. */
	@Override
	default long getData(long identity) { return getIntData(identity); }
	
	@Override
	default int getPreferredType() { return PREFERRED_INTEGER; }
	
	/** The stored value itself - no conversion, so no refusal. */
	@Override
	default int getIntFor(long identity) { return getIntData(identity); }
	
	/** Every int is an exact double; the 2^53 question the wide form has to ask cannot arise. */
	@Override
	default double getDoubleFor(long identity) { return getIntData(identity); }
	
}


class IntegerValueCatalogListerLong implements IntegerValueCatalog {
	
	/** One long a slot, packed - the list is the storage. */
	@Override
	public double getEstimatedBytesPerIdentity() { return 8; }
	public LongArrayList data = new LongArrayList();
	
	@Override
	public long getData(long identity) {
		return data.getLong((int) identity);
	}
	
	
	/** The flat list is the storage, so reaching an identity is just making the list that long. */
	@Override
	public boolean ensureCapacity(long identity) {
		int want = Math.toIntExact(identity + 1);
		if(data.size() >= want) { return false; }
		data.size(want);
		return true;
	}
	
	@Override
	public void truncate(long capacity) {
		data.size(capacity < 0 ? 0 : Math.toIntExact(capacity + 1));
	}

	/** The length, then every value. */
	@Override
	public boolean saveTo(DataOutputStream dos) throws IOException {
		dos.writeInt(data.size());
		for(int i = 0; i < data.size(); i++) { dos.writeLong(data.getLong(i)); }
		return true;
	}

	/** Rebuilds what {@link #saveTo} wrote. */
	public static IntegerValueCatalogListerLong loadFrom(ACSE_Core core, ACSE_Journal journal, DataInputStream dis) throws IOException {
		IntegerValueCatalogListerLong catalog = new IntegerValueCatalogListerLong();
		int size = dis.readInt();
		catalog.data = new LongArrayList(size);
		for(int i = 0; i < size; i++) { catalog.data.add(dis.readLong()); }
		return catalog;
	}


	/**
	 * The flat list is the storage, so the whole range is one run and there is no chunk split to pay
	 * for. This is the case the per-element default is worst against - the loop it replaces was all
	 * dispatch and no work.
	 * */
	@Override
	public void copyLongsInto(long from, long to, long[] dest, int offset) {
		data.getElements(Math.toIntExact(from), dest, offset, Math.toIntExact(to - from));
	}

}


class IntegerValueCatalogListerInt implements IntSizedIntegerValueCatalog {
	
	/** One int a slot, packed - the list is the storage. */
	@Override
	public double getEstimatedBytesPerIdentity() { return 4; }
	public IntArrayList data = new IntArrayList();
	
	@Override
	public int getIntData(long identity) {
		return data.getInt((int) identity);
	}
	
	
	/** The flat list is the storage, so reaching an identity is just making the list that long. */
	@Override
	public boolean ensureCapacity(long identity) {
		int want = Math.toIntExact(identity + 1);
		if(data.size() >= want) { return false; }
		data.size(want);
		return true;
	}
	
	@Override
	public void truncate(long capacity) {
		data.size(capacity < 0 ? 0 : Math.toIntExact(capacity + 1));
	}

	/** The length, then every value. */
	@Override
	public boolean saveTo(DataOutputStream dos) throws IOException {
		dos.writeInt(data.size());
		for(int i = 0; i < data.size(); i++) { dos.writeInt(data.getInt(i)); }
		return true;
	}

	/** Rebuilds what {@link #saveTo} wrote. */
	public static IntegerValueCatalogListerInt loadFrom(ACSE_Core core, ACSE_Journal journal, DataInputStream dis) throws IOException {
		IntegerValueCatalogListerInt catalog = new IntegerValueCatalogListerInt();
		int size = dis.readInt();
		catalog.data = new IntArrayList(size);
		for(int i = 0; i < size; i++) { catalog.data.add(dis.readInt()); }
		return catalog;
	}


	/**
	 * The flat list is the storage, so the whole range is one run and there is no chunk split to pay
	 * for. This is the case the per-element default is worst against - the loop it replaces was all
	 * dispatch and no work.
	 * */
	@Override
	public void copyIntsInto(long from, long to, int[] dest, int offset) {
		data.getElements(Math.toIntExact(from), dest, offset, Math.toIntExact(to - from));
	}

}


class IntegerValueCatalogListerShort implements IntSizedIntegerValueCatalog {
	
	/** One short a slot, packed - the list is the storage. */
	@Override
	public double getEstimatedBytesPerIdentity() { return 2; }
	public ShortArrayList data = new ShortArrayList();
	
	@Override
	public int getIntData(long identity) {
		return data.getShort((int) identity);
	}
	
	
	/** The flat list is the storage, so reaching an identity is just making the list that long. */
	@Override
	public boolean ensureCapacity(long identity) {
		int want = Math.toIntExact(identity + 1);
		if(data.size() >= want) { return false; }
		data.size(want);
		return true;
	}
	
	@Override
	public void truncate(long capacity) {
		data.size(capacity < 0 ? 0 : Math.toIntExact(capacity + 1));
	}

	/** The length, then every value. */
	@Override
	public boolean saveTo(DataOutputStream dos) throws IOException {
		dos.writeInt(data.size());
		for(int i = 0; i < data.size(); i++) { dos.writeShort(data.getShort(i)); }
		return true;
	}

	/** Rebuilds what {@link #saveTo} wrote. */
	public static IntegerValueCatalogListerShort loadFrom(ACSE_Core core, ACSE_Journal journal, DataInputStream dis) throws IOException {
		IntegerValueCatalogListerShort catalog = new IntegerValueCatalogListerShort();
		int size = dis.readInt();
		catalog.data = new ShortArrayList(size);
		for(int i = 0; i < size; i++) { catalog.data.add(dis.readShort()); }
		return catalog;
	}


	/**
	 * The flat list is the storage, so the whole range is one run and there is no chunk split to pay
	 * for. This is the case the per-element default is worst against - the loop it replaces was all
	 * dispatch and no work.
	 * */
	@Override
	public void copyIntsInto(long from, long to, int[] dest, int offset) {
		short[] elements = data.elements();
		int at = Math.toIntExact(from);
		int end = Math.toIntExact(to);
		if(end > data.size()) { throw new IndexOutOfBoundsException("identity " + (end - 1) + " is past this catalog"); }
		while(at < end) { dest[offset++] = elements[at++]; }
	}

}


class IntegerValueCatalogListerByte implements IntSizedIntegerValueCatalog {
	
	/** One byte a slot, packed - the list is the storage. */
	@Override
	public double getEstimatedBytesPerIdentity() { return 1; }
	public ByteArrayList data = new ByteArrayList();
	
	@Override
	public int getIntData(long identity) {
		return data.getByte((int) identity);
	}
	
	
	/** The flat list is the storage, so reaching an identity is just making the list that long. */
	@Override
	public boolean ensureCapacity(long identity) {
		int want = Math.toIntExact(identity + 1);
		if(data.size() >= want) { return false; }
		data.size(want);
		return true;
	}
	
	@Override
	public void truncate(long capacity) {
		data.size(capacity < 0 ? 0 : Math.toIntExact(capacity + 1));
	}

	/** The length, then every value. */
	@Override
	public boolean saveTo(DataOutputStream dos) throws IOException {
		dos.writeInt(data.size());
		dos.write(data.elements(), 0, data.size());
		return true;
	}

	/** Rebuilds what {@link #saveTo} wrote. */
	public static IntegerValueCatalogListerByte loadFrom(ACSE_Core core, ACSE_Journal journal, DataInputStream dis) throws IOException {
		IntegerValueCatalogListerByte catalog = new IntegerValueCatalogListerByte();
		int size = dis.readInt();
		byte[] values = new byte[size];
		dis.readFully(values);
		catalog.data = ByteArrayList.wrap(values);
		return catalog;
	}


	/**
	 * The flat list is the storage, so the whole range is one run and there is no chunk split to pay
	 * for. This is the case the per-element default is worst against - the loop it replaces was all
	 * dispatch and no work.
	 * */
	@Override
	public void copyIntsInto(long from, long to, int[] dest, int offset) {
		byte[] elements = data.elements();
		int at = Math.toIntExact(from);
		int end = Math.toIntExact(to);
		if(end > data.size()) { throw new IndexOutOfBoundsException("identity " + (end - 1) + " is past this catalog"); }
		while(at < end) { dest[offset++] = elements[at++]; }
	}

}


}
