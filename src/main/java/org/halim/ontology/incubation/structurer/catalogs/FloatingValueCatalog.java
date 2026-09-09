package org.halim.ontology.incubation.structurer.catalogs;

import org.halim.ontology.incubation.structurer.ArityModule;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;


/**
 * A catalog of floating values. {@link #getData} is the one thing an implementation supplies; the
 * views onto it follow from that and are exact or they throw.
 * <p>
 * The stored width is the implementation's business - the float variants below answer the same
 * {@code getData} as the double ones, widening on the way out, and differ only in what
 * {@link #getEstimatedBytesPerIdentity} reports.
 * */
public interface FloatingValueCatalog extends ArityModule.CatalogModuleBase {

double getData(long identity);


@Override
default int getPreferredType() { return PREFERRED_FLOATING; }

@Override
default double getDoubleFor(long identity) {
	return getData(identity);
}

@Override
default long getLongFor(long identity) {
	return ArityModule.exactLong(getData(identity));
}

@Override
default int getIntFor(long identity) {
	return ArityModule.exactInt(getData(identity));
}


class FloatingValueCatalogListerDouble implements FloatingValueCatalog {
	
	/** One double a slot, packed - the list is the storage. */
	@Override
	public double getEstimatedBytesPerIdentity() { return 8; }
	public DoubleArrayList data = new DoubleArrayList();
	
	@Override
	public double getData(long identity) {
		return data.getDouble((int) identity);
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


	/**
	 * The flat list is the storage, so the whole range is one run and there is no chunk split to pay
	 * for. This is the case the per-element default is worst against - the loop it replaces was all
	 * dispatch and no work.
	 * */
	@Override
	public void copyDoublesInto(long from, long to, double[] dest, int offset) {
		data.getElements(Math.toIntExact(from), dest, offset, Math.toIntExact(to - from));
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
class FloatingValueCatalogListerFloat implements FloatingValueCatalog {
	
	/** One float a slot, packed - half the double form, which is the point. */
	@Override
	public double getEstimatedBytesPerIdentity() { return 4; }
	public FloatArrayList data = new FloatArrayList();
	
	@Override
	public double getData(long identity) {
		return data.getFloat((int) identity);
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


	/**
	 * The flat list is the storage, so the whole range is one run and there is no chunk split to pay
	 * for. This is the case the per-element default is worst against - the loop it replaces was all
	 * dispatch and no work.
	 * */
	@Override
	public void copyDoublesInto(long from, long to, double[] dest, int offset) {
		float[] elements = data.elements();
		int at = Math.toIntExact(from);
		int end = Math.toIntExact(to);
		if(end > data.size()) { throw new IndexOutOfBoundsException("identity " + (end - 1) + " is past this catalog"); }
		while(at < end) { dest[offset++] = elements[at++]; }
	}

}


}
