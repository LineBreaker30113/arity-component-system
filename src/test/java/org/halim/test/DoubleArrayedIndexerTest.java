package org.halim.test;

import org.halim.ontology.legacy.core.TransactionContext;
import org.halim.ontology.legacy.core.segments.manager.DoubleArrayedIndexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static org.assertj.core.api.Assertions.*;

public class DoubleArrayedIndexerTest {

private DoubleArrayedIndexer indexer;

@BeforeEach
public void setup() {
	indexer = new DoubleArrayedIndexer();
}

@Test
public void testEnsureConnection_ReflectsOnBothSides() {
	int classId = indexer.newClass();
	int elementId = indexer.newElement();

	indexer.ensureConnection(classId, elementId);

	assertThat(indexer.isConnected(classId, elementId)).isTrue();
	assertThat(indexer.getElementsOf(classId).toIntArray()).containsExactly(elementId);
	assertThat(indexer.getClassesOf(elementId).toIntArray()).containsExactly(classId);
	assertThat(indexer.getClassesOfAsBitSet(elementId).get(classId)).isTrue();
	assertThat(indexer.getElementsCount(classId)).isEqualTo(1);
	assertThat(indexer.getClassesCount(elementId)).isEqualTo(1);
}

@Test
public void testEnsureConnection_IsIdempotent() {
	int classId = indexer.newClass();
	int elementId = indexer.newElement();

	indexer.ensureConnection(classId, elementId);
	indexer.ensureConnection(classId, elementId);

	assertThat(indexer.getElementsOf(classId).toIntArray()).containsExactly(elementId);
	assertThat(indexer.getClassesOfAsBitSet(elementId).cardinality()).isEqualTo(1);
}

@Test
public void testBreakConnection_SeversBothSides() {
	int classId = indexer.newClass();
	int elementId = indexer.newElement();
	indexer.ensureConnection(classId, elementId);

	indexer.breakConnection(classId, elementId);

	assertThat(indexer.isConnected(classId, elementId)).isFalse();
	assertThat(indexer.getElementsOf(classId).toIntArray()).isEmpty();
	assertThat(indexer.getClassesOfAsBitSet(elementId).isEmpty()).isTrue();
}

@Test
public void testDeleteElement_SeversReverseLinksOnClasses() {
	int classA = indexer.newClass();
	int classB = indexer.newClass();
	int elementId = indexer.newElement();
	indexer.ensureConnection(classA, elementId);
	indexer.ensureConnection(classB, elementId);

	indexer.deleteElement(elementId);

	assertThat(indexer.getElementsOf(classA).toIntArray()).doesNotContain(elementId);
	assertThat(indexer.getElementsOf(classB).toIntArray()).doesNotContain(elementId);
}

@Test
public void testDeleteClass_SeversReverseLinksOnElements() {
	int classId = indexer.newClass();
	int elementA = indexer.newElement();
	int elementB = indexer.newElement();
	indexer.ensureConnection(classId, elementA);
	indexer.ensureConnection(classId, elementB);

	indexer.deleteClass(classId);

	assertThat(indexer.getClassesOfAsBitSet(elementA).isEmpty()).isTrue();
	assertThat(indexer.getClassesOfAsBitSet(elementB).isEmpty()).isTrue();
}

@Test
public void testDeleteElement_ThenUndo_RestoresConnections() {
	int classId = indexer.newClass();
	int elementId = indexer.newElement();
	indexer.ensureConnection(classId, elementId);

	TransactionContext.begin("delete-element");
	indexer.deleteElement(elementId);
	TransactionContext.Transaction tx = TransactionContext.commit();

	assertThat(indexer.isConnected(classId, elementId)).isFalse();

	tx.undo();

	assertThat(indexer.isConnected(classId, elementId)).isTrue();
	assertThat(indexer.getElementsOf(classId).toIntArray()).containsExactly(elementId);
}

@Test
public void testBulkDeleteClass_SeversAllReverseLinks() {
	int classA = indexer.newClass();
	int classB = indexer.newClass();
	int elementId = indexer.newElement();
	indexer.ensureConnection(classA, elementId);
	indexer.ensureConnection(classB, elementId);

	indexer.deleteClass(new int[]{classA, classB});

	assertThat(indexer.getClassesOfAsBitSet(elementId).isEmpty()).isTrue();
}

@Test
public void testIdReclamation_ReusesTombstonedClassSlot() {
	int classA = indexer.newClass();
	indexer.deleteClass(classA);

	int classB = indexer.newClass();

	assertThat(classB).isEqualTo(classA);
	assertThat(indexer.getElementsOf(classB).toIntArray()).isEmpty();
}

@Test
public void testGetClassesForElements_PreservesTombstoneAsNull() {
	int classId = indexer.newClass();
	int elementA = indexer.newElement();
	int elementB = indexer.newElement();
	indexer.ensureConnection(classId, elementA);
	indexer.ensureConnection(classId, elementB);

	indexer.deleteElement(elementA);

	var snapshot = indexer.getClassesForElements();

	assertThat((Object) snapshot.get(elementA)).isNull();
	assertThat(snapshot.get(elementB).toIntArray()).containsExactly(classId);
}

@Test
public void testGetElementsForClasses_ReturnsDefensiveCopy() {
	int classId = indexer.newClass();
	int elementId = indexer.newElement();
	indexer.ensureConnection(classId, elementId);

	var snapshot = indexer.getElementsForClasses();
	snapshot.get(classId).clear();

	assertThat(indexer.getElementsOf(classId).toIntArray()).containsExactly(elementId);
}

@Test
public void testGetClassesOfAsBitSet_ReturnsLiveView() {
	int classId = indexer.newClass();
	int elementId = indexer.newElement();
	indexer.ensureConnection(classId, elementId);

	BitSet bits = indexer.getClassesOfAsBitSet(elementId);

	assertThat(bits.get(classId)).isTrue();
}

}
