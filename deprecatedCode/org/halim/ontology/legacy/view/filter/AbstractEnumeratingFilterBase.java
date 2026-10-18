package org.halim.ontology.legacy.view.filter;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.halim.ontology.legacy.core.segmentation.query.CustomOntologyFilterBase;

/**
 * Base class for a filter whose cheap direction is producing its whole matching set. A subclass
 * answers exactly one question — <em>which classes, or which elements, satisfy me out of all of them</em> — and this class
 * supplies the other direction.
 * <p>
 * Narrowing that answer to some smaller candidate set is never a subclass's job: {@link #enumerate()}
 * always returns the complete domain and the engine intersects. A subclass that had to honour a
 * caller-supplied candidate set could silently ignore it and over-match, with nothing to catch it.
 * <p>
 * {@link #test(int)} is derived here: {@link #enumerate()} runs once, lazily, and every later call is a
 * membership check against the cached result. The check is deliberately a plain linear scan rather than
 * a {@code BitSet} or hash set — matched sets rarely exceed a few dozen entries, which is not worth the
 * developer effort or the user's memory of handling small and large sets differently.
 * <p>
 * <b>The cache is a plain, unsynchronized field.</b> Build filter instances fresh per query. It is not
 * invalidated by engine mutations, so an instance reused across a mutation keeps answering from the
 * domain it computed the first time; {@link #invalidateCache()} is the supported way out for a subclass
 * that deliberately reuses an instance.
 * <p>
 * Extending this class is usually preferable to implementing {@link CustomOntologyFilterBase}
 * directly, which supplies no memoization. See that interface for the concurrency contract every custom
 * filter runs under.
 */
public abstract class AbstractEnumeratingFilterBase implements CustomOntologyFilterBase {

	private IntArrayList cachedDomain;

	/** Everything satisfying this filter, out of all of its kind. Called at most once per cache lifetime. */
	@Override
	public abstract IntArrayList enumerate();

	@Override
	public final boolean isEnumeratePreferred() { return true; }

	@Override
	public final boolean test(int candidateIdentity) {
		if (cachedDomain == null) cachedDomain = enumerate();
		return cachedDomain.contains(candidateIdentity);
	}

	/** Drops the memoized domain, so the next {@link #test(int)} recomputes it. Call when reusing this
	 *  instance after the data {@link #enumerate()} reads has changed. */
	protected final void invalidateCache() { cachedDomain = null; }

}
