package org.halim.test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.halim.ontology.legacy.core.segmentation.command.AnyCatalogMutableBase;
import org.halim.ontology.legacy.core.segmentation.query.AnyCatalogBase;
import org.halim.ontology.legacy.core.segmentation.query.CustomOntologyFilterBase;
import org.halim.ontology.legacy.core.segmentation.query.OntologyClassFilter;
import org.halim.ontology.legacy.core.segmentation.query.OntologyElementCatalogSegmentation;
import org.halim.ontology.legacy.core.segmentation.query.OntologyElementFilter;
import org.halim.ontology.legacy.core.segments.data.OntologyAnalyticsCalculator;
import org.halim.ontology.legacy.core.segments.data.OntologyClassCatalog;
import org.halim.ontology.legacy.core.segments.data.OntologyElementCatalog;
import org.halim.ontology.legacy.core.segments.data.OntologyFilterResolverSegmentation;
import org.halim.ontology.legacy.core.segments.manager.DoubleArrayedIndexer;
import org.halim.ontology.legacy.core.segments.manager.OntologyHierarchyLister;
import org.halim.ontology.legacy.view.filter.AbstractEnumeratingFilterBase;
import org.halim.ontology.legacy.view.filter.AbstractTestingFilterBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the feature this whole pass exists for: a client supplying its own filtering logic, in either
 * direction, standalone and composed with built-in leaves -- including filtering on data the engine
 * knows nothing about, which is the point (see {@code CUSTOM-FILTERING-SPEC.md} §1-2).
 * <p>
 * Also pins the staleness promise: engine-side memoization lives exactly one resolution, so a built-in
 * node can never answer from a pre-mutation set, while a client filter's own cache is documented as
 * fresh-per-query and has {@code invalidateCache()} as the supported way to reuse an instance.
 */
public class OntologyCustomFilterTest {

/** A detail id the engine has no knowledge of whatsoever -- exactly the situation custom filtering
 *  exists to serve. {@code NAME_DETAIL} is 0, so anything else is free for a client. */
private static final int SIZE_IN_BYTES_DETAIL = 7;

/**
 * A client-supplied catalog store, of the kind {@code AnyCatalogBase}'s javadoc describes: names in
 * one row, a typed {@code long} size in another. The engine's built-in
 * {@code OntologyCatalogDetailStore} deliberately stores only the name detail and tells callers to
 * supply exactly this for anything typed -- so using one here is not test scaffolding around a
 * limitation, it is the motivating scenario for custom filtering, end to end: a client stores data the
 * engine knows nothing about, then filters on it.
 */
private static final class FileMetadataStore implements AnyCatalogBase, AnyCatalogMutableBase {
	private final Map<Integer, String> names = new HashMap<>();
	private final Map<Integer, Long> sizes = new HashMap<>();

	@Override public Object getDetailObject(int entryIdentity, int detailTypeIdentity) {
		return detailTypeIdentity == OntologyElementCatalogSegmentation.NAME_DETAIL ? names.get(entryIdentity) : null;
	}
	@Override public long getDetailLong(int entryIdentity, int detailTypeIdentity) {
		return detailTypeIdentity == SIZE_IN_BYTES_DETAIL ? sizes.getOrDefault(entryIdentity, 0L) : 0L;
	}
	@Override public double getDetailDouble(int entryIdentity, int detailTypeIdentity) { return getDetailLong(entryIdentity, detailTypeIdentity); }
	@Override public boolean getDetailBoolean(int entryIdentity, int detailTypeIdentity) { return getDetailLong(entryIdentity, detailTypeIdentity) != 0L; }

