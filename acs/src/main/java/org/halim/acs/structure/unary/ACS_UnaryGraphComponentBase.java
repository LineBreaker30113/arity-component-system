package org.halim.acs.structure.unary;

import org.halim.acs.structure.ACS_GraphComponentBase;

/**
 * A graph over one arity: the edges alone, between that arity's identities. An edge carrying data is an
 * entity, an identity of an arity of its own, which makes it a node-edge structure over two arities. For
 * the same reason there are no parallel edges here - two elements are joined, one way round, or not.
 * <p>
 * Directed at 0: {@link #SYMMETRIC} makes every edge go both ways. {@link #NO_IMPLIED_EDGES} is kept rather
 * than refused: the edges a new one implies are dropped, which
 * loses nothing, since an edge here is only the relation.
 * */
public interface ACS_UnaryGraphComponentBase extends ACS_GraphComponentBase {

/** Every edge goes both ways, so an element's parents are its children: an edge and its reverse are made and broken together. */
int SYMMETRIC = 1 << 16;

@Override
default boolean isDirected() { return (getRestrictions() & SYMMETRIC) == 0; }

@Override
default int getArityCount() { return 1; }

@Override
default boolean isRelatedArity(int identity) { return identity == getArityIdentity(0); }


}
