package org.halim.ontology.core.segmentation.query;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * The engine's extension point for filtering logic {@code core} cannot express on its own — the
 * query-side counterpart to {@link AnyCatalogBase}'s role for storage. A client plugs its own
 * structure-of-arrays store into a catalog through {@code AnyCatalogBase}; it plugs its own filtering
 * rule over that same client-defined data in through here, and {@code core} only ever calls through
 * this contract. Wrapped into a filter tree by {@link OntologyClassFilter.Custom} or
 * {@link OntologyElementFilter.Custom}.
 * <p>
 * One contract serves both filter kinds, exactly as {@link AnyCatalogBase} serves both catalogs. A
 * filter is a predicate over identities, and a class identity and an element identity are the same
 * shape, so splitting this in two would only mean maintaining two byte-identical files. The trade-off
 * is real and worth knowing: nothing stops a filter written against element data from being wrapped in
 * an {@link OntologyClassFilter.Custom}, where it would silently judge class identities instead. Name
 * implementations for the kind they are written against.
 * <p>
 * A client provides exactly two things: <em>which direction it prefers</em>, and that direction's
 * implementation. Everything else — deriving the other direction, choosing where a resolution anchors,
 * and deciding whether a full scan is needed at all — is the engine's responsibility and never appears
 * in this contract.
 *
 * <h2>The two directions</h2>
 * Some filters are cheaper handing back everything that satisfies them; others are cheaper judging one
 * candidate at a time. Neither wins universally, so both are supported:
 * <ul>
 *   <li><b>test</b> — decide whether one candidate matches. Every filter can do this, so it is the one
 *       abstract method, which also makes this a functional interface: a test-preferring filter can be
 *       written as a lambda, {@code new OntologyElementFilter.Custom(id -> ...)}.</li>
 *   <li><b>enumerate</b> — produce the complete matching set, out of <em>everything</em> of that kind.
 *       Declared by overriding {@link #isEnumeratePreferred()} to {@code true} and implementing
 *       {@link #enumerate()}.</li>
 * </ul>
 * Only declare enumerate-preference when producing the whole set really is the cheap direction (an
 * index, a precomputed set, a direct engine query). A filter that would have to walk everything to
 * answer {@link #enumerate()} should stay test-preferring and let the engine decide when — if ever —
 * to walk the universe on its behalf. An implementation that declares nothing is treated as
 * test-preferring, which is always correct and at worst slower.
 * <p>
 * Narrowing is deliberately absent from this contract. An implementation is never handed a candidate
 * set to honour and never has to intersect against one: {@link #enumerate()} always returns this
 * filter's <em>complete</em> domain, and the engine intersects. Nor is a test-preferring filter ever
 * asked to produce a set — including for the "test against everything" case, where the engine supplies
 * the universe itself.
 * <p>
 * {@code org.halim.ontology.view.filter} offers ready-made base classes
 * ({@code AbstractTestingFilterBase}, {@code AbstractEnumeratingFilterBase}) that are usually easier to
 * extend than implementing this interface directly — notably, the enumerating one derives a memoized
 * {@link #test(int)} for you.
 *
 * <h2>Concurrency contract</h2>
 * An implementation runs under the engine's global <em>read</em> lock, held for the entire resolution.
 * It must never call back into anything requiring the engine's write lock:
 * {@code ReentrantReadWriteLock} cannot upgrade read to write on the same thread, so doing so
 * deadlocks unconditionally, not merely slowly. It should also avoid slow or I/O-bound work, since the
 * held read lock blocks every writer for its duration.
 * <p>
 * Build filter instances fresh per query. Any caching an implementation does — including the caching
 * {@code AbstractEnumeratingFilterBase} does on its behalf — is unsynchronized and is not invalidated
 * by engine mutations.
 */
@FunctionalInterface
public interface CustomOntologyFilterBase {

	/** Whether one specific class or element satisfies this filter. */
	boolean test(int candidateIdentity);

	/**
	 * Whether {@link #enumerate()} is this filter's cheap direction. Defaults to {@code false}: testing
	 * is the assumed direction and an implementation has to prove the opposite. An implementation that
	 * returns {@code true} must implement {@link #enumerate()}.
	 */
	default boolean isEnumeratePreferred() { return false; }

	/**
	 * Everything satisfying this filter, out of every class or element of the kind it is used for —
	 * this filter's complete domain, never narrowed to some candidate set. Only ever called when
	 * {@link #isEnumeratePreferred()} returns {@code true}; the default exists so a test-preferring
	 * implementation does not have to write one.
	 */
	default IntArrayList enumerate() {
		throw new UnsupportedOperationException(
			  "Filter " + getClass().getName() + " prefers testing but was asked to enumerate; "
					+ "override isEnumeratePreferred() to return true only alongside a real enumerate().");
	}

}
