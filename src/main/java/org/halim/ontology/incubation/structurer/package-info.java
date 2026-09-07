/**
 * Building structures over sets of identities. An arity is a set of identities plus the data hanging
 * off them, and this package is where the two halves meet.
 * <p>
 * The shape is an archetype table: identities are rows, catalogs are columns, and a column is stored
 * whole rather than interleaved with its neighbours. That is the layout the README calls a structure
 * of arrays, and it is the same one every engine that stores entities this way converges on. What
 * this package is trying to pass is the arity ceiling those engines stop at - a relationship there
 * is a pair, two halves of one 64-bit word, and two participants is a packing decision rather than a
 * semantic one.
 * <p>
 * <b>Identities.</b> Bare longs, minted densely from 0 within one arity, never negative. Uniqueness
 * is all they carry: no meaning, no ordering beyond ascending, no structure. Because they are dense
 * and local to an arity, their <em>values</em> stay small, which is what makes the byte- and
 * short-wide representations in {@code sets} carry real arities rather than toys. That premise ends
 * the moment identities become global across arities, or carry a generation counter for safe reuse -
 * either one pushes values up fast, and both are decisions to take deliberately rather than
 * discover.
 * <p>
 * <b>Nothing narrows silently.</b> A wrapped identity is indistinguishable from a real one, so every
 * narrowing that could wrap throws instead - which is also the signal to promote to a wider
 * representation. Floating values are the deliberate exception: narrowing a double to a float rounds
 * rather than throwing, because unlike a truncated identity a rounded measurement is still the value
 * it claims to be.
 * <p>
 * <b>Partiality lives in exactly one place.</b> A catalog is total: it answers for every identity,
 * and for a stored one a slot never written reads as zero rather than as absent. Whether an identity
 * exists at all is the occupied set's question, and only its question. Optional-shaped data is the
 * client's responsibility for now; when it is taken up, the shape that stays consistent with this is
 * a presence set beside the column rather than nullable values, since that keeps boxing out of the
 * primitive path.
 * <p>
 * <b>Slots, not names.</b> A catalog is addressed by an int resolved from its name once. Name lookup
 * is for wiring and debugging. An engine whose premise is that every identity is a bare int has no
 * business comparing strings inside a loop over identities.
 * <p>
 * <b>Take the concrete type before looping.</b> Reading values through a common interface is a
 * virtual call the JIT cannot inline once several implementations are loaded, and that cost is paid
 * per element - so it grows with the identity count instead of being amortised by it. The interfaces
 * here exist for uniform lifecycle and for generic code; bulk work resolves the concrete catalog or
 * table first and then loops on something the compiler can see through.
 * <p>
 * <b>What is settled here and what is not.</b> The rules above are decisions, not sketches - they
 * hold across the package and the code below already keeps them. The module type that owns an arity
 * is being replaced, so the class currently named {@code ArityModule} is a staging area rather than
 * the shape being aimed at; read it as work in flight and not as the contract.
 * <p>
 * {@link org.halim.ontology.incubation.structurer.ArityCatalogBase} is in progress too. It is the table
 * form of the catalog contract - one object holding every column of an arity, a cell addressed by
 * {@code (catalog, identity)} - so that a client who knows their arity can write one class over a
 * few flat arrays and skip the per-column machinery entirely. Catalogs stay interfaces rather than
 * classes precisely so a column need not be stored at all: a value that is a function of the
 * identity costs no memory, grows for free, and answers the same contract.
 * */
package org.halim.ontology.incubation.structurer;
