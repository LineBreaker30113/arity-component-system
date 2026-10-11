package org.halim.ontology.incubation.acse_beta;

import org.halim.ontology.incubation.acse_beta.ACSE_ArityCatalogRowBase;
import org.halim.ontology.incubation.acse_beta.ACSE_ArityComponentBase;
import org.halim.ontology.incubation.acse_beta.sets.IdentitiesSet;

import java.util.ArrayList;

/**
 * The arity whose columns really are objects: a list of {@link ACSE_ArityCatalogRowBase}, addressed by the
 * slot they sit at. This is the general case, the one that takes any storage form the catalogs
 * package offers and mixes widths and types freely in one arity.
 * <p>
 * What it costs is the thing {@link ACSE_ArityComponentBase} exists to let a client avoid. Every read is two
 * hops - table to row, row to storage - and the second is an interface call at a site that
 * sees every catalog implementation the program loaded, so it stops inlining early. A client whose
 * arity is three int columns pays that for nothing; they should implement {@code ACSE_ArityComponentBase}
 * directly over three arrays and skip this class. It earns its place when the columns are genuinely
 * heterogeneous, or when they already exist as objects and would only have to be unpacked.
 * <p>
 * The bulk copies below are where the two hops stop mattering: each forwards the whole range to the
 * row's own copy, so the pair of dispatches is paid once for the range rather than once for every
 * element. That is the only reason a modular arity can be read at speed at all, and it is why the
 * overrides are here rather than left to the interface defaults - the defaults would loop on
 * {@link #getIntFor}, which is exactly the two hops again.
 * <p>
 * Nothing here is multi thread safe.
 * */
