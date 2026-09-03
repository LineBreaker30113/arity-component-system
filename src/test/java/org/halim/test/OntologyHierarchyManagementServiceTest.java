package org.halim.test;

import org.halim.ontology.core.OntologyEngine;
import org.halim.ontology.core.segmentation.query.OntologyElementFilter;
import org.halim.ontology.view.implementations.DefaultOntologyService;
import org.halim.ontology.view.OntologyService;
import org.halim.ontology.view.command.OntologyHierarchyManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end coverage of {@link OntologyHierarchyManagementService} through the real
 * {@link OntologyEngine} + {@link DefaultOntologyService}. Topologies port legacy's
 * {@code org.halim.test.OntologyDeletionIntegrationTest} (which pins the equivalent behavior against
 * {@code org.halim.legacy.dlake.OntologyHierarchyFast}) as a behavioral oracle for
 * {@code deleteClass}'s cleanup order, plus proves the id-space unification and atomic
 * undo/rollback fixes this service depends on.
 */
public class OntologyHierarchyManagementServiceTest {

private OntologyEngine engine;
private OntologyService service;

@BeforeEach
public void setup() {
	engine = new OntologyEngine();
	service = new DefaultOntologyService(engine);
}

@Test
public void testCreateClass_MintsIdUsableThroughBothHierarchyAndIndexer() {
	int classId = service.getHierarchyManagementService().createClass("Documents");

	assertThat(service.getOntologyDataService().getClassName(classId)).isEqualTo("Documents");
	assertThat(engine.getHierarchyService().getParents(classId).toIntArray()).containsExactly(0);
	assertThat(engine.getIndexerService().getElementsOf(classId).toIntArray()).isEmpty();
}

@Test
public void testCreateClass_WithParent_ReparentsAwayFromRoot() {
	OntologyHierarchyManagementService hierarchyCmd = service.getHierarchyManagementService();
	int parent = hierarchyCmd.createClass("Parent");
	int child = hierarchyCmd.createClass("Child", parent);

	assertThat(engine.getHierarchyService().getParents(child).toIntArray()).containsExactly(parent);
}

@Test
public void testDeleteClass_Leaf_WithTaggedElement_ReanchorsElementToRoot() {
	int leaf = service.getHierarchyManagementService().createClass("Leaf");
	int element = service.getClassificationManagementService().createElement("file.txt", leaf);

	service.getHierarchyManagementService().deleteClass(leaf);

	assertThat(engine.getIndexerService().getClassesOf(element).toIntArray()).containsExactly(0);
}

@Test
public void testDeleteClass_MiddleNode_OrphanedChildReanchorsToRoot() {
	// Root -> Parent -> Middle -> Child
	OntologyHierarchyManagementService hierarchyCmd = service.getHierarchyManagementService();
	int parent = hierarchyCmd.createClass("Parent");
	int middle = hierarchyCmd.createClass("Middle", parent);
	int child = hierarchyCmd.createClass("Child", middle);

	hierarchyCmd.deleteClass(middle);

	assertThat(engine.getHierarchyService().getParents(child).toIntArray()).containsExactly(0);
	assertThat(engine.getHierarchyService().getChildren(parent).toIntArray()).isEmpty();
}

@Test
public void testDeleteClass_DiamondTopology_SurvivingParentsLoseChildButStayIntact() {
	// A -> B, A -> C, B -> D, C -> D. Delete D.
	OntologyHierarchyManagementService hierarchyCmd = service.getHierarchyManagementService();
	int a = hierarchyCmd.createClass("A");
	int b = hierarchyCmd.createClass("B", a);
	int c = hierarchyCmd.createClass("C", a);
	int d = hierarchyCmd.createClass("D", b, c);

	hierarchyCmd.deleteClass(d);

	assertThat(engine.getHierarchyService().getChildren(b).toIntArray()).isEmpty();
	assertThat(engine.getHierarchyService().getChildren(c).toIntArray()).isEmpty();
}

@Test
public void testDeleteClass_ThenUndo_RestoresExactTopologyAsOneOperation() {
	OntologyHierarchyManagementService hierarchyCmd = service.getHierarchyManagementService();
	int p1 = hierarchyCmd.createClass("P1");
	int target = hierarchyCmd.createClass("Target", p1);
	int c1 = hierarchyCmd.createClass("C1", target);

	hierarchyCmd.deleteClass(target);
	assertThat(engine.getHierarchyService().getParents(c1).toIntArray()).containsExactly(0);

	engine.undo(); // one call must restore the whole composed delete

	assertThat(engine.getHierarchyService().getParents(target).toIntArray()).containsExactly(p1);
	assertThat(engine.getHierarchyService().getParents(c1).toIntArray()).containsExactly(target);
}

@Test
public void testRapidFire_CreateDeleteCreate_ReusesTombstoneCleanly() {
	OntologyHierarchyManagementService hierarchyCmd = service.getHierarchyManagementService();
	int temp1 = hierarchyCmd.createClass("Temp1");
	int temp2 = hierarchyCmd.createClass("Temp2");

	hierarchyCmd.deleteClass(temp1);

	int temp3 = hierarchyCmd.createClass("Temp3");

	assertThat(temp3).isEqualTo(temp1); // tombstoned id reused
	assertThat(service.getOntologyDataService().getClassName(temp3)).isEqualTo("Temp3");
	assertThat(service.getOntologyDataService().getClassName(temp2)).isEqualTo("Temp2");
	// The reused slot must be clean: root-anchored, no leaked edges from its previous life
	assertThat(engine.getHierarchyService().getParents(temp3).toIntArray()).containsExactly(0);
	assertThat(engine.getHierarchyService().getChildren(temp3).toIntArray()).isEmpty();
}

@Test
public void testDeleteClass_Root_Throws() {
	assertThatThrownBy(() -> service.getHierarchyManagementService().deleteClass(0))
		  .isInstanceOf(IllegalArgumentException.class);
}

@Test
public void testMaterializeFilterAsClass_TagsAllMatchesInOneUndoEntry() {
	OntologyHierarchyManagementService hierarchyCmd = service.getHierarchyManagementService();
	int sourceClass = hierarchyCmd.createClass("Source");
	int e1 = service.getClassificationManagementService().createElement("a.txt", sourceClass);
	int e2 = service.getClassificationManagementService().createElement("b.txt", sourceClass);

	int materialized = hierarchyCmd.materializeFilterAsClass(
		  new OntologyElementFilter.DirectElementOf(sourceClass), "Materialized");

	assertThat(engine.getIndexerService().isConnected(materialized, e1)).isTrue();
	assertThat(engine.getIndexerService().isConnected(materialized, e2)).isTrue();

	engine.undo(); // one call must revert the whole materialization

	assertThat(engine.getIndexerService().isConnected(materialized, e1)).isFalse();
}

}
