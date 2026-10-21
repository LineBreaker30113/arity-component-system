package org.halim.acs.structure.ternary.of_by_to;

import org.halim.acs.structure.ACS_StructureComponentBase;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * A structure over three arities: each identity of the <em>by</em> arity, at index 1, takes a list of
 * identities of the <em>of</em> arity, at index 0, and gives a list of identities of the <em>to</em> arity,
 * at index 2 - a transition, whose own data lives in its arity. In meaning it is two connectors sharing
 * the by arity as their primaries, an order higher than a connector as node-edge is than the unary graph:
 * the connections matter, so each is an identity. The three are different arity slots, refused at build
 * otherwise.
 * <p>
 * The contract is the most formal case, and a storage optimizes for the kinds it takes: 0 is unordered lists
 * with repeats, any number of bys on any lists, and each restriction narrows that - the bits of one int,
 * given when it is built and fixed from then on. A storage takes the sets it can keep and refuses the
 * others when built, and a request that would break a restriction is refused. The cartesian product of the
 * of and the to arities is {@link #AT_MOST_ONE_OF}, {@link #AT_MOST_ONE_TO}, {@link #EVERY_BY_TAKES},
 * {@link #EVERY_BY_GIVES} and {@link #NO_PARALLEL_BYS}, a by for each pair there is; {@link #FULL} adds
 * every pair.
 * <p>
 * A by is any identity its arity holds: the takings work on bys captured elsewhere, and {@link #ensureBy}
 * captures a new one through the core, as a request nested in its own, so undoing it removes the by. A by
 * leaving its arity is forgotten here; an of or a to leaving its arity is dropped from every list, its gap
 * closed, and the by stays. Every change is journaled as the taking it adds or removes, with the place it
 * does so at under {@link #ORDERED}. Identities are ints, as in every structure.
 * */
public interface ACS_OfByToComponentBase extends ACS_StructureComponentBase {

/**
 * A by's lists keep an order: an element added is placed after the ones there and a removal closes its gap.
 * Without it they carry none, a storage may keep them as sets, and the reads give them in the order the
 * implementation states.
 * */
int ORDERED = 1;

/** An element is in a by's list once at most. Without it, a by may take the same of twice, as f(a, a), each time a taking of its own. */
int NO_REPEATS = 1 << 1;

/** No two bys take the same list and give the same list. */
int NO_PARALLEL_BYS = 1 << 2;

/** A by takes one of at most. */
int AT_MOST_ONE_OF = 1 << 3;

/** A by gives one to at most. */
int AT_MOST_ONE_TO = 1 << 4;

/**
 * Every by takes an of. A by counts from its first taking, since a new identity has none, and forget,
 * which no structure refuses, can still break it.
 * */
int EVERY_BY_TAKES = 1 << 5;

/** Every by gives a to, counted as {@link #EVERY_BY_TAKES} counts. */
int EVERY_BY_GIVES = 1 << 6;

/** An of is taken by one by at most. */
int ONE_BY_AN_OF = 1 << 7;

/** A to is given by one by at most. */
int ONE_BY_A_TO = 1 << 8;

/**
 * Every (of, to) pair has its by. Bys added one at a time cannot keep it, since a new of needs a by for
 * every to at once: it holds where the by arity is one the structure causes, which its creation
 * configures.
 * */
int FULL = 1 << 9;

/** The restrictions it was built with, an OR of the constants above. */
int getRestrictions();

@Override
default int getArityCount() { return 3; }

@Override
default boolean isRelatedArity(int identity) { return identity == getArityIdentity(0) || identity == getArityIdentity(1) || identity == getArityIdentity(2); }

default int getOfArity() { return getArityIdentity(0); }
default int getByArity() { return getArityIdentity(1); }
default int getToArity() { return getArityIdentity(2); }


/**
 * A by taking {@code ofs} and giving {@code tos}, in their order, given back: a new one captured on the by
 * arity, or, with {@link #NO_PARALLEL_BYS}, the one already there. Refuses an element its arity does not
 * hold, and lists a restriction forbids.
 * */
int ensureBy(int[] ofs, int[] tos, String comment);

/**
 * Adds {@code of} to what {@code by} takes, placed last under {@link #ORDERED}. False when, with
 * {@link #NO_REPEATS}, it already was; refuses an identity its arity does not hold, and a taking a
 * restriction forbids.
 * */
boolean addOf(int by, int of, String comment);

/** Removes {@code of} from what {@code by} takes, every time it is there, closing the gaps. False when it was not; refuses a removal a restriction forbids. */
boolean removeOf(int by, int of, String comment);

/**
 * Replaces what {@code by} takes with {@code ofs}, in their order, as one change: refused for what the new
 * list breaks, never for what breaks only midway. False when nothing changes.
 * */
boolean setOfs(int by, int[] ofs, String comment);

/** As {@link #addOf}, for what {@code by} gives. */
boolean addTo(int by, int to, String comment);

/** As {@link #removeOf}, for what {@code by} gives. */
boolean removeTo(int by, int to, String comment);

/** As {@link #setOfs}, for what {@code by} gives. */
boolean setTos(int by, int[] tos, String comment);


/** How many takings {@code by} has, a repeated of counting each time. */
int ofCount(int by);

/** What {@code by} takes, {@link #ofCount} of them: with {@link #ORDERED} in their order, otherwise in the order the implementation states. */
void copyOfsInto(int by, int[] dest, int offset);

/** How many tos {@code by} gives, a repeated one counting each time. */
int toCount(int by);

/** What {@code by} gives, {@link #toCount} of them, ordered as {@link #copyOfsInto} orders. */
void copyTosInto(int by, int[] dest, int offset);

/** How many bys take {@code of}, each once. */
int takerCount(int of);

/** The bys taking {@code of}, {@link #takerCount} of them, ascending. */
void copyTakersInto(int of, int[] dest, int offset);

/** How many bys give {@code to}, each once. */
int giverCount(int to);

/** The bys giving {@code to}, {@link #giverCount} of them, ascending. */
void copyGiversInto(int to, int[] dest, int offset);


/** The tos of every by taking {@code of}, each once and ascending, written as an int[] is: its length, then its elements. */
void writeTosOf(int of, DataOutputStream dos) throws IOException;

/** The ofs of every by giving {@code to}, each once and ascending, written as {@link #writeTosOf} writes. */
void writeOfsOf(int to, DataOutputStream dos) throws IOException;


}