public class ACSE_ModularArityCatalog implements ACSE_ArityComponentBase {

/** Which identities exist. The columns are total over it; none of them decides membership. */
public IdentitiesSet occupied;
public ArrayList<ACSE_ArityCatalogRowBase> catalogRows;
/**
 * The highest identity every column has been made to reach, not a count and not the occupied size -
 * an unsigned identity leaves a hole in the set without lowering what the columns address.
 * */
public long lastIndex = -1;
public final ACSE_Journal journal;
final int[] params = new int[2];

static final byte IDENTITY = 1, CATALOG = 2;

public ACSE_ModularArityCatalog(IdentitiesSet occupied, ACSE_Journal journal) {
	this.occupied = occupied;
	this.catalogRows = new ArrayList<>();
	this.journal = journal;
}


@Override
public IdentitiesSet occupied() { return occupied; }

@Override
public void removeIf(int identity, String comment) {
	int request = journal.beginRecordingActivity(this, "removeIf", params, comment);
	if(occupied.unsign(identity)) {
		params[0] = identity;
		journal.recordActivityStep((byte) -IDENTITY, 1);
	}
	complete(request);
}

@Override
public int catalogCount() { return catalogRows.size(); }

@Override
public ACSE_ArityCatalogRowBase getCatalogRow(int catalog) { return catalogRows.get(catalog); }

/**
 * Appends a column and gives back the slot it landed at. The slot is the address from here on -
 * name lookup, if it comes, is for wiring and not for a loop over identities.
 * <p>
 * The new column is grown to the arity's current reach before it is added, so it is addressable for
 * every identity that already exists rather than only for the ones minted after it.
 * */
public int addCatalogRow(ACSE_ArityCatalogRowBase row, String comment) {
	int request = journal.beginRecordingActivity(this, "addCatalogRow", params, comment);
	int catalog = catalogRows.size();
	putCatalogRow(catalog, row);
	params[0] = catalog;
	params[1] = journal.recordObject(row);
	journal.recordActivityStep(CATALOG, 2);
	complete(request);
	return catalog;
}

/** The slot stays empty; later slots do not shift. */
public void removeCatalogRow(int catalog, String comment) {
	ACSE_ArityCatalogRowBase row = catalogRows.get(catalog);
	if(row == null) { throw new IllegalStateException("catalog " + catalog + " is already removed"); }
	int request = journal.beginRecordingActivity(this, "removeCatalogRow", params, comment);
	catalogRows.set(catalog, null);
	params[0] = catalog;
	params[1] = journal.recordObject(row);
	journal.recordActivityStep((byte) -CATALOG, 2);
	complete(request);
}

@Override
public void registerIf(int identity, String comment) {
	int request = journal.beginRecordingActivity(this, "registerIf", params, comment);
	if(identity > lastIndex) { ensureCapacity(identity); }
	if(occupied.register(identity)) {
		params[0] = identity;
		journal.recordActivityStep(IDENTITY, 1);
	}
	complete(request);
}

void putCatalogRow(int catalog, ACSE_ArityCatalogRowBase row) {
	if(lastIndex >= 0) { row.ensureCapacity(lastIndex); }
	if(catalog == catalogRows.size()) { catalogRows.add(row); }
	else { catalogRows.set(catalog, row); }
}

void complete(int request) {
	int completed = journal.completeRecordingActivity();
	if(completed != request) { throw new IllegalStateException("began request " + request + ", completed " + completed); }
}

@Override
public void doTraceableStep(byte stepPair, int[] params, int index) {
	switch(stepPair) {
		case IDENTITY -> occupied.register(params[index]);
		case -IDENTITY -> occupied.unsign(params[index]);
		case CATALOG -> putCatalogRow(params[index], (ACSE_ArityCatalogRowBase) journal.getObject(params[index + 1]));
		case -CATALOG -> catalogRows.set(params[index], null);
		default -> throw new IllegalArgumentException("no step " + stepPair);
	}
}


@Override
public double getEstimatedBytesPerIdentity(int catalog) {
	return catalogRows.get(catalog).getEstimatedBytesPerIdentity();
}

@Override
public int getPreferredType(int catalog) {
	return catalogRows.get(catalog).getPreferredType();
}


/** Every column follows the identity, and the answer is whether any of them had to move. */
@Override
public boolean ensureCapacity(long identity) {
	boolean grown = false;
	for(ACSE_ArityCatalogRowBase row : catalogRows) { if(row != null) { grown |= row.ensureCapacity(identity); } }
	lastIndex = identity;
	return grown;
}

@Override
public void truncate(long capacity) {
	for(ACSE_ArityCatalogRowBase row : catalogRows) { if(row != null) { row.truncate(capacity); } }
	lastIndex = Math.max(capacity, -1);
}


@Override
public int getIntFor(int catalog, long identity) { return catalogRows.get(catalog).getIntFor(identity); }
@Override
public long getLongFor(int catalog, long identity) { return catalogRows.get(catalog).getLongFor(identity); }
@Override
public double getDoubleFor(int catalog, long identity) { return catalogRows.get(catalog).getDoubleFor(identity); }
@Override
public boolean getBooleanFor(int catalog, long identity) { return catalogRows.get(catalog).getBooleanFor(identity); }
@Override
public Object getObjectFor(int catalog, long identity) { return catalogRows.get(catalog).getObjectFor(identity); }


/** One dispatch pair for the range, where the interface default would have paid one per element. */
@Override
public void copyIntsInto(int catalog, long from, long to, int[] dest, int offset) {
	catalogRows.get(catalog).copyIntsInto(from, to, dest, offset);
}

@Override
public void copyLongsInto(int catalog, long from, long to, long[] dest, int offset) {
	catalogRows.get(catalog).copyLongsInto(from, to, dest, offset);
}

@Override
public void copyDoublesInto(int catalog, long from, long to, double[] dest, int offset) {
	catalogRows.get(catalog).copyDoublesInto(from, to, dest, offset);
}

@Override
public void copyBooleansInto(int catalog, long from, long to, boolean[] dest, int offset) {
	catalogRows.get(catalog).copyBooleansInto(from, to, dest, offset);
}


}
