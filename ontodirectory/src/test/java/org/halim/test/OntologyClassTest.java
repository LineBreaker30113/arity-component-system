package org.halim.test;

import org.halim.legacy.OntoDirectoryException;
import org.halim.legacy.dlake.OntologyClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

public class OntologyClassTest {

private OntologyClass root;

@BeforeEach
public void setup() {
	root = OntologyClass.makeROOT_ONTOLOGY_CLASS("File");
}

@Test
public void testConstructor_AllFieldsInitializedNonNull() {
	OntologyClass node = new OntologyClass("TestNode", root);
	assertThat(node.parents).isNotNull().isNotEmpty();
	assertThat(node.children).isNotNull().isEmpty();
	assertThat(node.name).isEqualTo("TestNode");
}

@Test
public void testAddParent_DeepCycleRejection() {
	OntologyClass nodeA = new OntologyClass("A", root);
	OntologyClass nodeB = new OntologyClass("B", nodeA);
	OntologyClass nodeC = new OntologyClass("C", nodeB);
	OntologyClass nodeD = new OntologyClass("D", nodeC);
	
	// D attempts to make its great-grandparent (A) its child, causing a loop.
	assertThatThrownBy(() -> nodeA.addParent(nodeD))
		  .isInstanceOf(OntoDirectoryException.OntologyAddParentCausesCycle.class);
}

@Test
public void testAddParent_TransitiveReduction_BypassesMiddleman() {
	// Setup: A -> B. C descends directly from A.
	OntologyClass nodeA = new OntologyClass("A", root);
	OntologyClass nodeB = new OntologyClass("B", nodeA);
	OntologyClass nodeC = new OntologyClass("C", nodeA);
	
	// Action: C adds B as a parent.
	// Because B descends from A (C's current parent), the engine should sever A->C and wire B->C.
	nodeC.addParent(nodeB);
	
	assertThat(nodeC.parents).containsExactly(nodeB);
	assertThat(nodeA.children).doesNotContain(nodeC);
	assertThat(nodeA.children).containsExactly(nodeB);
}

@Test
public void testAddParent_MultiParentConvergence() {
	OntologyClass nodeY = new OntologyClass("Y", root);
	OntologyClass nodeZ = new OntologyClass("Z", root);
	OntologyClass nodeX = new OntologyClass("X", root);
	
	nodeX.addParent(nodeY);
	nodeX.addParent(nodeZ);
	nodeX.removeParent(root); // X now uniquely descends from Y and Z
	
	// W descends from Y and Z.
	OntologyClass nodeW = new OntologyClass("W", root);
	nodeW.addParent(nodeY);
	nodeW.addParent(nodeZ);
	nodeW.removeParent(root);
	
	// X adds W as a parent. W is a descendant of both Y and Z.
	// Therefore, X's direct links to Y and Z are obsolete.
	nodeX.addParent(nodeW);
	
	assertThat(nodeX.parents).containsExactly(nodeW);
	assertThat(nodeY.children).doesNotContain(nodeX);
	assertThat(nodeZ.children).doesNotContain(nodeX);
}

@Test
public void testRemoveParent_BatchRemoval() {
	OntologyClass p1 = new OntologyClass("P1", root);
	OntologyClass p2 = new OntologyClass("P2", root);
	OntologyClass p3 = new OntologyClass("P3", root);
	OntologyClass child = new OntologyClass("Child", p1);
	child.addParent(p2).addParent(p3);
	
	assertThat(child.parents).hasSize(3);
	
	child.removeParent(p1).removeParent(p3);
	
	assertThat(child.parents).containsExactly(p2);
	assertThat(p1.children).doesNotContain(child);
}

@Test
public void testGetAncestry_DiamondTopology() {
	// D -> B, D -> C. B -> A, C -> A.
	OntologyClass nodeD = new OntologyClass("D", root);
	OntologyClass nodeB = new OntologyClass("B", nodeD);
	OntologyClass nodeC = new OntologyClass("C", nodeD);
	
	OntologyClass nodeA = new OntologyClass("A", nodeB);
	nodeA.addParent(nodeC);
	
	Set<OntologyClass> ancestors = nodeA.getAncestrySet();
	
	// Must strictly contain A, B, C, D, and Root (Size 5). No duplicates/infinite loops.
	assertThat(ancestors).hasSize(5).contains(nodeA, nodeB, nodeC, nodeD, root);
}
@Test
public void testMakeRoot_BindsToSelf_RejectsNewParents() {
	OntologyClass fakeParent = new OntologyClass("FakeParent", root);
	assertThatThrownBy(() -> root.addParent(fakeParent))
		  .isInstanceOf(OntoDirectoryException.OntologyRootClassAddParentCall.class);
}

@Test
public void testAddParent_SelfCycleRejection() {
	OntologyClass node = new OntologyClass("Node", root);
	assertThatThrownBy(() -> node.addParent(node))
		  .isInstanceOf(OntoDirectoryException.OntologyAddParentSelf.class);
}

@Test
public void testAddParent_ExistingParent_NoOp() {
	OntologyClass p1 = new OntologyClass("P1", root);
	OntologyClass child = new OntologyClass("Child", p1);
	
	// Attempting to add an existing parent should safely return `this` without duplicate entries
	child.addParent(p1);
	assertThat(child.parents).containsExactly(p1);
}

@Test
public void testGetAncestry_LinearDepth() {
	OntologyClass nA = new OntologyClass("A", root);
	OntologyClass nB = new OntologyClass("B", nA);
	OntologyClass nC = new OntologyClass("C", nB);
	OntologyClass nD = new OntologyClass("D", nC);
	
	Set<OntologyClass> ancestors = nD.getAncestrySet();
	
	// Exactly 5 (D, C, B, A, Root)
	assertThat(ancestors).hasSize(5).contains(nD, nC, nB, nA, root);
}


}