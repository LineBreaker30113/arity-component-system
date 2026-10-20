package org.halim.test;

import org.halim.nStructure.ontology.core.segmentation.query.OntologyClassFilter;
import org.halim.nStructure.ontology.core.segmentation.query.OntologyElementFilter;
import org.halim.nStructure.ontology.core.segments.data.*;
import org.halim.nStructure.ontology.core.segments.manager.DoubleArrayedIndexer;
import org.halim.nStructure.ontology.core.segments.manager.OntologyHierarchyLister;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class OntologyFilterResolverTest {

private static final int ROOT = 0;

private OntologyHierarchyLister hierarchy;
private DoubleArrayedIndexer indexer;
private OntologyClassCatalog classCatalog;
private OntologyElementCatalog elementCatalog;
private OntologyFilterResolverSegmentation resolver;

@BeforeEach
public void setup() {
	hierarchy = new OntologyHierarchyLister();
	indexer = new DoubleArrayedIndexer();
	classCatalog = new OntologyClassCatalog(hierarchy, indexer);
	elementCatalog = new OntologyElementCatalog(hierarchy, indexer);
	OntologyAnalyticsCalculator analytics = new OntologyAnalyticsCalculator(hierarchy, indexer);
	resolver = new OntologyFilterResolverSegmentation(hierarchy, indexer, analytics, classCatalog, elementCatalog);

	// DoubleArrayedIndexer has no reserved root id (its first newClass() returns 0), while
	// OntologyHierarchyLister permanently reserves id 0 for root. Consume the indexer's id 0
	// here so both engines' identity spaces line up for every class minted afterward.
	indexer.newClass();
}

/**
 * Mints a class id registered on both engines the way {@code OntologyHierarchyManagementServiceImpl
 * #createClass} does it -- {@code newClass()} for the classification side, {@code registerClass} for
 * the hierarchy side. Using the real API (rather than poking the hierarchy's internal parent array)
 * matters because {@code registerClass} also updates root's reverse children index, so classes minted
 * here are visible to root-down traversal and to the universe scan.
 */
private int newClass() {
	int id = indexer.newClass();
	hierarchy.registerClass(id);
	return id;
}

// --- Class filter leaves ---

@Test
public void testChildOf_ReturnsDirectChildrenOnly() {
	int parent = newClass();
	int child1 = newClass();
	int child2 = newClass();
	int grandchild = newClass();
	hierarchy.addParent(child1, parent);
	hierarchy.addParent(child2, parent);
	hierarchy.addParent(grandchild, child1);

	assertThat(resolver.getAllClasses(new OntologyClassFilter.ChildOf(parent)))
		  .containsExactlyInAnyOrder(child1, child2);
}

@Test
public void testDescendantOf_ReturnsAllDescendantsNotSelf() {
	int grandparent = newClass();
	int parent = newClass();
	int child = newClass();
	hierarchy.addParent(parent, grandparent);
	hierarchy.addParent(child, parent);

	assertThat(resolver.getAllClasses(new OntologyClassFilter.DescendantOf(grandparent)))
		  .containsExactlyInAnyOrder(parent, child);
}

@Test
public void testParentOf_ReturnsDirectParentOnly() {
	int parent = newClass();
	int child = newClass();
	hierarchy.addParent(child, parent);

	assertThat(resolver.getAllClasses(new OntologyClassFilter.ParentOf(child)))
		  .containsExactly(parent);
}

@Test
public void testAncestorOf_ReturnsFullChainIncludingRoot() {
	int grandparent = newClass();
	int parent = newClass();
	int child = newClass();
	hierarchy.addParent(parent, grandparent);
	hierarchy.addParent(child, parent);

	assertThat(resolver.getAllClasses(new OntologyClassFilter.AncestorOf(child)))
		  .containsExactlyInAnyOrder(parent, grandparent, ROOT);
}

// --- Class filter combinators: a/b share child y, each also has a child of their own ---

@Test
public void testClassAnd_ReturnsIntersection() {
	int a = newClass(), b = newClass(), x = newClass(), y = newClass(), z = newClass();
	hierarchy.addParent(x, a);
	hierarchy.addParent(y, a);
	hierarchy.addParent(y, b);
	hierarchy.addParent(z, b);

	OntologyClassFilter filter = new OntologyClassFilter.And(new OntologyClassFilter.ChildOf(a), new OntologyClassFilter.ChildOf(b));
	assertThat(resolver.getAllClasses(filter)).containsExactly(y);
}

@Test
public void testClassOr_ReturnsUnionWithoutDuplicatingSharedChild() {
	int a = newClass(), b = newClass(), x = newClass(), y = newClass(), z = newClass();
	hierarchy.addParent(x, a);
	hierarchy.addParent(y, a);
	hierarchy.addParent(y, b);
	hierarchy.addParent(z, b);

	OntologyClassFilter filter = new OntologyClassFilter.Or(new OntologyClassFilter.ChildOf(a), new OntologyClassFilter.ChildOf(b));
	assertThat(resolver.getAllClasses(filter)).containsExactlyInAnyOrder(x, y, z);
}

@Test
public void testClassSubtract_RemovesOtherSidesMatches() {
	int a = newClass(), b = newClass(), x = newClass(), y = newClass(), z = newClass();
	hierarchy.addParent(x, a);
	hierarchy.addParent(y, a);
	hierarchy.addParent(y, b);
	hierarchy.addParent(z, b);

	OntologyClassFilter filter = new OntologyClassFilter.Subtract(new OntologyClassFilter.ChildOf(a), new OntologyClassFilter.ChildOf(b));
	assertThat(resolver.getAllClasses(filter)).containsExactly(x);
}

@Test
public void testClassNameContaining_UnderAnd_NarrowsByCaseInsensitiveSubstring() {
	int parent = newClass();
	int reports = newClass();
	int archive = newClass();
	hierarchy.addParent(reports, parent);
	hierarchy.addParent(archive, parent);
	classCatalog.setName(reports, "Quarterly Reports");
	classCatalog.setName(archive, "Archive");

	OntologyClassFilter filter = new OntologyClassFilter.And(
		  new OntologyClassFilter.ChildOf(parent), new OntologyClassFilter.NameContaining("report"));
	assertThat(resolver.getAllClasses(filter)).containsExactly(reports);
}

// --- Element filter leaves: p has an element of its own, plus a child c with its own element ---

@Test
public void testDirectElementOf_ExcludesDescendantClassElements() {
	int p = newClass();
	int c = newClass();
	hierarchy.addParent(c, p);
	int onP = indexer.newElement();
	int onC = indexer.newElement();
	indexer.ensureConnection(p, onP);
	indexer.ensureConnection(c, onC);

	assertThat(resolver.getAllElements(new OntologyElementFilter.DirectElementOf(p))).containsExactly(onP);
}

@Test
public void testUnderDomain_IncludesSelfAndDescendantClassElements() {
	int p = newClass();
	int c = newClass();
	hierarchy.addParent(c, p);
	int onP = indexer.newElement();
	int onC = indexer.newElement();
	indexer.ensureConnection(p, onP);
	indexer.ensureConnection(c, onC);

	assertThat(resolver.getAllElements(new OntologyElementFilter.UnderDomain(p))).containsExactlyInAnyOrder(onP, onC);
}

// --- Element filter combinators: m/n share one element, each also tags one element of its own ---

@Test
public void testElementAnd_ReturnsIntersection() {
	int m = newClass(), n = newClass();
	int shared = indexer.newElement(), mOnly = indexer.newElement(), nOnly = indexer.newElement();
	indexer.ensureConnection(m, shared);
	indexer.ensureConnection(n, shared);
	indexer.ensureConnection(m, mOnly);
	indexer.ensureConnection(n, nOnly);

	OntologyElementFilter filter = new OntologyElementFilter.And(
		  new OntologyElementFilter.DirectElementOf(m), new OntologyElementFilter.DirectElementOf(n));
	assertThat(resolver.getAllElements(filter)).containsExactly(shared);
}

@Test
public void testElementOr_ReturnsUnionWithoutDuplicatingSharedElement() {
	int m = newClass(), n = newClass();
	int shared = indexer.newElement(), mOnly = indexer.newElement(), nOnly = indexer.newElement();
	indexer.ensureConnection(m, shared);
	indexer.ensureConnection(n, shared);
	indexer.ensureConnection(m, mOnly);
	indexer.ensureConnection(n, nOnly);

	OntologyElementFilter filter = new OntologyElementFilter.Or(
		  new OntologyElementFilter.DirectElementOf(m), new OntologyElementFilter.DirectElementOf(n));
	assertThat(resolver.getAllElements(filter)).containsExactlyInAnyOrder(shared, mOnly, nOnly);
}

@Test
public void testElementSubtract_RemovesOtherSidesMatches() {
	int m = newClass(), n = newClass();
	int shared = indexer.newElement(), mOnly = indexer.newElement(), nOnly = indexer.newElement();
	indexer.ensureConnection(m, shared);
	indexer.ensureConnection(n, shared);
	indexer.ensureConnection(m, mOnly);
	indexer.ensureConnection(n, nOnly);

	OntologyElementFilter filter = new OntologyElementFilter.Subtract(
		  new OntologyElementFilter.DirectElementOf(m), new OntologyElementFilter.DirectElementOf(n));
	assertThat(resolver.getAllElements(filter)).containsExactly(mOnly);
}

@Test
public void testElementNameContaining_UnderAnd_NarrowsByCaseInsensitiveSubstring() {
	int m = newClass();
	int report = indexer.newElement();
	int notes = indexer.newElement();
	indexer.ensureConnection(m, report);
	indexer.ensureConnection(m, notes);
	elementCatalog.setName(report, "report.pdf");
	elementCatalog.setName(notes, "notes.txt");

	OntologyElementFilter filter = new OntologyElementFilter.And(
		  new OntologyElementFilter.DirectElementOf(m), new OntologyElementFilter.NameContaining("report"));
	assertThat(resolver.getAllElements(filter)).containsExactly(report);
}

// --- forEach*Satisfying: same resolution, callback-driven instead of array-returning ---

@Test
public void testForEachClassSatisfying_VisitsSameClassesAsGetAllClasses() {
	int parent = newClass();
	int child1 = newClass();
	int child2 = newClass();
	hierarchy.addParent(child1, parent);
	hierarchy.addParent(child2, parent);

	java.util.List<Integer> visited = new java.util.ArrayList<>();
	resolver.forEachClassSatisfying(new OntologyClassFilter.ChildOf(parent), visited::add);
	assertThat(visited).containsExactlyInAnyOrder(child1, child2);
}

@Test
public void testForEachElementSatisfying_VisitsSameElementsAsGetAllElements() {
	int p = newClass();
	int e1 = indexer.newElement();
	int e2 = indexer.newElement();
	indexer.ensureConnection(p, e1);
	indexer.ensureConnection(p, e2);

	java.util.List<Integer> visited = new java.util.ArrayList<>();
	resolver.forEachElementSatisfying(new OntologyElementFilter.DirectElementOf(p), visited::add);
	assertThat(visited).containsExactlyInAnyOrder(e1, e2);
}

// --- Native test / enumerate agreement (docs/CUSTOM-FILTERING-SPEC.md §8.5) ---
//
// Each leaf must give the same answer whichever direction the resolver takes, and the direction is
// not the caller's to choose -- it follows from the shape of the surrounding tree. Wrapping a leaf in
// Or(leaf, <test-preferring>) is what forces the test direction: Or can only be enumerated when both
// sides can, so the whole tree falls back to the universe scan and every candidate is judged by the
// leaf's test(). Comparing that against the leaf resolved alone compares test against enumerate.
//
// For ChildOf, ParentOf, DirectElementOf and NameContaining the two directions are genuinely separate
// code paths, so agreement is a real assertion. For the ancestry-shaped leaves the resolver currently
// answers test() by membership in the same memoized enumeration, so agreement holds by construction --
// these cases are a regression guard for the day someone wires isAncestor/isHeritage in as native
// tests, which is exactly where findings §1's reflexivity trap lives.

/** Forces {@code leaf} to be resolved through its test direction. */
private static OntologyClassFilter throughTest(OntologyClassFilter leaf) {
	return new OntologyClassFilter.Or(leaf, new OntologyClassFilter.Custom(candidate -> false));
}

private static OntologyElementFilter throughTest(OntologyElementFilter leaf) {
	return new OntologyElementFilter.Or(leaf, new OntologyElementFilter.Custom(candidate -> false));
}

private void assertDirectionsAgree(OntologyClassFilter leaf) {
	assertThat(resolver.getAllClasses(throughTest(leaf)))
		  .as("test direction must agree with enumerate for %s", leaf)
		  .containsExactlyInAnyOrder(resolver.getAllClasses(leaf));
}

private void assertDirectionsAgree(OntologyElementFilter leaf) {
	assertThat(resolver.getAllElements(throughTest(leaf)))
		  .as("test direction must agree with enumerate for %s", leaf)
		  .containsExactlyInAnyOrder(resolver.getAllElements(leaf));
}

/** A diamond plus a class left directly under root, so ancestry is genuinely branching rather than a
 *  single chain. Returns every class in the fixture, root included. */
private int[] buildDiamond() {
	int top = newClass();
	int left = newClass();
	int right = newClass();
	int bottom = newClass();
	int rootChild = newClass();
	hierarchy.addParent(left, top);
	hierarchy.addParent(right, top);
	hierarchy.addParent(bottom, left);
	hierarchy.addParent(bottom, right);
	classCatalog.setName(top, "Top");
	classCatalog.setName(left, "Left branch");
	classCatalog.setName(right, "Right branch");
	classCatalog.setName(bottom, "Bottom");
	classCatalog.setName(rootChild, "Loose");
	return new int[]{ROOT, top, left, right, bottom, rootChild};
}

@Test
public void testEveryClassLeaf_BothDirectionsAgree_ForEveryAnchorInTheGraph() {
	for (int anchor : buildDiamond()) {
		assertDirectionsAgree(new OntologyClassFilter.ChildOf(anchor));
		assertDirectionsAgree(new OntologyClassFilter.ParentOf(anchor));
		assertDirectionsAgree(new OntologyClassFilter.DescendantOf(anchor));
		assertDirectionsAgree(new OntologyClassFilter.AncestorOf(anchor));
		assertDirectionsAgree(new OntologyClassFilter.DomainOf(anchor));
	}
}

@Test
public void testClassNameContaining_BothDirectionsAgree() {
	buildDiamond();
	assertDirectionsAgree(new OntologyClassFilter.And(
		  new OntologyClassFilter.DomainOf(ROOT), new OntologyClassFilter.NameContaining("branch")));
	assertDirectionsAgree(new OntologyClassFilter.And(
		  new OntologyClassFilter.DomainOf(ROOT), new OntologyClassFilter.NameContaining("no-such-name")));
}

@Test
public void testEveryElementLeaf_BothDirectionsAgree() {
	int parent = newClass();
	int child = newClass();
	hierarchy.addParent(child, parent);
	int onParent = indexer.newElement();
	int onChild = indexer.newElement();
	int onBoth = indexer.newElement();
	int unclassified = indexer.newElement();
	indexer.ensureConnection(parent, onParent);
	indexer.ensureConnection(child, onChild);
	indexer.ensureConnection(parent, onBoth);
	indexer.ensureConnection(child, onBoth);
	elementCatalog.setName(onParent, "report.pdf");
	elementCatalog.setName(onChild, "report.txt");
	elementCatalog.setName(onBoth, "notes.md");
	elementCatalog.setName(unclassified, "orphan.bin");

	for (int anchor : new int[]{ROOT, parent, child}) {
		assertDirectionsAgree(new OntologyElementFilter.DirectElementOf(anchor));
		assertDirectionsAgree(new OntologyElementFilter.UnderDomain(anchor));
	}
	assertDirectionsAgree(new OntologyElementFilter.And(
		  new OntologyElementFilter.UnderDomain(parent), new OntologyElementFilter.NameContaining("report")));
}

/**
 * The self-identity candidate, asserted by value rather than by agreement -- findings §1 records a real
 * trap here: {@code isAncestor(X, X)} and {@code isHeritage(X, X)} are {@code true}, while
 * {@code getAncestors(X)}/{@code getHeritage(X)} exclude {@code X}. So a native test wired straight
 * onto those predicates would disagree with enumeration on exactly one candidate, and only that one.
 * {@code DomainOf} is the leaf that genuinely wants the reflexivity.
 */
@Test
public void testSelfIdentityCandidate_IsIncludedOnlyByDomainOf() {
	int top = newClass();
	int under = newClass();
	hierarchy.addParent(under, top);

	assertThat(resolver.getAllClasses(new OntologyClassFilter.DescendantOf(top))).doesNotContain(top);
	assertThat(resolver.getAllClasses(throughTest(new OntologyClassFilter.DescendantOf(top)))).doesNotContain(top);

	assertThat(resolver.getAllClasses(new OntologyClassFilter.AncestorOf(top))).doesNotContain(top);
	assertThat(resolver.getAllClasses(throughTest(new OntologyClassFilter.AncestorOf(top)))).doesNotContain(top);

	assertThat(resolver.getAllClasses(new OntologyClassFilter.ChildOf(top))).doesNotContain(top);
	assertThat(resolver.getAllClasses(throughTest(new OntologyClassFilter.ChildOf(top)))).doesNotContain(top);

	assertThat(resolver.getAllClasses(new OntologyClassFilter.ParentOf(top))).doesNotContain(top);
	assertThat(resolver.getAllClasses(throughTest(new OntologyClassFilter.ParentOf(top)))).doesNotContain(top);

	assertThat(resolver.getAllClasses(new OntologyClassFilter.DomainOf(top)))
		  .as("DomainOf is the self-inclusive leaf -- it must contain its own anchor")
		  .contains(top);
	assertThat(resolver.getAllClasses(throughTest(new OntologyClassFilter.DomainOf(top)))).contains(top);

	assertThat(under).isNotNegative();
}

/** {@code DomainOf(X)} is exactly {@code DescendantOf(X)} plus {@code X} -- the one difference between
 *  the two leaves, pinned in both directions. */
@Test
public void testDomainOf_IsDescendantOfPlusSelf() {
	int top = newClass();
	int a = newClass();
	int b = newClass();
	hierarchy.addParent(a, top);
	hierarchy.addParent(b, a);

	assertThat(resolver.getAllClasses(new OntologyClassFilter.DescendantOf(top))).containsExactlyInAnyOrder(a, b);
	assertThat(resolver.getAllClasses(new OntologyClassFilter.DomainOf(top))).containsExactlyInAnyOrder(top, a, b);
	assertThat(resolver.getAllClasses(throughTest(new OntologyClassFilter.DomainOf(top)))).containsExactlyInAnyOrder(top, a, b);
}

/** A bare name predicate is legal now that leaves carry no base -- it searches everything, which is
 *  what the universe fallback exists for. */
@Test
public void testBareNameContaining_SearchesTheWholeUniverse() {
	int parent = newClass();
	int reports = newClass();
	hierarchy.addParent(reports, parent);
	classCatalog.setName(reports, "Quarterly Reports");
	classCatalog.setName(parent, "Archive");

	assertThat(resolver.getAllClasses(new OntologyClassFilter.NameContaining("report"))).containsExactly(reports);

	int tagged = indexer.newElement();
	int other = indexer.newElement();
	indexer.ensureConnection(reports, tagged);
	indexer.ensureConnection(parent, other);
	elementCatalog.setName(tagged, "q1-report.pdf");
	elementCatalog.setName(other, "notes.txt");

	assertThat(resolver.getAllElements(new OntologyElementFilter.NameContaining("REPORT"))).containsExactly(tagged);
}

}
