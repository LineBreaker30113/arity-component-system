package org.halim.test;

import org.halim.ontology.legacy.core.OntologyEngine;
import org.halim.ontology.legacy.core.segmentation.command.AnyCatalogMutableBase;
import org.halim.ontology.legacy.core.segmentation.query.AnyCatalogBase;
import org.halim.ontology.legacy.core.segments.data.OntologyCatalogDetailStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Covers {@link OntologyEngine}'s client-injected-catalog constructor: a caller can supply its own
 * {@link AnyCatalogBase}/{@link AnyCatalogMutableBase} storage for classes and/or elements instead
 * of the engine's default {@link OntologyCatalogDetailStore}, and the engine's catalog services
 * (through the real proxy) read and write through that supplied storage.
 */
public class OntologyEngineCatalogInjectionTest {

@Test
public void testClientSuppliedClassCatalogStore_IsUsedByTheEngine() {
	OntologyCatalogDetailStore classStore = new OntologyCatalogDetailStore();
	OntologyEngine engine = new OntologyEngine(classStore, new OntologyCatalogDetailStore());

	engine.getClassDataService().setName(0, "File");

	assertThat(engine.getClassDataService().getName(0)).isEqualTo("File");
	// The engine's write landed on the exact store instance the caller supplied, not a private copy.
	assertThat(classStore.getDetailObject(0, 0)).isEqualTo("File");
}

@Test
public void testClientSuppliedElementCatalogStore_IsUsedByTheEngine() {
	OntologyCatalogDetailStore elementStore = new OntologyCatalogDetailStore();
	OntologyEngine engine = new OntologyEngine(new OntologyCatalogDetailStore(), elementStore);
	engine.getIndexerService().newClass(); // burns the indexer's id 0, aligning it with the hierarchy's reserved root
	int element = engine.getIndexerService().newElement();

	engine.getElementDataService().setName(element, "todo.txt");

	assertThat(engine.getElementDataService().getName(element)).isEqualTo("todo.txt");
	assertThat(elementStore.getDetailObject(element, 0)).isEqualTo("todo.txt");
}

@Test
public void testClassAndElementCatalogStores_AreIndependent() {
	OntologyCatalogDetailStore classStore = new OntologyCatalogDetailStore();
	OntologyCatalogDetailStore elementStore = new OntologyCatalogDetailStore();
	OntologyEngine engine = new OntologyEngine(classStore, elementStore);

	engine.getClassDataService().setName(0, "File");

	// The write must land only in the store the engine was given for classes, never in the
	// separately-instanced one it was given for elements (both use detail id 0 for "name").
	assertThat(classStore.getDetailObject(0, 0)).isEqualTo("File");
	assertThat(elementStore.getDetailObject(0, 0)).isNull();
}

}
