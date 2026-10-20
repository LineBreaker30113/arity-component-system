package org.halim.test;

import org.halim.nStructure.ontology.core.OntologyEngine;
import org.halim.nStructure.ontology.core.segmentation.query.OntologyElementFilter;
import org.halim.nStructure.ontology.view.implementations.DefaultOntologyService;
import org.halim.nStructure.ontology.view.OntologyService;
import org.halim.nStructure.ontology.view.command.OntologyHierarchyManagementService;
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

/**
 * Root-parented counterpart of the test above, and currently <b>FAILING</b> — a known open bug
 * (found 2026-09-04), not a flaky test. Deleting a top-level tag works; undoing that deletion
 * throws {@code OntologyHierarchyError: "Class is Already Parent!"} from
 * {@code OntologyHierarchyLister:72}.
 * <p>
 * Why the suite missed it: {@link #testDeleteClass_ThenUndo_RestoresExactTopologyAsOneOperation}
 * always gives its target a real parent, so {@code clearParents} removes that parent and undo
 * re-adds it to a class that no longer has it. When the only parent is root, undo instead re-adds
 * root to a class that already has root — {@code registerClass} installs {@code parent = root} as a
 * class's default leaf state ({@code OntologyHierarchyLister:38}), and {@code addParent} rejects a
 * parent it already holds ({@code :72}).
 * <p>
 * {@code org.halim.legacy} handles this case correctly (three top-level tags deleted, three undos,
 * all three restored), so this is an nStructure-only regression against the shipping behaviour.
 * Top-level tags are the common shape a user creates, so this is not an edge case.
 * <p>
 * The fix belongs in the undo closure ordering for {@code deleteClassInternal} — either re-adding
 * root only when it is not already present, or not letting the class's restoration re-establish the
 * root edge before the recorded closure replays. Which of the two is correct is the open question.
 */
@Test
public void testDeleteRootParentedClass_ThenUndo_RestoresIt() {
	OntologyHierarchyManagementService hierarchyCmd = service.getHierarchyManagementService();
	int target = hierarchyCmd.createClass("TopLevelTag"); // only parent is root (id 0)
	assertThat(engine.getHierarchyService().getParents(target).toIntArray()).containsExactly(0);

	hierarchyCmd.deleteClass(target);

	engine.undo(); // currently throws OntologyHierarchyError("Class is Already Parent!")

	assertThat(engine.getHierarchyService().getParents(target).toIntArray()).containsExactly(0);
	assertThat(service.getOntologyDataService().getClassName(target)).isEqualTo("TopLevelTag");
}

/**
 * The bulk shape of the same bug: the GUI's "Delete All Selected Tags" loops the single-tag call
 * ({@code HierarchyTreeView:266}), so N top-level tags record N {@code DELETE_CLASS} entries and the
 * first undo already throws. Currently <b>FAILING</b> for the same root cause as the test above.
 */
@Test
public void testDeleteSeveralRootParentedClasses_ThenUndoEach_RestoresAll() {
	OntologyHierarchyManagementService hierarchyCmd = service.getHierarchyManagementService();
	int a = hierarchyCmd.createClass("A");
	int b = hierarchyCmd.createClass("B");
	int c = hierarchyCmd.createClass("C");

	for (int id : new int[]{a, b, c}) hierarchyCmd.deleteClass(id);

	engine.undo();
	engine.undo();
	engine.undo(); // one undo per tag -- deletion is not batched into a single entry

	assertThat(service.getOntologyDataService().getClassName(a)).isEqualTo("A");
	assertThat(service.getOntologyDataService().getClassName(b)).isEqualTo("B");
	assertThat(service.getOntologyDataService().getClassName(c)).isEqualTo("C");
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
