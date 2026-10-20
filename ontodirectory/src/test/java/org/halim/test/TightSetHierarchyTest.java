package org.halim.test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.halim.acs.ACS_Journal;
import org.halim.acse.ACSE_CoreBasic;
import org.halim.acse.structure.unary.TightSetHierarchyDirectedAcyclicIntGraphComponent;
import org.halim.nStructure.ontology.core.segments.manager.OntologyHierarchyLister;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.assertj.core.api.Assertions.*;

/**
 * OntologyHierarchyListerTest's scenarios ported to ACSE's tight-set hierarchy, and a seeded random
 * sequence run on both, the lister being the reference. Three differences are the ACS contract's and
 * accepted: a parent already held or implied is refused with false rather than an
 * OntologyHierarchyError, a cycle with an IllegalArgumentException, and the root lists no parent. A
 * fourth is skipped: the lister keeps an edge below the edited class that another path implies, and
 * the graph drops it.
 */
public class TightSetHierarchyTest {

private static final int ROOT = TightSetHierarchyDirectedAcyclicIntGraphComponent.ROOT;

private ACSE_CoreBasic core;
private TightSetHierarchyDirectedAcyclicIntGraphComponent graph;

@BeforeEach
public void setup() {
	core = new ACSE_CoreBasic();
	core.addArity("classes", null);
	graph = new TightSetHierarchyDirectedAcyclicIntGraphComponent(core, core.getJournal(), 0);
	core.addStructure(graph, "hierarchy", null);
	core.captureIdentity(0, null); // the root
}

/** Mints a fresh class id under the root, the arity's lowest free identity. */
private int newClass() {
	return (int) core.captureIdentity(0, null);
}

private int[] parentsOf(int element) {
	int[] parents = new int[graph.parentCount(element)];
	graph.copyParentsInto(element, parents, 0);
	return parents;
}

private int[] childrenOf(int element) {
	int[] children = new int[graph.childCount(element)];
	graph.copyChildrenInto(element, children, 0);
	return children;
}

@Test
public void testGetChildren_LeafClass_ReturnsEmptyNotNull() {
	int leaf = newClass();
	assertThat(childrenOf(leaf)).isEmpty();
}

@Test
public void testRemoveParent_NotAnActualParent_NoOp() {
	int c = newClass();
	int unrelated = newClass();

	assertThat(graph.removeParent(c, unrelated, null)).isFalse();

	assertThat(parentsOf(c)).containsExactly(ROOT);
}

@Test
public void testAddParent_FullReplacementShortcut_WiresForwardAndSeversOldLink() {
	int p1 = newClass();
	int p2 = newClass();
	graph.addParent(p2, p1, null); // p2 descends from p1: root -> p1 -> p2
	int c = newClass();
	graph.addParent(c, p1, null);

	assertThat(childrenOf(p1)).containsExactlyInAnyOrder(p2, c);

	graph.addParent(c, p2, null); // p1 is now redundant (p2 already implies it)

	assertThat(parentsOf(c)).containsExactly(p2);
	assertThat(childrenOf(p2)).containsExactly(c);
	assertThat(childrenOf(p1)).containsExactly(p2); // c severed from its old parent
}

@Test
public void testRemoveParent_MultiParent_DropsOnlyThatEdge() {
	int p1 = newClass();
	int p2 = newClass();
	int c = newClass();
	graph.addParent(c, p1, null);
	graph.addParent(c, p2, null); // c now has [p1, p2] -- unrelated siblings, neither implies the other

	assertThat(parentsOf(c)).containsExactlyInAnyOrder(p1, p2);

	graph.removeParent(c, p1, null);

	assertThat(parentsOf(c)).containsExactly(p2);
	assertThat(childrenOf(p1)).doesNotContain(c);
	assertThat(graph.hasParent(c, p2)).isTrue();
}

@Test
public void testRemoveParent_LastRemainingParent_ReanchorsToRoot() {
	int p1 = newClass();
	int p2 = newClass();
	int c = newClass();
	graph.addParent(c, p1, null);
	graph.addParent(c, p2, null);

	graph.removeParent(c, p1, null);
	assertThat(parentsOf(c)).containsExactly(p2);

	graph.removeParent(c, p2, null);

	assertThat(parentsOf(c)).containsExactly(ROOT);
	assertThat(graph.hasParent(c, ROOT)).isTrue();
}

@Test
public void testIsParent_MultiParent_CorrectForEveryParent() {
	int p1 = newClass();
	int p2 = newClass();
	int c = newClass();
	graph.addParent(c, p1, null);
	graph.addParent(c, p2, null);

	assertThat(graph.hasParent(c, p1)).isTrue();
	assertThat(graph.hasParent(c, p2)).isTrue();
	assertThat(graph.hasParent(c, ROOT)).isFalse();
	assertThat(graph.hasParent(c, 999)).isFalse();
}

@Test
public void testSetAutoTightenParents_CannotBeDisabledOnceOn() {
	// The graph has no policy to switch: it always tightens, so a parent that is already an ancestor is not added.
	int p = newClass();
	int q = newClass();
	int c = newClass();
	graph.addParent(q, p, null);
	graph.addParent(c, q, null);

	assertThat(graph.addParent(c, p, null)).isFalse();
	assertThat(parentsOf(c)).containsExactly(q);
}

@Test
public void testAddParent_AutoTighten_RejectsCycle() {
	int p = newClass();
	int c = newClass();
	graph.addParent(c, p, null);

	assertThatThrownBy(() -> graph.addParent(p, c, null))
		  .isInstanceOf(IllegalArgumentException.class);
}

@ParameterizedTest
@ValueSource(longs = {1, 2, 3, 4, 5, 6, 7, 8})
public void testRandomEdits_AgainstLister_SameAfterEveryStepAndUndo(long seed) {
	int classes = 12;
	OntologyHierarchyLister lister = new OntologyHierarchyLister();
	lister.registerClass(classes - 1);
	for(int i = 1; i < classes; i++) { newClass(); }
	ACS_Journal journal = core.getJournal();
	int start = journal.getCurrentActivityIndex();

	Random random = new Random(seed);
	StringBuilder log = new StringBuilder("seed " + seed + ":");
	for(int step = 0; step < 400; step++) {
		int c = 1 + random.nextInt(classes - 1);
		int kind = random.nextInt(20);
		if(kind < 10) {
			int p = 1 + random.nextInt(classes - 1);
			if(p == c) { continue; }
			boolean cycle = lister.isHeritage(c, p);
			boolean refused = cycle || lister.isAncestor(c, p);
			if(!refused && leavesImpliedEdgeBelow(lister, c, p)) { continue; }
			log.append(" add(").append(c).append(", ").append(p).append(')');
			if(refused) {
				assertThatThrownBy(() -> lister.addParent(c, p)).isInstanceOf(OntologyHierarchyLister.OntologyHierarchyError.class);
				if(cycle) { assertThatThrownBy(() -> graph.addParent(c, p, null)).as(log.toString()).isInstanceOf(IllegalArgumentException.class); }
				else { assertThat(graph.addParent(c, p, null)).as(log.toString()).isFalse(); }
			} else {
				lister.addParent(c, p);
				assertThat(graph.addParent(c, p, null)).as(log.toString()).isTrue();
			}
		} else if(kind < 17) {
			IntArrayList held = lister.getParents(c);
			int p = held.getInt(0) == ROOT ? 1 + random.nextInt(classes - 1) : held.getInt(random.nextInt(held.size()));
			log.append(" remove(").append(c).append(", ").append(p).append(')');
			boolean wasParent = lister.isParent(c, p);
			lister.removeParent(c, p);
			assertThat(graph.removeParent(c, p, null)).as(log.toString()).isEqualTo(wasParent);
		} else {
			log.append(" clear(").append(c).append(')');
			lister.clearParents(c);
			for(int p : parentsOf(c)) { if(p != ROOT) { graph.removeParent(c, p, null); } }
		}
		assertMatches(lister, classes, log.toString());
	}

	journal.undoUntilActivityIndex(start);
	OntologyHierarchyLister untouched = new OntologyHierarchyLister();
	untouched.registerClass(classes - 1);
	assertMatches(untouched, classes, log + " undone");
}

/**
 * Whether giving {@code c} the parent {@code p} leaves an edge below c that another path implies: a
 * class below c holding p, or an ancestor of p, as a direct parent. The lister keeps that edge, the
 * graph drops it.
 */
private static boolean leavesImpliedEdgeBelow(OntologyHierarchyLister lister, int c, int p) {
	for(int x : lister.getHeritage(c)) {
		for(int q : lister.getParents(x)) {
			if(q != ROOT && lister.isAncestor(p, q)) { return true; }
		}
	}
	return false;
}

/** Parents, children, ancestors and descendants of every class, as sets; the root's parents are left out, since the lister lists the root as its own. */
private void assertMatches(OntologyHierarchyLister lister, int classes, String log) {
	for(int x = 0; x < classes; x++) {
		IntArrayList ancestors = new IntArrayList(), descendants = new IntArrayList();
		for(int y = 0; y < classes; y++) {
			if(y == x) { continue; }
			if(graph.reaches(y, x)) { ancestors.add(y); }
			if(y != ROOT && graph.reaches(x, y)) { descendants.add(y); }
		}
		if(x != ROOT) {
			assertThat(parentsOf(x)).as("parents of %d after %s", x, log).containsExactlyInAnyOrder(lister.getParents(x).toIntArray());
			assertThat(ancestors.toIntArray()).as("ancestors of %d after %s", x, log).containsExactlyInAnyOrder(lister.getAncestors(x).toIntArray());
		}
		assertThat(childrenOf(x)).as("children of %d after %s", x, log).containsExactlyInAnyOrder(lister.getChildren(x).toIntArray());
		assertThat(descendants.toIntArray()).as("descendants of %d after %s", x, log).containsExactlyInAnyOrder(lister.getHeritage(x).toIntArray());
	}
}

}
