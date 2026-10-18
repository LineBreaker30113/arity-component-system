package org.halim.acs.structure.binary.connector;

import org.halim.acs.structure.ACS_StructureComponentBase;

/**
 * The indexer: a structure over two arities, where an identity of the first is connected to an identity
 * of the second, or not. That is the whole relation - no order, no weight, no count - so one indexing
 * logic has many representations, each fast for a different case, and which one an arity pair gets is
 * the client's choice, not this contract's.
 * <p>
 * The two sides are the <em>primary</em>, the arity at index 0, and the <em>complement</em>, the arity
 * at index 1. They are not symmetric in any implementation: each keeps one direction cheaper than the
 * other, and says which. Both may be the same arity slot, which makes the indexer a directed graph over
 * that arity; forgetting an identity then drops it from both sides.
 * <p>
 * A connection is made only between identities their arities hold, and checked there; breaking one,
 * and every read, take the identities as given. Every change is journaled as the connection it makes or
 * breaks, so undo and redo replay single connections and their order never matters. Identities are ints,
 * as in every structure: a connection costs an int on both ends.
 * */
public interface ACS_ConnectorComponentBase extends ACS_StructureComponentBase {

@Override
default int getArityCount() { return 2; }

@Override
default boolean isRelatedArity(int identity) { return identity == getArityIdentity(0) || identity == getArityIdentity(1); }

default int getPrimaryArity() { return getArityIdentity(0); }
default int getComplementArity() { return getArityIdentity(1); }


boolean isConnected(int primary, int complement);

/** Connects the two. False when they already were; a refusal when either is not an occupied identity of its arity. */
boolean ensureConnection(int primary, int complement, String comment);

/** False when they were not connected. */
boolean breakConnection(int primary, int complement, String comment);


/** How many complements {@code primary} is connected to. */
int complementCount(int primary);

/** How many primaries {@code complement} is connected to. */
int primaryCount(int complement);

/** The complements of {@code primary}, {@link #complementCount} of them, in the order the implementation states. */
void copyComplementsInto(int primary, int[] dest, int offset);

/** The primaries of {@code complement}, {@link #primaryCount} of them, in the order the implementation states. */
void copyPrimariesInto(int complement, int[] dest, int offset);


}
