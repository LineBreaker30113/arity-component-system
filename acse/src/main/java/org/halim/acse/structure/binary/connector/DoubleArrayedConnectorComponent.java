package org.halim.acse.structure.binary.connector;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.halim.acs.ACS_Core;
import org.halim.acs.ACS_Journal;
import org.halim.acs.structure.binary.connector.ACS_ConnectorComponentBase;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;


/**
 * The relation kept twice, as arrays in both directions, so both directions read without a scan: each
 * primary holds a list of its complements, and each complement a list of its primaries, each in the order
 * they were connected. The legacy {@code DoubleArrayedIndexer} kept a bit set on one side; here both sides
 * are the same shape.
 * <p>
 * A connection costs an int on each side and an identity without connections costs nothing, so the
 * relation's size is its connection count, whatever the identities' range. What it pays for that is the
 * search: {@link #isConnected} searches the shorter of the two lists, and breaking a connection searches
 * both from the end, so the shape this is for is an identity with a few dozen connections, on either side.
 * <p>
 * The legacy tombstone stacks are gone: which identities exist, and which come back, is the arities'
 * identity sets' business, and an identity leaving its arity reaches this through {@link #forget}. Both
 * sides grow when a connection first needs an identity, never from the arity. Not multi thread safe.
 * */
public class DoubleArrayedConnectorComponent implements ACS_ConnectorComponentBase {

/** The connection (primary, complement); the negative pair breaks it. */
static final byte CONNECT = 1;

final ACS_Core core;
final ACS_Journal journal;
/** The slots of the arities at index 0 and 1; they may be the same slot. */
final int primaryArity, complementArity;
final int[] params = new int[2];

/** Per primary, its complements in the order connected; null until it has had one. */
final ArrayList<IntArrayList> complements = new ArrayList<>();
/** Per complement, its primaries in the order connected; null until it has had one. */
final ArrayList<IntArrayList> primaries = new ArrayList<>();


public DoubleArrayedConnectorComponent(ACS_Core core, ACS_Journal journal, int primaryArity, int complementArity) {
	this.core = core;
	this.journal = journal;
	this.primaryArity = primaryArity;
	this.complementArity = complementArity;
}


@Override
public int getArityIdentity(int index) {
	return switch(index) {
		case 0 -> primaryArity;
		case 1 -> complementArity;
		default -> throw new IndexOutOfBoundsException("arity " + index + " of an indexer over two");
	};
}


/** A search of the shorter of the two lists. */
@Override
public boolean isConnected(int primary, int complement) {
	IntArrayList ofPrimary = complementsOf(primary), ofComplement = primariesOf(complement);
	if(ofPrimary == null || ofComplement == null) { return false; }
	return ofPrimary.size() <= ofComplement.size() ? ofPrimary.contains(complement) : ofComplement.contains(primary);
}

@Override
public int complementCount(int primary) {
	IntArrayList held = complementsOf(primary);
	return held == null ? 0 : held.size();
}

@Override
public int primaryCount(int complement) {
	IntArrayList held = primariesOf(complement);
	return held == null ? 0 : held.size();
}

/** In the order they were connected. */
@Override
public void copyComplementsInto(int primary, int[] dest, int offset) {
	IntArrayList held = complementsOf(primary);
	if(held != null) { held.getElements(0, dest, offset, held.size()); }
}

/** In the order they were connected. */
@Override
public void copyPrimariesInto(int complement, int[] dest, int offset) {
	IntArrayList held = primariesOf(complement);
	if(held != null) { held.getElements(0, dest, offset, held.size()); }
}


@Override
public boolean ensureConnection(int primary, int complement, String comment) {
	requireLive(primaryArity, primary);
	requireLive(complementArity, complement);
	if(isConnected(primary, complement)) { return false; }
	int request = journal.beginRecordingActivity(this, "ensureConnection", params, comment);
	connect(primary, complement);
	complete(request);
	return true;
}

@Override
public boolean breakConnection(int primary, int complement, String comment) {
	if(!isConnected(primary, complement)) { return false; }
	int request = journal.beginRecordingActivity(this, "breakConnection", params, comment);
	disconnect(primary, complement);
	complete(request);
	return true;
}

/**
 * Breaks every connection the identity has on the side or sides its arity is. Its list is emptied from
 * the end, so each break finds it there at once. Undo restores the connections, not the lists' order: the
 * journal records relations only.
 * */
@Override
public boolean forget(int arity, int identity) {
	boolean asPrimary = arity == primaryArity && complementCount(identity) > 0;
	boolean asComplement = arity == complementArity && primaryCount(identity) > 0;
	if(!asPrimary && !asComplement) { return false; }
	int request = journal.beginRecordingActivity(this, "forget", params, null);
	if(asPrimary) {
		IntArrayList held = complementsOf(identity);
		while(!held.isEmpty()) { disconnect(identity, held.getInt(held.size() - 1)); }
	}
	// Over one arity, the identity's connection to itself is already broken by now, and off this list too.
	if(asComplement) {
		IntArrayList held = primariesOf(identity);
		while(!held.isEmpty()) { disconnect(held.getInt(held.size() - 1), identity); }
	}
	complete(request);
	return true;
}


/** Connects and records it. */
void connect(int primary, int complement) {
	connectUnchecked(primary, complement);
	params[0] = primary;
	params[1] = complement;
	journal.recordActivityStep(CONNECT, 2);
}

void disconnect(int primary, int complement) {
	disconnectUnchecked(primary, complement);
	params[0] = primary;
	params[1] = complement;
	journal.recordActivityStep((byte) -CONNECT, 2);
}

@Override
public void doTraceableStep(byte stepPair, int[] params, int index) {
	switch(stepPair) {
		case CONNECT -> connectUnchecked(params[index], params[index + 1]);
		case -CONNECT -> disconnectUnchecked(params[index], params[index + 1]);
		default -> throw new IllegalArgumentException("no step " + stepPair);
	}
}

/** The connection with no check; the two must not be connected yet. Grows either side to reach it. */
void connectUnchecked(int primary, int complement) {
	while(complements.size() <= primary) { complements.add(null); }
	while(primaries.size() <= complement) { primaries.add(null); }
	if(complements.get(primary) == null) { complements.set(primary, new IntArrayList()); }
	if(primaries.get(complement) == null) { primaries.set(complement, new IntArrayList()); }
	complements.get(primary).add(complement);
	primaries.get(complement).add(primary);
}

/** The two must be connected. */
void disconnectUnchecked(int primary, int complement) {
	IntArrayList ofPrimary = complements.get(primary), ofComplement = primaries.get(complement);
	ofPrimary.removeInt(ofPrimary.lastIndexOf(complement));
	ofComplement.removeInt(ofComplement.lastIndexOf(primary));
}


/** The two arity slots, then the connections as (primary, complement) pairs, primary by primary, so the file carries the relation and not this encoding. */
@Override
public boolean saveTo(DataOutputStream dos) throws IOException {
	dos.writeInt(primaryArity);
	dos.writeInt(complementArity);
	long count = 0;
	for(IntArrayList held : complements) { if(held != null) { count += held.size(); } }
	dos.writeLong(count);
	for(int primary = 0; primary < complements.size(); primary++) {
		IntArrayList held = complements.get(primary);
		if(held == null) { continue; }
		for(int i = 0; i < held.size(); i++) {
			dos.writeInt(primary);
			dos.writeInt(held.getInt(i));
		}
	}
	return true;
}

/** Rebuilds what {@link #saveTo} wrote, attached to the core, outside the journal; refuses a connection written twice. */
public static DoubleArrayedConnectorComponent loadFrom(ACS_Core core, ACS_Journal journal, DataInputStream dis) throws IOException {
	int primaryArity = dis.readInt();
	DoubleArrayedConnectorComponent indexer = new DoubleArrayedConnectorComponent(core, journal, primaryArity, dis.readInt());
	for(long i = 0, count = dis.readLong(); i < count; i++) {
		int primary = dis.readInt();
		int complement = dis.readInt();
		if(primary < 0 || complement < 0) { throw new IOException("connection (" + primary + ", " + complement + ") has a negative identity"); }
		if(indexer.isConnected(primary, complement)) { throw new IOException("connection (" + primary + ", " + complement + ") is written twice"); }
		indexer.connectUnchecked(primary, complement);
	}
	return indexer;
}


IntArrayList complementsOf(int primary) { return primary >= 0 && primary < complements.size() ? complements.get(primary) : null; }
IntArrayList primariesOf(int complement) { return complement >= 0 && complement < primaries.size() ? primaries.get(complement) : null; }

void requireLive(int arity, int identity) {
	if(identity < 0 || !core.getArity(arity).occupied().contains(identity)) {
		throw new IllegalArgumentException(identity + " is not an occupied identity of arity " + arity);
	}
}

void complete(int request) {
	int completed = journal.completeRecordingActivity();
	if(completed != request) { throw new IllegalStateException("began request " + request + ", completed " + completed); }
}


}
