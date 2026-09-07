/**
 * The engine's internal write-side ports (as opposed to {@code segmentation.query}'s read-side
 * ones): {@code OntologyClassificationSegmentation} (class/element identity minting and tagging),
 * {@code OntologyHierarchySegmentation} (parent/child structure), {@code
 * OntologyInplaceCommandingSegmentation} (callback-style bulk iteration with mutation allowed),
 * {@code AnyCatalogMutableBase} (the generic write side of a catalog's detail storage -- see
 * {@code segmentation.query.AnyCatalogBase} for its read counterpart), and {@code
 * OntologyServicesFactorySegmentation} (the composition root's own port, giving access to every
 * other segmentation service plus undo/redo control). These are {@code core}-internal contracts;
 * external callers use {@code org.halim.ontology.legacy.view} instead.
 */
package org.halim.ontology.legacy.core.segmentation.command;
