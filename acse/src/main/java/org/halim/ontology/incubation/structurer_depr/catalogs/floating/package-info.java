/**
 * Chunked floating catalogs, one per storage width: double and float. Every value leaves as a double
 * through {@link org.halim.ontology.incubation.structurer_depr.catalogs.FloatingValueCatalog}, and the
 * widening out of a float is exact, so nothing downstream has to know which it is holding.
 * <p>
 * What the float form costs is precision, not range - about seven decimal digits against sixteen -
 * and writing narrows with a cast that <em>rounds</em> rather than throwing. That is the deliberate
 * difference from the integral catalogs: a truncated identity is a wrong answer no later operation
 * can detect, while a rounded measurement is still the value it claims to be. A catalog of scores,
 * weights or coordinates is the case for it; a catalog of exact sums is not.
 * <p>
 * Each class is its four array hooks and its two accessors; growth, truncation and the chunk list
 * itself live once in
 * {@link org.halim.ontology.incubation.structurer_depr.catalogs.ChunkedCatalogBase}.
 * */
package org.halim.ontology.incubation.structurer_depr.catalogs.floating;
