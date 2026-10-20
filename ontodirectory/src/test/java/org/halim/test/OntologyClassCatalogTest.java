package org.halim.test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.halim.nStructure.ontology.core.segments.data.OntologyClassCatalog;
import org.halim.nStructure.ontology.core.segments.manager.DoubleArrayedIndexer;
import org.halim.nStructure.ontology.core.segments.manager.OntologyHierarchyLister;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.*;

public class OntologyClassCatalogTest {

private static final int ROOT = 0;

private OntologyHierarchyLister hierarchy;
private DoubleArrayedIndexer indexer;
private OntologyClassCatalog catalog;
private IntArrayList hierarchyClassesField;

@BeforeEach
public void setup() throws Exception {
	hierarchy = new OntologyHierarchyLister();
	indexer = new DoubleArrayedIndexer();
	catalog = new OntologyClassCatalog(hierarchy, indexer);

	Field f = OntologyHierarchyLister.class.getDeclaredField("classes");
	f.setAccessible(true);
	hierarchyClassesField = (IntArrayList) f.get(hierarchy);

	// DoubleArrayedIndexer has no reserved root id (its first newClass() returns 0), while
	// OntologyHierarchyLister permanently reserves id 0 for root. Consume the indexer's id 0
	// here so both engines' identity spaces line up for every class minted afterward.
	indexer.newClass();
}

/** Mints a class id shared by both the indexer (for elements/tombs) and the hierarchy (anchored to root). */
private int newClass() {
	int id = indexer.newClass();
	hierarchyClassesField.add(ROOT);
	return id;
}

@Test
public void testSetName_ThenGetName_RoundTrips() {
	int c = newClass();
	catalog.setName(c, "Documents");
	assertThat(catalog.getName(c)).isEqualTo("Documents");
}

@Test
public void testGetName_NeverSet_ReturnsNull() {
	int c = newClass();
	assertThat(catalog.getName(c)).isNull();
}

@Test
public void testDelegatesHierarchyQueries() {
	int p = newClass();
	int c = newClass();
	hierarchy.addParent(c, p);

	assertThat(catalog.isParent(c, p)).isTrue();
	assertThat(catalog.getParentCount(c)).isEqualTo(1);
	assertThat(catalog.getChildrenCount(p)).isEqualTo(1);
	assertThat(catalog.getParents(c).toIntArray()).containsExactly(p);
	assertThat(catalog.getChildren(p).toIntArray()).containsExactly(c);
}

@Test
public void testDelegatesClassificationQueries() {
	int c = newClass();
	int e = indexer.newElement();
	indexer.ensureConnection(c, e);

	assertThat(catalog.getElementsCount(c)).isEqualTo(1);
	assertThat(catalog.getElementsOf(c).toIntArray()).containsExactly(e);
}

@Test
public void testGetDomainSize_CountsSelfAndDescendantsWithoutDoubleCounting() {
	int parent = newClass();
	int child = newClass();
	hierarchy.addParent(child, parent);

	int sharedElement = indexer.newElement();
	int parentOnlyElement = indexer.newElement();
	int childOnlyElement = indexer.newElement();

	// sharedElement is tagged under BOTH parent and child -- domain size must not double-count it
	indexer.ensureConnection(parent, sharedElement);
	indexer.ensureConnection(child, sharedElement);
	indexer.ensureConnection(parent, parentOnlyElement);
	indexer.ensureConnection(child, childOnlyElement);

	assertThat(catalog.getDomainElementCount(parent)).isEqualTo(3);
	assertThat(catalog.getDomainElementCount(child)).isEqualTo(2);
}

@Test
public void testGetDomainSize_LeafWithNoElements_IsZero() {
	int c = newClass();
	assertThat(catalog.getDomainElementCount(c)).isEqualTo(0);
}

}
