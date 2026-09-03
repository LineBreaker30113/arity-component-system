/**
 * Implementations of the {@link org.halim.ontology.core.segmentation.query} read-side
 * ports. Classes here compose the already-proxied manager services ({@code segments.manager}'s
 * hierarchy and classification engines) into the higher-level answers those ports promise; they hold
 * no locking or undo/redo bookkeeping of their own, which lives entirely behind
 * {@code core.OntologyServiceFactory}'s proxy.
 *
 * <h2>Contents</h2>
 * <ul>
 *   <li>{@code OntologyClassCatalog} / {@code OntologyElementCatalog} — per-entry detail storage,
 *       layered over a composed {@code AnyCatalogBase}/{@code AnyCatalogMutableBase} store that
 *       defaults to {@code OntologyCatalogDetailStore} but can be supplied by a client.</li>
 *   <li>{@code OntologyCatalogDetailStore} — the engine's built-in default for that store. It holds
 *       the name detail only, and deliberately refuses typed numeric/boolean details rather than
 *       guessing at a representation: a client wanting those supplies its own store.</li>
 *   <li>{@code OntologyAnalyticsCalculator} — hierarchy-shape analytics, stateless by design (every
 *       method recomputes; a caching sibling using the change-listener callbacks is planned
 *       separately).</li>
 *   <li>{@code OntologyFilterResolverSegmentation} — filter resolution, described below.</li>
 * </ul>
 *
 * <h2>Filter resolution</h2>
 * The resolver is the one class here with a non-obvious algorithm, and its own javadoc carries the
 * full account. In outline: every filter node supports both <b>enumerate</b> (produce the matching
 * set) and <b>test</b> (judge one candidate), and reports which is its cheap direction. Resolution is
 * two mutually recursive walks — {@code enumerate}, which maps a node onto an already-efficient engine
 * call and routes combinators toward whichever child can drive one, and {@code test}, which composes
 * upward through every combinator for free and never requires a scan.
 * <p>
 * The constraint the whole shape exists to satisfy: the entirety of the classes — or of the elements,
 * never both — is walked <b>at most once</b> per resolution, and not at all when any part of the tree
 * can anchor an enumeration. Two mechanisms hold that up, and both are pinned by
 * {@code OntologyFilterScanCountTest} rather than asserted: the universe is materialized in exactly
 * one place, and a per-resolution memo collapses repeated occurrences of one node so a broad leaf
 * sitting in several branches walks the hierarchy once, not once per branch.
 */
package org.halim.ontology.core.segments.data;
