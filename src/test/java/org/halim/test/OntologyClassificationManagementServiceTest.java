package org.halim.test;

import org.halim.ontology.core.OntologyEngine;
import org.halim.ontology.view.implementations.DefaultOntologyService;
import org.halim.ontology.view.OntologyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end coverage of {@code OntologyClassificationManagementService} through the real
 * {@link OntologyEngine} + {@link DefaultOntologyService}: proves elements never end up with zero
 * class connections (creation defaults to root, untagging the last tag reanchors to root), and that
 * bulk tagging produces exactly one undo entry per user-facing batch action.
 */
public class OntologyClassificationManagementServiceTest {

private OntologyEngine engine;
private OntologyService service;

@BeforeEach
public void setup() {
	engine = new OntologyEngine();
	service = new DefaultOntologyService(engine);
}

@Test
public void testCreateElement_NoClassesGiven_DefaultsToRoot() {
	int element = service.getClassificationManagementService().createElement("orphan.txt");

	assertThat(engine.getIndexerService().getClassesOf(element).toIntArray()).containsExactly(0);
}

@Test
public void testCreateElement_WithClasses_TagsAllOfThemInOneCall() {
	int classA = service.getHierarchyManagementService().createClass("A");
	int classB = service.getHierarchyManagementService().createClass("B");

	int element = service.getClassificationManagementService().createElement("file.txt", classA, classB);

	assertThat(engine.getIndexerService().getClassesOf(element).toIntArray()).containsExactlyInAnyOrder(classA, classB);
}

@Test
public void testUntagElement_LastTag_ReanchorsToRoot() {
	int classA = service.getHierarchyManagementService().createClass("A");
	int element = service.getClassificationManagementService().createElement("file.txt", classA);

	service.getClassificationManagementService().untagElement(classA, element);

	assertThat(engine.getIndexerService().getClassesOf(element).toIntArray()).containsExactly(0);
}

@Test
public void testUntagElement_NotLastTag_DoesNotReanchor() {
	int classA = service.getHierarchyManagementService().createClass("A");
	int classB = service.getHierarchyManagementService().createClass("B");
	int element = service.getClassificationManagementService().createElement("file.txt", classA, classB);

	service.getClassificationManagementService().untagElement(classA, element);

	assertThat(engine.getIndexerService().getClassesOf(element).toIntArray()).containsExactly(classB);
}

@Test
public void testTagElements_Bulk_ProducesExactlyOneUndoEntry() {
	int classA = service.getHierarchyManagementService().createClass("A");
	int e1 = service.getClassificationManagementService().createElement("1.txt");
	int e2 = service.getClassificationManagementService().createElement("2.txt");
	int e3 = service.getClassificationManagementService().createElement("3.txt");

	service.getClassificationManagementService().tagElements(classA, new int[]{e1, e2, e3});

	assertThat(engine.getIndexerService().isConnected(classA, e1)).isTrue();
	assertThat(engine.getIndexerService().isConnected(classA, e2)).isTrue();
	assertThat(engine.getIndexerService().isConnected(classA, e3)).isTrue();

	engine.undo(); // one call must undo all three tags

	assertThat(engine.getIndexerService().isConnected(classA, e1)).isFalse();
	assertThat(engine.getIndexerService().isConnected(classA, e2)).isFalse();
	assertThat(engine.getIndexerService().isConnected(classA, e3)).isFalse();
}

}
