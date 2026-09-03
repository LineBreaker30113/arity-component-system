/**
 * The three concrete engine implementations {@code core}'s composition root wires together. {@code
 * DoubleArrayedIndexer} implements {@code segmentation.command.OntologyClassificationSegmentation}
 * -- minting and reclaiming class/element identities and recording which elements are tagged with
 * which classes. {@code OntologyHierarchyLister} implements {@code
 * segmentation.command.OntologyHierarchySegmentation} -- the parent/child DAG structure,
 * permanently reserving id 0 for the root class. {@code OntologyInplaceWalker} implements {@code
 * segmentation.command.OntologyInplaceCommandingSegmentation} by composing the (already-proxied)
 * hierarchy and classification services to drive filter-driven, callback-style iteration. None of
 * the three do their own locking or undo/redo bookkeeping -- see {@code
 * org.halim.ontology.core.OntologyServiceFactory}.
 */
package org.halim.ontology.core.segments.manager;
