/**
 * A pure set-theory/DAG classification engine: elements are associated with named classes ("tags")
 * arranged in a multiple-inheritance hierarchy, and classified via ordinary set operations --
 * membership, union, intersection, domain-of-vs-direct-element-of -- rather than a single-parent
 * folder tree.
 * <p>
 * <b>Elements are deliberately undefined, and so is the data a class carries.</b> An element is an
 * int identity and nothing more; the engine attaches no meaning to it and has no notion of files,
 * disk paths, or a user interface. Anything a client wants to hang off a class or an element goes in
 * through {@code core.segmentation.query.AnyCatalogBase} (storage) and is filtered on through
 * {@code core.segmentation.query.CustomOntologyFilterBase} (queries). Consequently anything in this
 * package that assumed a shape for an element or its data would be wrong by construction -- the
 * engine is not usable on its own, it is meant to be adaptable to anything.
 * <p>
 * Two subpackages split the engine along the hexagonal architecture's inside/outside seam:
 * {@link org.halim.ontology.core} builds and wires the engine itself (segmentation ports plus their
 * segments implementations, composed by {@code OntologyEngine}), while
 * {@link org.halim.ontology.view} is the client-facing surface external callers actually depend on
 * -- {@code core} is never referenced from outside this package.
 * <p>
 * Extracted from the OntoDirectory application, where the same engine is applied to file
 * classification; that application supplies the "files are elements, tags are classes" reading this
 * package deliberately does not.
 */
package org.halim.ontology;