	@Override public void setDetailObject(int entryIdentity, int detailTypeIdentity, Object value) {
		if (detailTypeIdentity == OntologyElementCatalogSegmentation.NAME_DETAIL) names.put(entryIdentity, (String) value);
	}
	@Override public void setDetailLong(int entryIdentity, int detailTypeIdentity, long value) {
		if (detailTypeIdentity == SIZE_IN_BYTES_DETAIL) sizes.put(entryIdentity, value);
	}
	@Override public void setDetailDouble(int entryIdentity, int detailTypeIdentity, double value) { setDetailLong(entryIdentity, detailTypeIdentity, (long) value); }
	@Override public void setDetailBoolean(int entryIdentity, int detailTypeIdentity, boolean value) { setDetailLong(entryIdentity, detailTypeIdentity, value ? 1L : 0L); }
}

private OntologyHierarchyLister hierarchy;
private DoubleArrayedIndexer indexer;
private FileMetadataStore elementDetails;
private OntologyElementCatalog elementCatalog;
private OntologyClassCatalog classCatalog;
private OntologyFilterResolverSegmentation resolver;

@BeforeEach
public void setup() {
	hierarchy = new OntologyHierarchyLister();
	indexer = new DoubleArrayedIndexer();
	indexer.newClass(); // burn the indexer's id 0; the hierarchy reserves it for root
	elementDetails = new FileMetadataStore();
	classCatalog = new OntologyClassCatalog(hierarchy, indexer);
	elementCatalog = new OntologyElementCatalog(hierarchy, indexer, elementDetails);
	OntologyAnalyticsCalculator analytics = new OntologyAnalyticsCalculator(hierarchy, indexer);
	resolver = new OntologyFilterResolverSegmentation(hierarchy, indexer, analytics, classCatalog, elementCatalog);
}

private int newClass(String name) {
	int id = indexer.newClass();
	hierarchy.registerClass(id);
	classCatalog.setName(id, name);
	return id;
}

private int newElement(String name, long sizeInBytes, int... classIdentities) {
	int id = indexer.newElement();
	for (int classIdentity : classIdentities) indexer.ensureConnection(classIdentity, id);
	elementCatalog.setName(id, name);
	elementDetails.setDetailLong(id, SIZE_IN_BYTES_DETAIL, sizeInBytes);
	return id;
}

// --- Standalone, both directions, both kinds ---

@Test
public void testTestPreferringClassFilter_Standalone() {
	int alpha = newClass("alpha");
	int beta = newClass("beta");
	OntologyClassFilter filter = new OntologyClassFilter.Custom(new AbstractTestingFilterBase() {
		@Override public boolean test(int candidateIdentity) { return candidateIdentity == beta; }
	});
	assertThat(resolver.getAllClasses(filter)).containsExactly(beta);
	assertThat(alpha).isNotNegative();
}

@Test
public void testEnumeratePreferringClassFilter_Standalone() {
	int alpha = newClass("alpha");
	newClass("beta");
	OntologyClassFilter filter = new OntologyClassFilter.Custom(new AbstractEnumeratingFilterBase() {
		@Override public IntArrayList enumerate() { return IntArrayList.of(alpha); }
	});
	assertThat(resolver.getAllClasses(filter)).containsExactly(alpha);
}

@Test
public void testTestPreferringElementFilter_Standalone() {
	int tag = newClass("tag");
	int small = newElement("small.txt", 100, tag);
	newElement("large.bin", 9_000_000, tag);

	OntologyElementFilter filter = new OntologyElementFilter.Custom(new AbstractTestingFilterBase() {
		@Override public boolean test(int candidateIdentity) {
			return elementDetails.getDetailLong(candidateIdentity, SIZE_IN_BYTES_DETAIL) < 1_000;
		}
	});
	assertThat(resolver.getAllElements(filter)).containsExactly(small);
}

@Test
public void testEnumeratePreferringElementFilter_Standalone() {
	int tag = newClass("tag");
	int first = newElement("a.txt", 1, tag);
	newElement("b.txt", 2, tag);
	OntologyElementFilter filter = new OntologyElementFilter.Custom(new AbstractEnumeratingFilterBase() {
		@Override public IntArrayList enumerate() { return IntArrayList.of(first); }
	});
	assertThat(resolver.getAllElements(filter)).containsExactly(first);
}

/** {@code test} is the single abstract method on both contracts, so the simplest custom filter is a
 *  lambda -- no class, no base class. */
@Test
public void testLambdaFilter_Works() {
	int tag = newClass("tag");
	int keep = newElement("keep.txt", 10, tag);
	newElement("drop.txt", 10, tag);
	assertThat(resolver.getAllElements(new OntologyElementFilter.Custom(id -> id == keep))).containsExactly(keep);
}

// --- Composed with built-in leaves ---

@Test
public void testCustomFilter_UnderAnd_WithBuiltInLeafDriving() {
	int photos = newClass("photos");
	int other = newClass("other");
	int big = newElement("big.raw", 5_000_000, photos);
	newElement("small.jpg", 500, photos);
	newElement("big-elsewhere.raw", 5_000_000, other);

	OntologyElementFilter filter = new OntologyElementFilter.And(
		  new OntologyElementFilter.UnderDomain(photos),
		  new OntologyElementFilter.Custom(id -> elementDetails.getDetailLong(id, SIZE_IN_BYTES_DETAIL) > 1_000_000));
	assertThat(resolver.getAllElements(filter)).containsExactly(big);
}

/** Operand order must not change the answer, even though it changes which side drives. */
@Test
public void testCustomFilter_UnderAnd_EitherOperandOrder_SameResult() {
	int photos = newClass("photos");
	int big = newElement("big.raw", 5_000_000, photos);
	newElement("small.jpg", 500, photos);

	OntologyElementFilter custom =
		  new OntologyElementFilter.Custom(id -> elementDetails.getDetailLong(id, SIZE_IN_BYTES_DETAIL) > 1_000_000);
	OntologyElementFilter builtIn = new OntologyElementFilter.UnderDomain(photos);

	assertThat(resolver.getAllElements(new OntologyElementFilter.And(builtIn, custom))).containsExactly(big);
	assertThat(resolver.getAllElements(new OntologyElementFilter.And(custom, builtIn))).containsExactly(big);
}

@Test
public void testCustomFilter_UnderOr_WithBuiltInLeaf() {
	int photos = newClass("photos");
	int docs = newClass("docs");
	int photo = newElement("a.jpg", 10, photos);
	int doc = newElement("b.txt", 10, docs);
	newElement("c.bin", 10, docs);

	OntologyElementFilter filter = new OntologyElementFilter.Or(
		  new OntologyElementFilter.UnderDomain(photos),
		  new OntologyElementFilter.Custom(id -> id == doc));
	assertThat(resolver.getAllElements(filter)).containsExactlyInAnyOrder(photo, doc);
}

@Test
public void testCustomFilter_UnderSubtract_AsBothKeptAndExcluded() {
	int tag = newClass("tag");
	int keep = newElement("keep.txt", 10, tag);
	int drop = newElement("drop.txt", 10, tag);

	assertThat(resolver.getAllElements(new OntologyElementFilter.Subtract(
		  new OntologyElementFilter.UnderDomain(tag), new OntologyElementFilter.Custom(id -> id == drop))))
		  .containsExactly(keep);

	assertThat(resolver.getAllElements(new OntologyElementFilter.Subtract(
		  new OntologyElementFilter.Custom(new AbstractEnumeratingFilterBase() {
			  @Override public IntArrayList enumerate() { return IntArrayList.of(keep, drop); }
		  }),
		  new OntologyElementFilter.DirectElementOf(tag))))
		  .isEmpty();
}

/** An enumerate-preferring custom filter placed where the tree cannot anchor gets used through its
 *  derived test instead -- the base class supplies that direction from a single enumerate() call. */
@Test
public void testEnumeratePreferringFilter_IsUsedThroughItsDerivedTest_WhenTheTreeCannotAnchor() {
	int tag = newClass("tag");
	int wanted = newElement("wanted.txt", 10, tag);
	newElement("other.txt", 10, tag);

	int[] enumerateCalls = {0};
	OntologyElementFilter enumerating = new OntologyElementFilter.Custom(new AbstractEnumeratingFilterBase() {
		@Override public IntArrayList enumerate() { enumerateCalls[0]++; return IntArrayList.of(wanted); }
	});
	// NameContaining never prefers enumeration, so the Or cannot anchor and every element is tested.
	OntologyElementFilter filter = new OntologyElementFilter.Or(
		  enumerating, new OntologyElementFilter.NameContaining("no-such-name"));

	assertThat(resolver.getAllElements(filter)).containsExactly(wanted);
	assertThat(enumerateCalls[0]).isEqualTo(1);
}

@Test
public void testCustomClassFilter_ComposedWithHierarchyLeaf() {
	int parent = newClass("parent");
	int wanted = newClass("wanted");
	int unwanted = newClass("unwanted");
	hierarchy.addParent(wanted, parent);
	hierarchy.addParent(unwanted, parent);

	OntologyClassFilter filter = new OntologyClassFilter.And(
		  new OntologyClassFilter.ChildOf(parent),
		  new OntologyClassFilter.Custom(id -> id == wanted));
	assertThat(resolver.getAllClasses(filter)).containsExactly(wanted);
}

// --- Malformed implementations ---

/** Declaring neither direction is silently test-preferring: correct, just possibly slower. */
@Test
public void testImplementationDeclaringNeitherDirection_ResolvesThroughTheUniverseScan() {
	int alpha = newClass("alpha");
	newClass("beta");
	CustomOntologyFilterBase declaresNothing = candidateIdentity -> candidateIdentity == alpha;
	assertThat(resolver.getAllClasses(new OntologyClassFilter.Custom(declaresNothing))).containsExactly(alpha);
}

/** Claiming enumerate-preference without implementing enumerate() fails loudly rather than quietly
 *  returning wrong results -- the one malformed case that cannot be silently absorbed. */
@Test
public void testImplementationClaimingEnumerateWithoutProvidingIt_FailsLoudly() {
	newClass("alpha");
	CustomOntologyFilterBase liar = new CustomOntologyFilterBase() {
		@Override public boolean test(int candidateIdentity) { return true; }
		@Override public boolean isEnumeratePreferred() { return true; }
	};
	assertThatThrownBy(() -> resolver.getAllClasses(new OntologyClassFilter.Custom(liar)))
		  .isInstanceOf(UnsupportedOperationException.class)
		  .hasMessageContaining("prefers testing but was asked to enumerate");
}

// --- Staleness (CUSTOM-FILTERING-SPEC.md §6.4 / §8.4) ---

/** Engine-side memoization lives exactly one resolution, so a built-in node re-resolved after a
 *  mutation always reflects the new state -- no generation counter needed. */
@Test
public void testBuiltInNodes_AreNeverStaleAcrossResolutions() {
	int tag = newClass("tag");
	int first = newElement("first.txt", 10, tag);
	OntologyElementFilter reusedTree = new OntologyElementFilter.UnderDomain(tag);

	assertThat(resolver.getAllElements(reusedTree)).containsExactly(first);

	int second = newElement("second.txt", 10, tag);
	assertThat(resolver.getAllElements(reusedTree)).containsExactlyInAnyOrder(first, second);
}

/**
 * A client filter's own cache is the documented exception: build filters fresh per query. A reused
 * enumerating instance keeps answering from the domain it computed the first time, and
 * {@code invalidateCache()} is the supported way out. Asserting the stale answer is deliberate -- the
 * contract is a real behavioural promise, so it is pinned rather than assumed.
 */
@Test
public void testReusedEnumeratingCustomFilter_IsStaleUntilInvalidated() {
	int tag = newClass("tag");
	int first = newElement("first.txt", 10, tag);

	class TagSnapshotFilter extends AbstractEnumeratingFilterBase {
		// A copy, not the live list: getElementsOf hands back the indexer's own IntArrayList, so
		// returning it directly would make the "cache" a view that silently tracks later mutations.
		@Override public IntArrayList enumerate() { return new IntArrayList(indexer.getElementsOf(tag)); }
		void refresh() { invalidateCache(); }
	}
	TagSnapshotFilter reusedInstance = new TagSnapshotFilter();

	// Force the derived-test direction, which is the direction that caches.
	OntologyElementFilter cannotAnchor = new OntologyElementFilter.Or(
		  new OntologyElementFilter.Custom(reusedInstance), new OntologyElementFilter.NameContaining("no-such-name"));

	assertThat(resolver.getAllElements(cannotAnchor)).containsExactly(first);

	int second = newElement("second.txt", 10, tag);
	assertThat(resolver.getAllElements(cannotAnchor))
		  .as("a reused instance answers from its first domain -- the fresh-per-query contract")
		  .containsExactly(first);

	reusedInstance.refresh();
	assertThat(resolver.getAllElements(cannotAnchor)).containsExactlyInAnyOrder(first, second);

	// A filter built fresh per query, as the contract asks, never sees the problem at all.
	assertThat(resolver.getAllElements(new OntologyElementFilter.Or(
		  new OntologyElementFilter.Custom(new TagSnapshotFilter()),
		  new OntologyElementFilter.NameContaining("no-such-name"))))
		  .containsExactlyInAnyOrder(first, second);
}

}
