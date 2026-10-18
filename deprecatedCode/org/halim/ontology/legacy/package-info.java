/**
 * A pure classification engine: elements are associated with named classes, that's all there is
 * to be known by the engine. The engine has no idea of the topology, it can be given using addons.
 * <p>
 * <b>Elements adn classes are deliberately undefined, and so is the data a class carries.</b>
 * An element or Class is an int identity and nothing more; the engine attaches no meaning to it.
 * Anything a client wants to hang off a class or an element goes in through
 * {@code core.segmentation.query.CatalogModuleBase} (storage) and is filtered on through
 * {@code core.segmentation.query.CustomOntologyFilterBase} (queries). Consequently anything in this
 * package that assumed a shape for an element or its data would be wrong by construction -- the
 * engine is not usable on its own, it is meant to be adaptable to anything.
 * <p>
 * Two subpackages split the engine along the hexagonal architecture's inside/outside seam:
 * {@link org.halim.ontology.legacy.core} builds and wires the engine itself (segmentation ports plus their
 * segments implementations, composed by {@code OntologyEngine}), while
 * {@link org.halim.ontology.legacy.view} is the client-facing surface external callers actually depend on
 * -- {@code core} is never referenced from outside this package.
 * <p>
 * Extracted from the OntoDirectory application, where the same engine is applied to file
 * classification; that application supplies the "files are elements, tags are classes" reading this
 * package deliberately does not.
 */
package org.halim.ontology.legacy;
