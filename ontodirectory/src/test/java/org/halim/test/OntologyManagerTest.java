package org.halim.test;

import org.halim.legacy.dlake.FileInterface;
import org.halim.legacy.dlake.OntologyClass;
import org.halim.legacy.dlake.OntologyFilter;
import org.halim.legacy.dlake.OntologyHierarchyFast;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class OntologyManagerTest {

private OntologyHierarchyFast hierarchy;
private OntologyHierarchyFast.OntologyHierarchyManager manager;

@BeforeEach
public void setup() {
	hierarchy = new OntologyHierarchyFast();
	hierarchy.onLoad(); // Initializes parallel filesPerOntology arrays
	manager = hierarchy.new OntologyHierarchyManager();
}

@Test
public void testUndoRedo_RestoresExactGraphState() {
	// 1. Action: Create Tag
	int tagId = manager.createOntologyClass("TestTag", null, null);
	
	// 2. Action: Rename Tag
	manager.renameOntologyClass(tagId, "RenamedTag");
	
	// 3. Action: Add File
	FileInterface dummyFile = new FileInterface();
	manager.addElement(tagId, dummyFile);
	
	// Verify State Applied
	assertThat(hierarchy.getClassFromIdentity(tagId).name).isEqualTo("RenamedTag");
	assertThat(hierarchy.filesPerOntology.get(tagId)).contains(dummyFile);
	
	// ACT: Undo 3 times
	manager.undo(); // Undo Add File
	manager.undo(); // Undo Rename
	manager.undo(); // Undo Create
	
	// Assert Empty Graph State
	assertThat(hierarchy.getClassFromIdentity(tagId)).isNull(); // Tombstoned
	
	// ACT: Redo 3 times
	manager.redo();
	manager.redo();
	manager.redo();
	
	// Assert Exact Restoration
	assertThat(hierarchy.getClassFromIdentity(tagId).name).isEqualTo("RenamedTag");
	assertThat(hierarchy.filesPerOntology.get(tagId)).contains(dummyFile);
}

@Test
public void testRenameClass_PreservesIdentityAndEdges() {
	int parentId = manager.createOntologyClass("Parent", null, null);
	int childId = manager.createOntologyClass("Child", List.of(parentId), null);
	
	manager.renameOntologyClass(parentId, "NewParentName");
	
	OntologyClass renamedParent = hierarchy.getClassFromIdentity(parentId);
	OntologyClass child = hierarchy.getClassFromIdentity(childId);
	
	// String updated, but edges and IDs remain completely untouched
	assertThat(renamedParent.name).isEqualTo("NewParentName");
	assertThat(renamedParent.identityNumber).isEqualTo(parentId);
	assertThat(child.parents).contains(renamedParent);
	assertThat(renamedParent.children).contains(child);
}

@Test
public void testFilterToClass_ResolvesSetTheoryCorrectly() {
	int tagA = manager.createOntologyClass("TagA", null, null);
	int tagB = manager.createOntologyClass("TagB", null, null);
	
	FileInterface file1 = new FileInterface(); file1.identity = 1; // In A and B
	FileInterface file2 = new FileInterface(); file2.identity = 2; // In A only
	
	manager.addElement(tagA, file1); manager.addElement(tagB, file1);
	manager.addElement(tagA, file2);
	
	// Filter: TagA AND TagB
	OntologyFilter filter = new OntologyFilter.AndFilter(
		  new OntologyFilter.DirectElementOf(tagA),
		  new OntologyFilter.DirectElementOf(tagB)
	);
	
	int newClassId = manager.filterToClass(filter, "IntersectionClass");
	
	// Assert new class is generated and strictly contains only file1
	assertThat(newClassId).isGreaterThan(0);
	assertThat(hierarchy.filesPerOntology.get(newClassId)).containsExactly(file1);
}

@Test
public void testCopyContentsTo_FullMatrix_NoConcurrentModification() {
	int sourceId = manager.createOntologyClass("Source", null, null);
	int targetId = manager.createOntologyClass("Target", null, null);
	int parentId = manager.createOntologyClass("Parent", null, null);
	int childId = manager.createOntologyClass("Child", null, null);
	
	manager.addParent(sourceId, parentId);
	manager.addParent(childId, sourceId);
	
	FileInterface fi = new FileInterface();
	manager.addElement(sourceId, fi);
	
	// Execute full copy matrix (Parents, Children, Files)
	manager.copyContentsTo(sourceId, targetId, true, true, true);
	
	OntologyClass target = hierarchy.getClassFromIdentity(targetId);
	
	// Assert target perfectly cloned the edges without throwing iterator exceptions
	assertThat(target.parents).extracting("identityNumber").contains(parentId);
	assertThat(target.children).extracting("identityNumber").contains(childId);
	assertThat(hierarchy.filesPerOntology.get(targetId)).contains(fi);
}
}