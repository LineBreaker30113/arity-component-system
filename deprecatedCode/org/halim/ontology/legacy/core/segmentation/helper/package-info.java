/**
 * Annotations that drive {@code OntologyServiceProxy}'s cross-cutting behavior, rather than any
 * executable logic. {@code BypassCrossCuttingConcerns} names the concerns a proxied method can opt
 * out of ({@code LOCKING_R}/{@code LOCKING_W}/{@code LOGGING}/{@code UNDO_REDO}); {@code
 * OntologyTransaction} marks a method as starting/joining an undo/redo transaction; {@code
 * OntologyCommandable} and {@code OntologyQueryable} are marker namespaces of further, more
 * specific bypass annotations (e.g. {@code OntologyCommandable.BypassNonCommandConcerns}, {@code
 * OntologyQueryable.BypassNonQueryConcerns}) plus grouping interfaces ({@code ClassFamily}, {@code
 * ClassHierarchy}, ...) that {@code segmentation.command}/{@code segmentation.query} ports extend
 * to share method signatures without duplicating them.
 */
package org.halim.ontology.legacy.core.segmentation.helper;
