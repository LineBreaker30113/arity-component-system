package org.halim.test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.halim.ontology.legacy.core.segments.data.OntologyElementCatalog;
import org.halim.ontology.legacy.core.segments.manager.DoubleArrayedIndexer;
import org.halim.ontology.legacy.core.segments.manager.OntologyHierarchyLister;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.BitSet;

import static org.assertj.core.api.Assertions.*;

public class OntologyElementCatalogTest {

private static final int ROOT = 0;

private OntologyHierarchyLister hierarchy;
private DoubleArrayedIndexer indexer;
private OntologyElementCatalog catalog;
private IntArrayList hierarchyClassesField;

@BeforeEach
public void setup() throws Exception {
	hierarchy = new OntologyHierarchyLister();
	indexer = new DoubleArrayedIndexer();
	catalog = new OntologyElementCatalog(hierarchy, indexer);

	Field f = OntologyHierarchyLister.class.getDeclaredField("classes");
	f.setAccessible(true);
	hierarchyClassesField = (IntArrayList) f.get(hierarchy);

	// DoubleArrayedIndexer has no reserved root id (its first newClass() returns 0), while
	// OntologyHierarchyLister permanently reserves id 0 for root. Consume the indexer's id 0
	// here so both engines' identity spaces line up for every class minted afterward.
	indexer.newClass();
}

private int newClass() {
	int id = indexer.newClass();
	hierarchyClassesField.add(ROOT);
	return id;
}

@Test
public void testSetName_ThenGetName_RoundTrips() {
	int e = indexer.newElement();
	catalog.setName(e, "report.pdf");
	assertThat(catalog.getName(e)).isEqualTo("report.pdf");
}

@Test
public void testGetName_NeverSet_ReturnsNull() {
	int e = indexer.newElement();
	assertThat(catalog.getName(e)).isNull();
}

@Test
public void testDelegatesClassificationQueries() {
	int c = newClass();
	int e = indexer.newElement();
	indexer.ensureConnection(c, e);

	assertThat(catalog.getClassesCount(e)).isEqualTo(1);
	assertThat(catalog.getClassesOf(e).toIntArray()).containsExactly(c);
	BitSet bits = catalog.getClassesOfAsBitSet(e);
	assertThat(bits.get(c)).isTrue();
}

@Test
public void testGetMostDescriptivePath_NoClasses_ReturnsEmpty() {
	int e = indexer.newElement();
	assertThat(catalog.getMostDescriptivePath(e)).isEmpty();
}

@Test
public void testGetMostDescriptivePath_SingleClass_ReturnsRootToLeaf() {
	int c = newClass(); // anchored to root by default
	int e = indexer.newElement();
	indexer.ensureConnection(c, e);

	assertThat(catalog.getMostDescriptivePath(e)).containsExactly(ROOT, c);
}

@Test
public void testGetMostDescriptivePath_MultipleClasses_PicksDeepestOne() {
	int a = newClass();
	int b = newClass();
	hierarchy.addParent(b, a); // root -> a -> b

	int shallow = newClass(); // root -> shallow

	int e = indexer.newElement();
	indexer.ensureConnection(b, e);       // deep tag
	indexer.ensureConnection(shallow, e); // shallow tag

	assertThat(catalog.getMostDescriptivePath(e)).containsExactly(ROOT, a, b);
}

}
