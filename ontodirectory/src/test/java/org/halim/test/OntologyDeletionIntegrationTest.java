package org.halim.test;

import org.halim.legacy.dlake.FileInterface;
import org.halim.legacy.dlake.OntologyClass;
import org.halim.legacy.dlake.OntologyHierarchyFast;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class OntologyDeletionIntegrationTest {

private OntologyHierarchyFast hierarchy;
private OntologyHierarchyFast.OntologyHierarchyManager manager;

@BeforeEach
public void setup() {
	hierarchy = new OntologyHierarchyFast();
	hierarchy.onLoad();
	manager = hierarchy.new OntologyHierarchyManager();
}

@Test
public void testCase1_DeleteLeafNode_WithFiles() {
	int leafId = manager.createOntologyClass("Leaf", null, null);
	FileInterface file = new FileInterface(); file.identity = 1;
	manager.addElement(leafId, file);
	
	manager.removeOntologyClass(leafId);
	
	assertThat(hierarchy.getClassFromIdentity(leafId)).isNull(); // Tombstoned
	// File must drop the tag gracefully
	assertThat(file.tagsByIdentity).doesNotContain(leafId);
}

@Test
public void testCase2_DeleteMiddleNode_OrphansMustReanchor() {
	// Root -> Parent -> Middle -> Child
	int parentId = manager.createOntologyClass("Parent", null, null);
	int middleId = manager.createOntologyClass("Middle", List.of(parentId), null);
	int childId = manager.createOntologyClass("Child", List.of(middleId), null);
	
	manager.removeOntologyClass(middleId);
	
	OntologyClass child = hierarchy.getClassFromIdentity(childId);
	OntologyClass parent = hierarchy.getClassFromIdentity(parentId);
	
	// Middle is gone. Child must not point to Middle anymore.
	assertThat(child.parents).doesNotContain(hierarchy.getClassFromIdentity(middleId));
	// Parent must not point to Middle anymore.
	assertThat(parent.children).doesNotContain(hierarchy.getClassFromIdentity(middleId));
	
	// If the child has 0 parents after deletion, it should mathematically fall back to Root.
	assertThat(child.parents).isNotEmpty();
}

@Test
public void testCase3_DeleteMultiParentNode_DiamondTopology() {
	// A -> B, A -> C, B -> D, C -> D. Delete D.
	int nodeA = manager.createOntologyClass("A", null, null);
	int nodeB = manager.createOntologyClass("B", List.of(nodeA), null);
	int nodeC = manager.createOntologyClass("C", List.of(nodeA), null);
	int nodeD = manager.createOntologyClass("D", List.of(nodeB, nodeC), null);
	
	manager.removeOntologyClass(nodeD);
	
	assertThat(hierarchy.getClassFromIdentity(nodeB).children).isEmpty();
	assertThat(hierarchy.getClassFromIdentity(nodeC).children).isEmpty();
	assertThat(hierarchy.getClassFromIdentity(nodeD)).isNull();
}

@Test
public void testCase4_DeleteAndUndo_RestoresExactTopology() {
	int p1 = manager.createOntologyClass("P1", null, null);
	int target = manager.createOntologyClass("Target", List.of(p1), null);
	int c1 = manager.createOntologyClass("C1", List.of(target), null);
	
	manager.removeOntologyClass(target);
	assertThat(hierarchy.getClassFromIdentity(target)).isNull();
	
	manager.undo();
	
	OntologyClass restored = hierarchy.getClassFromIdentity(target);
	assertThat(restored).isNotNull();
	assertThat(restored.name).isEqualTo("Target");
	assertThat(hierarchy.getClassFromIdentity(p1).children).contains(restored);
	assertThat(hierarchy.getClassFromIdentity(c1).parents).contains(restored);
}

@Test
public void testCase5_RapidFire_CreateDeleteCreate() {
	// Proves that tombstone insertion doesn't break new identity assignments
	int id1 = manager.createOntologyClass("Temp1", null, null);
	int id2 = manager.createOntologyClass("Temp2", null, null);
	
	manager.removeOntologyClass(id1);
	
	int id3 = manager.createOntologyClass("Temp3", null, null);
	
	// The system should reuse the tombstoned index (id1) for id3
	assertThat(id3).isEqualTo(id1);
	assertThat(hierarchy.getClassFromIdentity(id3).name).isEqualTo("Temp3");
	assertThat(hierarchy.getClassFromIdentity(id2).name).isEqualTo("Temp2");
}

@Test
public void testCase6_RootDeletionRejection() {
	// Root class should mathematically reject deletion to prevent complete DAG collapse
	OntologyClass root = manager.getRootOntologyClass();
	manager.removeOntologyClass(root.identityNumber);
	
	assertThat(hierarchy.getClassFromIdentity(root.identityNumber)).isNotNull();
}
}