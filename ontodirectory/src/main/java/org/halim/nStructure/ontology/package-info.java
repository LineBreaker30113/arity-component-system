/**
 * A pure set-theory/DAG classification engine: elements ("files," in this application, though the
 * engine itself has no file concept) are associated with named classes ("tags") arranged in a
 * multiple-inheritance hierarchy, and classified via ordinary set operations -- membership, union,
 * intersection, domain-of-vs-direct-element-of -- rather than a single-parent folder tree. Nothing
 * in this package or its subpackages knows about files, disk paths, or Swing; that's left entirely
 * to {@code org.halim.nStructure.archive} and {@code org.halim.nStructure.sgui}.
 * <p>
 * Two subpackages split the engine along the hexagonal architecture's inside/outside seam:
 * {@link org.halim.nStructure.ontology.core} builds and wires the engine itself (segmentation
 * ports plus their segments implementations, composed by {@code OntologyEngine}), while
 * {@link org.halim.nStructure.ontology.view} is the client-facing surface external callers
 * actually depend on -- {@code core} is never referenced from outside this package.
 */
package org.halim.nStructure.ontology;
