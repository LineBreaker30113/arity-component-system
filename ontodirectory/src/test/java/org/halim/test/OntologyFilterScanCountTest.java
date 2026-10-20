package org.halim.test;

import org.halim.nStructure.ontology.core.segmentation.command.OntologyClassificationSegmentation;
import org.halim.nStructure.ontology.core.segmentation.command.OntologyHierarchySegmentation;
import org.halim.nStructure.ontology.core.segmentation.query.OntologyAnalyticsSegmentation;
import org.halim.nStructure.ontology.core.segmentation.query.OntologyClassFilter;
import org.halim.nStructure.ontology.core.segmentation.query.OntologyElementFilter;
import org.halim.nStructure.ontology.core.segments.data.OntologyAnalyticsCalculator;
import org.halim.nStructure.ontology.core.segments.data.OntologyClassCatalog;
import org.halim.nStructure.ontology.core.segments.data.OntologyElementCatalog;
import org.halim.nStructure.ontology.core.segments.data.OntologyFilterResolverSegmentation;
import org.halim.nStructure.ontology.core.segments.manager.DoubleArrayedIndexer;
import org.halim.nStructure.ontology.core.segments.manager.OntologyHierarchyLister;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the constraint the whole design exists to satisfy: <b>the entirety of the classes -- or of
 * the elements, never both -- is walked at most once per resolution. If any part of the tree can
 * anchor an enumeration, not even once.</b>
 * <p>
 * The previous attempt at this feature (branch {@code rejected/nstructure-custom-filtering}) claimed
 * this guarantee and wrote tests for it, and the tests missed a real violation: they only ever
 * exercised the fallback path, so a broad leaf repeated across several branches of one tree -- which
 * walked the hierarchy once per occurrence -- went unnoticed. {@link #testBroadLeafInTwoBranches_SharedInstance_WalksOnce}
 * and {@link #testBroadLeafInTwoBranches_EqualButDistinctInstances_WalksOnce} are that missing case.
 * <p>
 * Counting is done with {@link Proxy} rather than a mocking framework -- the project has none
 * ({@code pom.xml}: JUnit Jupiter + AssertJ) and the engine already builds its own proxy-based AOP in
 * {@code core.OntologyServiceProxy}, so this adds no dependency. Counting starts only once the fixture
 * graph is built ({@link #startCounting()}), so setup traffic never inflates a count.
 */
public class OntologyFilterScanCountTest {

private static final int ROOT = 0;

/** Counts calls by method name and by name-with-first-argument, so a test can distinguish
 *  "getHeritage was called" from "getHeritage was called for this particular class". */
private static final class CallCounter implements InvocationHandler {
	private final Object target;
	private final Map<String, Integer> counts = new HashMap<>();
	private boolean recording = false;

	private CallCounter(Object target) { this.target = target; }

	@Override
	public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
		if (recording) {
			counts.merge(method.getName(), 1, Integer::sum);
			if (args != null && args.length > 0) counts.merge(method.getName() + "(" + args[0] + ")", 1, Integer::sum);
		}
		try {
			return method.invoke(target, args);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

	private int count(String key) { return counts.getOrDefault(key, 0); }
}

private OntologyHierarchyLister realHierarchy;
private DoubleArrayedIndexer realIndexer;
private CallCounter hierarchyCounter, classificationCounter, analyticsCounter;
private OntologyFilterResolverSegmentation resolver;

@BeforeEach
public void setup() {
	realHierarchy = new OntologyHierarchyLister();
	realIndexer = new DoubleArrayedIndexer();
	realIndexer.newClass(); // burn the indexer's id 0; the hierarchy permanently reserves it for root

	hierarchyCounter = new CallCounter(realHierarchy);
	classificationCounter = new CallCounter(realIndexer);
	OntologyHierarchySegmentation hierarchy = (OntologyHierarchySegmentation) Proxy.newProxyInstance(
		  OntologyHierarchySegmentation.class.getClassLoader(),
		  new Class<?>[]{OntologyHierarchySegmentation.class}, hierarchyCounter);
	OntologyClassificationSegmentation classification = (OntologyClassificationSegmentation) Proxy.newProxyInstance(
		  OntologyClassificationSegmentation.class.getClassLoader(),
		  new Class<?>[]{OntologyClassificationSegmentation.class}, classificationCounter);

	analyticsCounter = new CallCounter(new OntologyAnalyticsCalculator(hierarchy, classification));
	OntologyAnalyticsSegmentation analytics = (OntologyAnalyticsSegmentation) Proxy.newProxyInstance(
		  OntologyAnalyticsSegmentation.class.getClassLoader(),
		  new Class<?>[]{OntologyAnalyticsSegmentation.class}, analyticsCounter);

	resolver = new OntologyFilterResolverSegmentation(hierarchy, classification, analytics,
		  new OntologyClassCatalog(hierarchy, classification), new OntologyElementCatalog(hierarchy, classification));
}

private int newClass() {
	int id = realIndexer.newClass();
	realHierarchy.registerClass(id);
	return id;
}

private int newElement(int... classIdentities) {
	int id = realIndexer.newElement();
	for (int classIdentity : classIdentities) realIndexer.ensureConnection(classIdentity, id);
	return id;
}

/** Begins counting, after the fixture graph is built. */
private void startCounting() {
	hierarchyCounter.recording = true;
	classificationCounter.recording = true;
	analyticsCounter.recording = true;
}

/** How many times the class universe was materialized. {@code universeClasses()} asks for the last
 *  class index exactly once per materialization, so this counts materializations, not iterations. */
private int classUniverseScans() { return classificationCounter.count("getLastClassIndex"); }

private int elementUniverseScans() { return classificationCounter.count("getLastElementIndex"); }

// --- 1. Avoidance: an enumerate-anchored tree never materializes the universe at all ---

@Test
public void testEnumerateAnchoredClassTree_NeverMaterializesTheUniverse() {
	int parent = newClass();
	int child = newClass();
	int other = newClass();
	realHierarchy.addParent(child, parent);
	startCounting();

	assertThat(resolver.getAllClasses(new OntologyClassFilter.ChildOf(parent))).containsExactly(child);
	assertThat(classUniverseScans()).isZero();
	assertThat(other).isNotNegative(); // fixture noise, kept so the universe is non-trivial
}

@Test
public void testEnumerateAnchoredElementTree_NeverMaterializesTheUniverse() {
	int tag = newClass();
	int tagged = newElement(tag);
	newElement(ROOT);
	startCounting();

	assertThat(resolver.getAllElements(new OntologyElementFilter.UnderDomain(tag))).containsExactly(tagged);
	assertThat(elementUniverseScans()).isZero();
}

/** Even when only one side of the tree can anchor: {@code And} routes the enumeration to whichever
 *  child prefers it and uses the other purely as a test, so no scan is needed. */
@Test
public void testAndWithOneTestOnlySide_NeverMaterializesTheUniverse() {
	int parent = newClass();
	int wanted = newClass();
	int unwanted = newClass();
	realHierarchy.addParent(wanted, parent);
	realHierarchy.addParent(unwanted, parent);
	startCounting();

	OntologyClassFilter mixed = new OntologyClassFilter.And(
		  new OntologyClassFilter.ChildOf(parent),
		  new OntologyClassFilter.Custom(candidate -> candidate == wanted));
	assertThat(resolver.getAllClasses(mixed)).containsExactly(wanted);
	assertThat(classUniverseScans()).isZero();
}

// --- 2. Fallback: a tree that prefers testing all the way up scans exactly once ---

@Test
public void testFullyTestOnlyClassTree_MaterializesTheUniverseExactlyOnce() {
	int a = newClass();
	int b = newClass();
	newClass();
	startCounting();

	OntologyClassFilter testOnly = new OntologyClassFilter.Or(
		  new OntologyClassFilter.Custom(candidate -> candidate == a),
		  new OntologyClassFilter.Custom(candidate -> candidate == b));
	assertThat(resolver.getAllClasses(testOnly)).containsExactlyInAnyOrder(a, b);
	assertThat(classUniverseScans()).isEqualTo(1);
}

@Test
public void testFullyTestOnlyElementTree_MaterializesTheUniverseExactlyOnce() {
	int tag = newClass();
	int report = newElement(tag);
	newElement(tag);
	startCounting();

	assertThat(resolver.getAllElements(new OntologyElementFilter.Custom(candidate -> candidate == report)))
		  .containsExactly(report);
	assertThat(elementUniverseScans()).isEqualTo(1);
}

/** An {@code Or} with one test-only side cannot anchor, so the whole tree falls back -- but the
 *  enumerate-preferring side is then used through its native test and never enumerated either. */
@Test
public void testOrWithOneTestOnlySide_ScansOnce_AndDoesNotAlsoEnumerateTheOtherSide() {
	int parent = newClass();
	int child = newClass();
	int loner = newClass();
	realHierarchy.addParent(child, parent);
	startCounting();

	OntologyClassFilter mixedOr = new OntologyClassFilter.Or(
		  new OntologyClassFilter.ChildOf(parent),
		  new OntologyClassFilter.Custom(candidate -> candidate == loner));
	assertThat(resolver.getAllClasses(mixedOr)).containsExactlyInAnyOrder(child, loner);
	assertThat(classUniverseScans()).isEqualTo(1);
	assertThat(hierarchyCounter.count("getChildren")).isZero(); // ChildOf answered by isParent, natively
}

/** The class-side fallback must not drag the element side in with it, and vice versa -- "the classes,
 *  or the elements, never both". */
@Test
public void testClassFallback_DoesNotTouchTheElementUniverse() {
	newClass();
	newElement(ROOT);
	startCounting();

	resolver.getAllClasses(new OntologyClassFilter.NameContaining("nothing-matches-this"));
	assertThat(classUniverseScans()).isEqualTo(1);
	assertThat(elementUniverseScans()).isZero();
}

@Test
public void testElementFallback_DoesNotTouchTheClassUniverse() {
	newClass();
	newElement(ROOT);
	startCounting();

	resolver.getAllElements(new OntologyElementFilter.NameContaining("nothing-matches-this"));
	assertThat(elementUniverseScans()).isEqualTo(1);
	assertThat(classUniverseScans()).isZero();
}

// --- 3. Repeated subtrees: the case the previous attempt missed ---

/**
 * {@code Or(And(broad, x), And(broad, y))}: both {@code And}s prefer enumeration and both drive off
 * {@code broad}, so the broad leaf is enumerated twice by the tree's shape. It must still walk the
 * hierarchy once.
 */
@Test
public void testBroadLeafInTwoBranches_SharedInstance_WalksOnce() {
	int top = newClass();
	int first = newClass();
	int second = newClass();
	int excluded = newClass();
	realHierarchy.addParent(first, top);
	realHierarchy.addParent(second, top);
	realHierarchy.addParent(excluded, top);
	startCounting();

	OntologyClassFilter broad = new OntologyClassFilter.DomainOf(top);
	OntologyClassFilter tree = new OntologyClassFilter.Or(
		  new OntologyClassFilter.And(broad, new OntologyClassFilter.Custom(candidate -> candidate == first)),
		  new OntologyClassFilter.And(broad, new OntologyClassFilter.Custom(candidate -> candidate == second)));

	assertThat(resolver.getAllClasses(tree)).containsExactlyInAnyOrder(first, second);
	assertThat(hierarchyCounter.count("getHeritage(" + top + ")")).isEqualTo(1);
	assertThat(classUniverseScans()).isZero();
}

/** The same tree built from two separately-constructed but equal nodes -- what a GUI tree builder
 *  actually produces. The AST is records, so structural equality collapses them too. */
@Test
public void testBroadLeafInTwoBranches_EqualButDistinctInstances_WalksOnce() {
	int top = newClass();
	int first = newClass();
	int second = newClass();
	realHierarchy.addParent(first, top);
	realHierarchy.addParent(second, top);
	startCounting();

	OntologyClassFilter tree = new OntologyClassFilter.Or(
		  new OntologyClassFilter.And(new OntologyClassFilter.DomainOf(top),
				new OntologyClassFilter.Custom(candidate -> candidate == first)),
		  new OntologyClassFilter.And(new OntologyClassFilter.DomainOf(top),
				new OntologyClassFilter.Custom(candidate -> candidate == second)));

	assertThat(resolver.getAllClasses(tree)).containsExactlyInAnyOrder(first, second);
	assertThat(hierarchyCounter.count("getHeritage(" + top + ")")).isEqualTo(1);
}

/** Element-side equivalent: a repeated {@code UnderDomain} resolves its domain once. */
@Test
public void testBroadElementLeafInTwoBranches_WalksOnce() {
	int top = newClass();
	int under = newClass();
	realHierarchy.addParent(under, top);
	int first = newElement(top);
	int second = newElement(under);
	startCounting();

	OntologyElementFilter tree = new OntologyElementFilter.Or(
		  new OntologyElementFilter.And(new OntologyElementFilter.UnderDomain(top),
				new OntologyElementFilter.Custom(candidate -> candidate == first)),
		  new OntologyElementFilter.And(new OntologyElementFilter.UnderDomain(top),
				new OntologyElementFilter.Custom(candidate -> candidate == second)));

	assertThat(resolver.getAllElements(tree)).containsExactlyInAnyOrder(first, second);
	assertThat(analyticsCounter.count("getDomainElements(" + top + ")")).isEqualTo(1);
	assertThat(elementUniverseScans()).isZero();
}

/**
 * A broad leaf used in the <em>test</em> direction across a whole universe scan resolves its domain
 * once, not once per candidate. This is the trap that makes {@code UnderDomain}'s
 * enumerate-then-lookup viable at all.
 */
@Test
public void testBroadLeafTestedAcrossTheUniverse_ResolvesItsDomainOnce() {
	int top = newClass();
	int under = newClass();
	realHierarchy.addParent(under, top);
	int inDomain = newElement(under);
	for (int i = 0; i < 5; i++) newElement(ROOT);
	startCounting();

	// Or cannot anchor (NameContaining never can), so every element is tested against UnderDomain.
	OntologyElementFilter tree = new OntologyElementFilter.Or(
		  new OntologyElementFilter.UnderDomain(top),
		  new OntologyElementFilter.NameContaining("no-such-name"));
	assertThat(resolver.getAllElements(tree)).containsExactly(inDomain);
	assertThat(elementUniverseScans()).isEqualTo(1);
	assertThat(analyticsCounter.count("getDomainElements(" + top + ")")).isEqualTo(1);
}

/** Three occurrences across a deeper tree, mixing shared and distinct instances. */
@Test
public void testBroadLeafInThreeBranches_WalksOnce() {
	int top = newClass();
	int a = newClass();
	int b = newClass();
	int c = newClass();
	realHierarchy.addParent(a, top);
	realHierarchy.addParent(b, top);
	realHierarchy.addParent(c, top);
	startCounting();

	OntologyClassFilter shared = new OntologyClassFilter.DomainOf(top);
	OntologyClassFilter tree = new OntologyClassFilter.Or(
		  new OntologyClassFilter.And(shared, new OntologyClassFilter.Custom(candidate -> candidate == a)),
		  new OntologyClassFilter.Or(
				new OntologyClassFilter.And(new OntologyClassFilter.DomainOf(top),
					  new OntologyClassFilter.Custom(candidate -> candidate == b)),
				new OntologyClassFilter.Subtract(shared,
					  new OntologyClassFilter.Custom(candidate -> candidate != c))));

	assertThat(resolver.getAllClasses(tree)).containsExactlyInAnyOrder(a, b, c);
	assertThat(hierarchyCounter.count("getHeritage(" + top + ")")).isEqualTo(1);
}

/** A memo lives no longer than one resolution: two separate calls each pay their own walk, and the
 *  second sees post-mutation state. */
@Test
public void testMemoDoesNotOutliveOneResolution() {
	int top = newClass();
	int first = newClass();
	realHierarchy.addParent(first, top);
	startCounting();

	OntologyClassFilter broad = new OntologyClassFilter.DomainOf(top);
	assertThat(resolver.getAllClasses(broad)).containsExactlyInAnyOrder(top, first);
	assertThat(hierarchyCounter.count("getHeritage(" + top + ")")).isEqualTo(1);

	int second = newClass();
	realHierarchy.addParent(second, top);

	assertThat(resolver.getAllClasses(broad)).containsExactlyInAnyOrder(top, first, second);
	assertThat(hierarchyCounter.count("getHeritage(" + top + ")")).isEqualTo(2);
}

/** Guards the counting fixture itself: without the memo these numbers would be the interesting ones,
 *  so an assertion of 1 has to be able to fail. A leaf genuinely used twice across two resolutions
 *  is counted twice (above), and a fresh IntArrayList is returned each time, so the counter is not
 *  measuring something trivially constant. */
@Test
public void testCounterActuallyObservesRepeatedCalls() {
	int top = newClass();
	startCounting();
	int[] first = resolver.getAllClasses(new OntologyClassFilter.DescendantOf(top));
	int[] second = resolver.getAllClasses(new OntologyClassFilter.DescendantOf(top));
	assertThat(first).isEqualTo(second);
	assertThat(hierarchyCounter.count("getHeritage(" + top + ")")).isEqualTo(2);
}

}
