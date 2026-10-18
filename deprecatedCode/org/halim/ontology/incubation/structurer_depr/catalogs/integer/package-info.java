/**
 * Chunked integral catalogs, one per storage width: long, int, short and byte. Every value still
 * leaves as a long through
 * {@link org.halim.ontology.incubation.structurer_depr.catalogs.IntegerValueCatalog}, so the width a
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
 * {@link org.halim.ontology.incubation.structurer_depr.catalogs.ChunkedCatalogBase}, which is generic
 * over the <em>array</em> type rather than the element type and so shares that machinery without
 * boxing anything.
 * */
package org.halim.ontology.incubation.structurer_depr.catalogs.integer;
