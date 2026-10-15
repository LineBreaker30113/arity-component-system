/**
 * Chunked integral catalogs, one per storage width: long, int, short and byte. Every value still
 * leaves as a long through
 * {@link org.halim.ontology.incubation.acse.arity.catalog.IntegerValueCatalog}, so the width a
 * catalog was built at is invisible to anything reading it - and is multiplied by the identity count
 * and by nothing else, which is why there is a class for each.
 * <p>
 * The three narrow ones implement {@code IntSizedIntegerValueCatalog}, so their values are known to
 * fit an int and neither the int view nor the double view can refuse one. Reading widens, which is
 * free and exact. Writing from the long side is checked, because that direction wraps rather than
 * saturating and a wrapped value is indistinguishable from a real one - the refusal is the signal
 * that the catalog wanted was a wider one.
 * <p>
 * Each class is its four array hooks and its two accessors; growth, truncation and the chunk list
 * itself live once in
 * {@link org.halim.ontology.incubation.acse.arity.catalog.memory.ChunkedCatalogBase}, which is generic
 * over the <em>array</em> type rather than the element type and so shares that machinery without
 * boxing anything.
 * <p>
 * Chunked floating catalogs, one per storage width: double and float. Every value leaves as a double
 * through {@link org.halim.ontology.incubation.acse.arity.catalog.FloatingValueCatalog}, and the
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
 * {@link org.halim.ontology.incubation.acse.arity.catalog.memory.ChunkedCatalogBase}.
 * */
package org.halim.ontology.incubation.acse.arity.catalog.memory;
