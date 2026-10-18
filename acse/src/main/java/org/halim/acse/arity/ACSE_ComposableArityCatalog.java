package org.halim.acse.arity;

import org.halim.acs.ACS_Core;
import org.halim.acs.ACS_Journal;
import org.halim.acs.ACS_PersistenceManager;
import org.halim.acs.arity.ACS_ArityComponentBase;
import org.halim.acs.arity.catalog.ACS_ArityCatalogRowBase;
import org.halim.acs.arity.sets.IdentitiesSet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;


/**
 * The arity whose columns really are objects: a list of {@link ACS_ArityCatalogRowBase}, addressed by the
 * slot they sit at. That is what composable means here: the arity is put together from catalog rows,
 * each its own object, added and removed one at a time through the journal, rather than written as
 * one class with its columns as fields. This is the general case, the one that takes any storage form
 * the catalog package offers and mixes widths and types freely in one arity.
 * <p>
 * What it costs is the thing {@link ACS_ArityComponentBase} exists to let a client avoid. Every read is two
 * hops - table to row, row to storage - and the second is an interface call at a site that
 * sees every catalog implementation the program loaded, so it stops inlining early. A client whose
 * arity is three int columns pays that for nothing; they should implement {@code ACS_ArityComponentBase}
 * directly over three arrays and skip this class. It earns its place when the columns are genuinely
 * heterogeneous, or when they already exist as objects and would only have to be unpacked.
 * <p>
 * The bulk copies below are where the two hops stop mattering: each forwards the whole range to the
 * row's own copy, so the pair of dispatches is paid once for the range rather than once for every
 * element. That is the only reason a composable arity can be read at speed at all, and it is why the
 * overrides are here rather than left to the interface defaults - the defaults would loop on
 * {@link #getIntFor}, which is exactly the two hops again.
 * <p>
 * Nothing here is multi thread safe.
 * */
