/**
 * Ready-made base classes for the engine's filtering extension point — the {@code core.segmentation
 * .query.CustomOntologyFilterBase} contract that lets a client supply filtering logic {@code core}
 * cannot express on its own. This is the query-side counterpart to {@code AnyCatalogBase}: that lets a
 * client plug its own storage into a catalog, these let filtering exploit whatever it stored there.
 *
 * <h2>Contents</h2>
 * Two base classes, and the choice between them is the whole API:
 * <ul>
 *   <li>{@code AbstractEnumeratingFilterBase} — implement {@code enumerate()}; a memoized
 *       {@code test(int)} is derived for you.</li>
 *   <li>{@code AbstractTestingFilterBase} — implement {@code test(int)}; you are never asked to
 *       produce a set.</li>
 * </ul>
 * Both serve class filtering and element filtering alike, for the same reason {@code AnyCatalogBase}
 * serves both catalogs: a filter is a predicate over identities, and the two kinds of identity are the
 * same shape. Which kind an implementation is actually written for is a matter of naming and
 * discipline, not of type — see {@code CustomOntologyFilterBase} for that trade-off.
 *
 * <h2>Why the split</h2>
 * Some filters are cheaper handing back everything that satisfies them; others are cheaper judging one
 * candidate at a time, and neither wins universally. So a client provides exactly two things: which
 * direction it prefers, and that direction's implementation. Which base class you extend <em>is</em>
 * that declaration, so a filter can never claim a direction it does not implement. Everything else —
 * deriving the other direction, choosing where a resolution anchors, and deciding whether the whole
 * class or element set has to be walked at all — belongs to the resolver.
 * <p>
 * Narrowing in particular never appears in these contracts. A subclass answers only about its own
 * filtering rule: its complete domain, or one candidate's verdict. Intersecting that against a smaller
 * candidate set is the resolver's bookkeeping, so no subclass can get it wrong or quietly skip it.
 * <p>
 * Implementing {@code CustomOntologyFilterBase} directly is equally valid and is the only way to write
 * a filter as a lambda ({@code test} is its single abstract method); these classes exist because the
 * enumerating one also carries the derived-test cache, and because they are the discoverable place to
 * document each strategy.
 */
package org.halim.ontology.view.filter;
