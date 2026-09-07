package org.halim.test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.halim.ontology.legacy.core.segments.manager.OntologyHierarchyLister;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.*;

public class OntologyHierarchyListerTest {

private static final int ROOT = 0;

private OntologyHierarchyLister lister;
private IntArrayList classesField;

@BeforeEach
public void setup() throws Exception {
	lister = new OntologyHierarchyLister();
	Field f = OntologyHierarchyLister.class.getDeclaredField("classes");
	f.setAccessible(true);
	classesField = (IntArrayList) f.get(lister);
}

/** Mints a fresh class id anchored to root, standing in for a composition root registering a new class. */
private int newClass() {
	classesField.add(ROOT);
	return classesField.size() - 1;
}

@Test
public void testGetChildren_LeafClass_ReturnsEmptyNotNull() {
	int leaf = newClass();
	IntArrayList children = lister.getChildren(leaf);
	assertThat((Object) children).isNotNull();
	assertThat(children.toIntArray()).isEmpty();
}

@Test
public void testRemoveParent_NotAnActualParent_NoOp() {
	int c = newClass();
	int unrelated = newClass();

	lister.removeParent(c, unrelated);

	assertThat(lister.getParents(c).toIntArray()).containsExactly(ROOT);
}

@Test
public void testAddParent_FullReplacementShortcut_WiresForwardAndSeversOldLink() {
	int p1 = newClass();
	int p2 = newClass();
	lister.addParent(p2, p1); // p2 descends from p1: root -> p1 -> p2
	int c = newClass();
	lister.addParent(c, p1); // c's root parent fully subsumed by p1 -> "full replacement" shortcut

	assertThat(lister.getChildren(p1).toIntArray()).containsExactlyInAnyOrder(p2, c);

	lister.addParent(c, p2); // p1 is now redundant (p2 already implies it) -> shortcut fires again

	assertThat(lister.getParents(c).toIntArray()).containsExactly(p2);
	assertThat(lister.getChildren(p2).toIntArray()).containsExactly(c);
	assertThat(lister.getChildren(p1).toIntArray()).containsExactly(p2); // c severed from its old parent
}

@Test
public void testRemoveParent_MultiParent_DropsOnlyThatEdge() {
	int p1 = newClass();
	int p2 = newClass();
	int c = newClass();
	lister.addParent(c, p1); // auto-tighten prunes root immediately (p1 already implies it)
	lister.addParent(c, p2); // c now has [p1, p2] -- unrelated siblings, neither implies the other

	assertThat(lister.getParents(c).toIntArray()).containsExactlyInAnyOrder(p1, p2);

	lister.removeParent(c, p1);

	assertThat(lister.getParents(c).toIntArray()).containsExactly(p2);
	assertThat(lister.getChildren(p1).toIntArray()).doesNotContain(c);
	assertThat(lister.isParent(c, p2)).isTrue();
}

@Test
public void testRemoveParent_LastRemainingParent_ReanchorsToRoot() {
	int p1 = newClass();
	int p2 = newClass();
	int c = newClass();
	lister.addParent(c, p1); // c=[p1] (root pruned by auto-tighten)
	lister.addParent(c, p2); // c=[p1, p2], promoted to multi-parent slab

	lister.removeParent(c, p1); // down to [p2], still multi-parent slab
	assertThat(lister.getParents(c).toIntArray()).containsExactly(p2);

	lister.removeParent(c, p2); // drop the last remaining parent -> exercises clearParentsTo's fixed multi-branch

	assertThat(lister.getParents(c).toIntArray()).containsExactly(ROOT);
	assertThat(lister.isParent(c, ROOT)).isTrue();
}

@Test
public void testIsParent_MultiParent_CorrectForEveryParent() {
	int p1 = newClass();
	int p2 = newClass();
	int c = newClass();
	lister.addParent(c, p1);
	lister.addParent(c, p2);

	assertThat(lister.isParent(c, p1)).isTrue();
	assertThat(lister.isParent(c, p2)).isTrue();
	assertThat(lister.isParent(c, ROOT)).isFalse(); // root was pruned once p1 subsumed it
	assertThat(lister.isParent(c, 999)).isFalse();
}

@Test
public void testSetAutoTightenParents_CannotBeDisabledOnceOn() {
	// autoTighten defaults to true and is combined with OR, so it can only ever turn on, never off.
	assertThat(lister.getAutoTightenParentsPolicy()).isTrue();
	lister.setAutoTightenParents(false);
	assertThat(lister.getAutoTightenParentsPolicy()).isTrue();
}

@Test
public void testAddParent_AutoTighten_RejectsCycle() {
	int p = newClass();
	int c = newClass();
	lister.addParent(c, p);

	assertThatThrownBy(() -> lister.addParent(p, c))
		  .isInstanceOf(OntologyHierarchyLister.OntologyHierarchyError.class);
}

}
