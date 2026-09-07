/**
 * The live work. Everything here is provisional and expected to change.
 * <p>
 * Two generations sit side by side. {@link org.halim.ontology.incubation.structurer} is the current
 * one: long identities, set algebra over them, catalogs of data hanging off them. The classes at
 * this level - {@link org.halim.ontology.incubation.OE_IntSet},
 * {@link org.halim.ontology.incubation.OE_Catalog},
 * {@link org.halim.ontology.incubation.IntSetListerNegative},
 * {@link org.halim.ontology.incubation.IntSetListerPositive} - are the int-identity generation that
 * came first, kept because the negative representation was worked out here and the test suite still
 * holds them to their contract. Read them for the idea; the widened forms in {@code structurer} are
 * where the work continues.
 * <p>
 * {@link org.halim.ontology.incubation.BidirectionalIndexingService} is the one live file still
 * reaching into {@code legacy}, and is the seam where the old engine's classification service was
 * left behind.
 * <p>
 * Nothing here is multi thread safe, and nothing here promises source compatibility between
 * sessions.
 * */
package org.halim.ontology.incubation;
