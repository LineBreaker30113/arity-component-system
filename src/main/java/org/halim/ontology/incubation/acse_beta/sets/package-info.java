/**
 * Which identities exist. One contract,
 * {@link org.halim.ontology.incubation.acse_beta.sets.IdentitiesSet}, over several representations
 * that differ only in what they physically store - so a set can be swapped for a cheaper form as its
 * density or its size changes, and nothing above notices.
 * <p>
 * Two families of operation live on the contract. {@code identityAt} and {@code identityIteritor}
 * are the <em>list</em> view: positions, ascending, which is what a navigation UI pages through. The
 * rest are the <em>set</em> algebra, which is what closure over a formal context is built from -
 * computing a concept is nothing but repeated intersection, so a set without them cannot take part
 * in one. Everything past {@code contains} is a default written once against {@code contains} and
 * {@code identityAt}, so a new representation gains the whole algebra by supplying those two, and
 * overrides only where it can genuinely do better.
 * <p>
 * <b>The representations, and when each wins.</b>
 * <ul>
 *   <li><b>Negative</b> - the range {@code [firstIndex, lastIndex]} minus the holes punched in it.
 *       Memory is proportional to the holes, not the identities, so a dense arity of a billion costs
 *       two bounds and its tombstones. Sparsity is what it cannot afford: one identity far outside
 *       the range widens it and turns every integer swallowed on the way into a hole.</li>
 *   <li><b>Positive</b> - the identities themselves, sorted ascending. Memory is proportional to how
 *       many there are and not to the span they cover, which makes it the form for small or
 *       scattered sets. Below {@code LINEAR_SCAN_LIMIT} membership walks instead of searching: a
 *       handful of values is a cache line or two, and a predictable walk beats a branchy search
 *       inside it.</li>
 *   <li><b>Roaring</b> - a compressed bitmap, and the only one whose algebra works on whole
 *       containers rather than element by element. Its cost is the other side: a bitmap has no
 *       positions, so {@code identityAt} is a search where the listers answer in one step.</li>
 * </ul>
 * The algebra is correct across every pairing of these. A representation takes its fast path only
 * when both sides share it, and falls back to the generic walk otherwise, so mixing is never wrong -
 * only slower.
 * <p>
 * <b>Storage width bounds the identity values, not how many there are.</b> A byte-wide set holds
 * forty identities under 256 comfortably and two identities at a million not at all. Storage is
 * unsigned throughout, because identities start at 0 and never go negative, so spending the sign bit
 * would cost half the range for a case that cannot occur. A value that will not fit is refused
 * rather than wrapped, and that refusal is the signal to promote to the next width.
 * <p>
 * <b>Sizes are long, the int views are not.</b> {@code size()} returns a long because a negative
 * form holds a dense range in its bounds and a set of 2^40 identities is three fields wide. The
 * int-shaped snapshots - {@code getIntList}, {@code toList} - are the ones with a ceiling, and they
 * throw rather than truncate when an identity outgrows it. They are kept because many sets are small
 * and an int list is the fastest thing to query.
 * <p>
 * The storage is the set: each class implements the contract itself rather than handing out a
 * separate view, so there is one object per set and nothing to unwrap. None of them is multi thread
 * safe.
 * */
package org.halim.ontology.incubation.acse_beta.sets;
