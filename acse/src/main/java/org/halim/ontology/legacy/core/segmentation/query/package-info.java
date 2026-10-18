/**
 * The engine's internal read-side ports. {@code OntologyClassCatalogSegmentation}/{@code
 * OntologyElementCatalogSegmentation} expose per-class/per-element detail storage through the
 * generic {@code CatalogModuleBase} addressing scheme ({@code (entryIdentity, detailTypeIdentity)});
 * {@code OntologyAnalyticsSegmentation} answers hierarchy-shape questions (ancestry, heritage,
 * counts); {@code OntologyClassFilter}/{@code OntologyElementFilter} are plain-data predicate ASTs
 * that {@code OntologyFilteringServiceSegmentation} resolves into actual matches; {@code
 * OntologyInplaceQuerySegmentation} is the read-only half of callback-style bulk iteration (see
 * {@code segmentation.command.OntologyInplaceCommandingSegmentation} for its mutating half). These
 * are {@code core}-internal contracts; external callers use {@code
 * org.halim.ontology.legacy.view} instead.
 *
 * <h2>The two client-implementable contracts here</h2>
 * Most of this package is implemented by {@code core.segments} and consumed by callers. Two
 * interfaces run the other way — a client implements them and the engine calls through:
 * <ul>
 *   <li>{@code CatalogModuleBase} — storage. A client plugs its own structure-of-arrays store in behind
 *       a catalog.</li>
 *   <li>{@code CustomOntologyFilterBase} — filtering. A client supplies a filtering rule over that
 *       same client-defined data, wrapped into a tree by {@code OntologyClassFilter.Custom} or
 *       {@code OntologyElementFilter.Custom}.</li>
 * </ul>
 * They are the pair that makes the engine adaptable: the engine deliberately fixes no shape for what
 * an element is or what data hangs off a class, so anything that would assume one has to be pluggable.
 * Both are addressed by bare int identity and are shared across classes and elements rather than
 * duplicated per kind. {@code org.halim.ontology.legacy.view.filter} carries ready-made base
 * classes for the filtering one.
 *
 * <h2>Why the filter ASTs carry a preference</h2>
 * A filter node can be resolved in either of two directions — produce its whole matching set, or judge
 * one candidate — and neither is universally cheaper. Each node answers
 * {@code isEnumeratePreferred()} for itself, recursively for combinators, so a new building block
 * declares its own strategy instead of being classified from outside by the resolver. Testing is the
 * default and a node has to prove the opposite. The nodes stay plain data: the preference is a
 * property of the filter's shape, needing no engine access, so building a filter still never touches
 * the engine. {@code core.segments.data.OntologyFilterResolverSegmentation} documents how the two
 * directions are actually driven.
 */
package org.halim.ontology.legacy.core.segmentation.query;