public class ACSE_ComposableArityCatalog implements ACS_ArityComponentBase {

/** Which identities exist. The columns are total over it; none of them decides membership. */
public IdentitiesSet occupied;
public ArrayList<ACS_ArityCatalogRowBase> catalogRows;
/**
 * The highest identity every column has been made to reach, not a count and not the occupied size -
 * an unsigned identity leaves a hole in the set without lowering what the columns address.
 * */
public long lastIndex = -1;
public final ACS_Journal journal;
final int[] params = new int[5];

static final byte IDENTITY = 1, CATALOG = 2, WRITE = 3;

public ACSE_ComposableArityCatalog(IdentitiesSet occupied, ACS_Journal journal) {
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
public ACS_ArityCatalogRowBase getCatalogRow(int catalog) { return catalogRows.get(catalog); }

/**
 * Appends a column and gives back the slot it landed at. The slot is the address from here on -
 * name lookup, if it comes, is for wiring and not for a loop over identities.
 * <p>
 * The new column is grown to the arity's current reach before it is added, so it is addressable for
 * every identity that already exists rather than only for the ones minted after it.
 * */
public int addCatalogRow(ACS_ArityCatalogRowBase row, String comment) {
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
	ACS_ArityCatalogRowBase row = catalogRows.get(catalog);
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

/**
 * The checked request around a column's unjournaled write: refused before anything is recorded when
 * the column holds no numbers or the identity is past the arity's reach, then recorded as the
 * difference of the stored bits, which undo and redo both apply as it is. A write that changed
 * nothing records no step.
 * */
@Override
public void setLongFor(int catalog, long identity, long value, String comment) {
	ACS_ArityCatalogRowBase row = writable(catalog, identity, "a written long");
	long old = bitsOf(row, identity);
	int request = journal.beginRecordingActivity(this, "setLongFor", params, comment);
	try { row.setLongFor(identity, value); }
	catch(RuntimeException | Error e) { journal.abortRecordingActivity(); throw e; }
	recordWrite(catalog, identity, old ^ bitsOf(row, identity));
	complete(request);
}

@Override
public void setDoubleFor(int catalog, long identity, double value, String comment) {
	ACS_ArityCatalogRowBase row = writable(catalog, identity, "a written double");
	long old = bitsOf(row, identity);
	int request = journal.beginRecordingActivity(this, "setDoubleFor", params, comment);
	try { row.setDoubleFor(identity, value); }
	catch(RuntimeException | Error e) { journal.abortRecordingActivity(); throw e; }
	recordWrite(catalog, identity, old ^ bitsOf(row, identity));
	complete(request);
}

/** The column, checked before anything is recorded: present, numeric, and reaching the identity. */
ACS_ArityCatalogRowBase writable(int catalog, long identity, String asked) {
	ACS_ArityCatalogRowBase row = catalogRows.get(catalog);
	if(row == null) { throw new IllegalStateException("catalog " + catalog + " is removed"); }
	int type = row.getPreferredType();
	if(type != PREFERRED_INTEGER && type != PREFERRED_LONGI && type != PREFERRED_FLOATING) { throw cannotAnswer(catalog, asked); }
	Objects.checkIndex(identity, lastIndex + 1);
	return row;
}

/** [catalog, identity, difference], the two longs as high and low ints. */
void recordWrite(int catalog, long identity, long difference) {
	if(difference == 0) { return; }
	params[0] = catalog;
	params[1] = (int) (identity >>> 32);
	params[2] = (int) identity;
	params[3] = (int) (difference >>> 32);
	params[4] = (int) difference;
	journal.recordActivityStep(WRITE, 5);
}

/** The stored value as bits: a double's raw bits on a floating column, so a NaN or a -0.0 survives; the value itself on an integral one. */
static long bitsOf(ACS_ArityCatalogRowBase row, long identity) {
	return row.getPreferredType() == PREFERRED_FLOATING ? Double.doubleToRawLongBits(row.getDoubleFor(identity)) : row.getLongFor(identity);
}

/** Applies a recorded difference. One step serves undo and redo alike, since {@code bits ^ d ^ d} is {@code bits}. */
void flip(int catalog, long identity, long difference) {
	ACS_ArityCatalogRowBase row = catalogRows.get(catalog);
	long bits = bitsOf(row, identity) ^ difference;
	if(row.getPreferredType() == PREFERRED_FLOATING) { row.setDoubleFor(identity, Double.longBitsToDouble(bits)); }
	else { row.setLongFor(identity, bits); }
}

static long join(int high, int low) { return (long) high << 32 | Integer.toUnsignedLong(low); }

void putCatalogRow(int catalog, ACS_ArityCatalogRowBase row) {
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
		case CATALOG -> putCatalogRow(params[index], (ACS_ArityCatalogRowBase) journal.getObject(params[index + 1]));
		case -CATALOG -> catalogRows.set(params[index], null);
		case WRITE, -WRITE -> flip(params[index], join(params[index + 1], params[index + 2]), join(params[index + 3], params[index + 4]));
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
	for(ACS_ArityCatalogRowBase row : catalogRows) { if(row != null) { grown |= row.ensureCapacity(identity); } }
	lastIndex = identity;
	return grown;
}

@Override
public void truncate(long capacity) {
	for(ACS_ArityCatalogRowBase row : catalogRows) { if(row != null) { row.truncate(capacity); } }
	lastIndex = Math.max(capacity, -1);
}


/** The reach, the identity set by class and data, then each column the same way, a removed or left-out one as no class. */
@Override
public boolean saveTo(DataOutputStream dos) throws IOException {
	dos.writeLong(lastIndex);
	if(!ACS_PersistenceManager.writeClass(dos, occupied)) {
		throw new IOException(occupied.getClass().getName() + " declares no public static loadFrom, and an arity cannot be stored without its identity set");
	}
	if(!occupied.saveTo(dos)) { return false; }
	dos.writeInt(catalogRows.size());
	for(ACS_ArityCatalogRowBase row : catalogRows) {
		if(ACS_PersistenceManager.writeClass(dos, row) && !row.saveTo(dos)) { return false; }
	}
	return true;
}

/** Rebuilds what {@link #saveTo} wrote, each part through its own class. */
public static ACSE_ComposableArityCatalog loadFrom(ACS_Core core, ACS_Journal journal, DataInputStream dis) throws IOException {
	long lastIndex = dis.readLong();
	ACSE_ComposableArityCatalog arity = new ACSE_ComposableArityCatalog(ACS_PersistenceManager.readPart(dis, IdentitiesSet.class, core, journal), journal);
	arity.lastIndex = lastIndex;
	for(int catalog = 0, count = dis.readInt(); catalog < count; catalog++) {
		arity.catalogRows.add(ACS_PersistenceManager.readPart(dis, ACS_ArityCatalogRowBase.class, core, journal));
	}
	return arity;
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
