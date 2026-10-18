package org.halim.ontology.legacy.core.segments.data;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.halim.ontology.legacy.core.segmentation.command.OntologyClassificationSegmentation;
import org.halim.ontology.legacy.core.segmentation.command.OntologyHierarchySegmentation;
import org.halim.ontology.legacy.core.segmentation.query.OntologyFilteringServiceSegmentation;
import org.halim.ontology.legacy.core.segmentation.query.OntologyAnalyticsSegmentation;
import org.halim.ontology.legacy.core.segmentation.query.OntologyClassCatalogSegmentation;
import org.halim.ontology.legacy.core.segmentation.query.OntologyElementCatalogSegmentation;
import org.halim.ontology.legacy.core.segmentation.query.OntologyInplaceQuerySegmentation;
import org.halim.ontology.legacy.core.segmentation.query.OntologyClassFilter;
import org.halim.ontology.legacy.core.segmentation.query.OntologyElementFilter;
import org.halim.ontology.legacy.core.segmentation.query.AnyCatalogBase;
import org.halim.ontology.legacy.core.segmentation.helper.OntologyQueryable;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * Resolves {@link OntologyClassFilter}/{@link OntologyElementFilter} trees into actual matches.
 *
 * <h2>Enumerate vs. test</h2>
 * Every node supports two operations: <b>enumerate</b> (produce the matching set) and <b>test</b>
 * (judge one candidate). Each node reports its cheap direction through
 * {@code isEnumeratePreferred()} — the filter's own responsibility, not this class's to infer.
 * <p>
 * Enumeration comes in two flavours, kept deliberately distinct. A node's own native {@code enumerate}
 * produces its complete domain, out of everything of its kind, and is the only one a client ever
 * implements. {@link #enumerateFromTest} produces a set from a candidate set the engine already holds,
 * by keeping what passes {@code test}; it is engine bookkeeping and never appears in a client contract,
 * precisely so no client can be handed a base it might silently ignore and over-match against.
 * Resolution is then two mutually recursive walks:
 * <ul>
 *   <li>{@code enumerate} maps a node onto an already-efficient engine call — {@code DescendantOf}
 *       onto {@link OntologyHierarchySegmentation#getHeritage}, {@code UnderDomain} onto
 *       {@link OntologyAnalyticsSegmentation#getDomainElements}, and so on — and combines children
 *       through {@link BitSet}-backed set ops, the dedup convention the rest of {@code core.segments}
 *       already uses.</li>
 *   <li>{@code test} composes upward for free through every combinator ({@code And} needs both,
 *       {@code Or} either, {@code Subtract} kept-and-not-excluded) and never requires a scan.</li>
 * </ul>
 * {@code enumerate} is only ever entered on an enumerate-preferring node: {@code Or} is reached only
 * when both children prefer it, {@code And} routes to whichever side does (left when both do — there
 * is deliberately no cost estimation, so {@code And(broad, narrow)} drives off the broad side purely
 * because it is leftmost), and {@code Subtract} drives off {@code kept}. Any other arrival is a
 * routing-invariant violation and throws rather than silently falling back.
 *
 * <h2>Scanning everything at most once</h2>
 * The entirety of the classes — or of the elements, never both — is walked at most once per
 * resolution. If any part of a tree can anchor an enumeration, no full scan happens at all; only a
 * tree that prefers testing all the way to its root falls back, and then the universe is materialized
 * in exactly one place, {@link #resolveClasses}/{@link #resolveElements}, and handed to
 * {@link #enumerateFromTest} as its base. Never once per subtree, per leaf, or per occurrence.
 * <p>
 * The universe is an index scan of the identity space ({@code getLastClassIndex}/
 * {@code getLastElementIndex} minus tombstones) rather than a walk down from root, so the class-side
 * fallback touches only classes and the element-side fallback touches only elements. Deriving the
 * element universe from root would have meant walking every class's heritage as well.
 * <p>
 * Repeated occurrences of one node collapse through the per-resolution {@code memo}, created
 * fresh per top-level call and discarded with it. This is what stops a broad leaf sitting in several
 * branches of one tree — {@code Or(And(DomainOf(x), a), And(DomainOf(x), b))} — from walking the
 * hierarchy once per occurrence. The AST is records, so structurally equal nodes share even when they
 * were constructed separately; {@code Custom} degrades to identity equality, which is correct and
 * harmless. Because a memo lives no longer than one resolution, no built-in node can ever answer from
 * a set computed before a mutation.
 *
 * <h2>Which leaves get a native test</h2>
 * Only the ones that are genuinely cheap per candidate: {@code ChildOf}/{@code ParentOf} via
 * {@code isParent} (one parent-list scan), {@code DirectElementOf} via {@code isConnected} (one
 * bit read), and {@code NameContaining} via a single catalog read. The ancestry-shaped leaves —
 * {@code DescendantOf}, {@code AncestorOf}, {@code DomainOf}, {@code UnderDomain} — instead test by
 * membership in their own memoized enumeration: {@code isAncestor}/{@code isHeritage} are
 * undeduplicated depth-first walks, so running one per candidate across a universe scan costs far more
 * than materializing the set once. As a bonus this makes the two directions agree by construction for
 * exactly the leaves where reflexivity is a trap ({@code isAncestor(X, X)} is {@code true} while
 * {@code getAncestors(X)} excludes {@code X}).
 * <p>
 * Restriction push-down is deliberately absent: no candidate set is threaded down the recursion.
 * Narrowing happens by filtering a driver's result, never by pushing a restriction downward.
 * <p>
 * One cost worth knowing: the services injected here are the engine's dynamic proxies, so every
 * per-candidate native test pays a reflective dispatch and a reentrant read-lock acquisition. That is
 * inherent to the fallback path, which is exactly the path the design works to avoid entering.
 */
public class OntologyFilterResolverSegmentation implements OntologyFilteringServiceSegmentation {

private final OntologyHierarchySegmentation hierarchy;
private final OntologyClassificationSegmentation classification;
private final OntologyAnalyticsSegmentation analytics;
private final OntologyClassCatalogSegmentation classData;
private final OntologyElementCatalogSegmentation elementData;

public OntologyFilterResolverSegmentation(OntologyHierarchySegmentation hierarchy, OntologyClassificationSegmentation classification,
                                          OntologyAnalyticsSegmentation analytics, OntologyClassCatalogSegmentation classData, OntologyElementCatalogSegmentation elementData) {
    this.hierarchy = hierarchy;
    this.classification = classification;
    this.analytics = analytics;
    this.classData = classData;
    this.elementData = elementData;
}

// --- OntologyFilteringServiceSegmentation ---

@Override
public int[] getAllClasses(OntologyClassFilter filter) { return resolveClasses(filter).toIntArray(); }

@Override
public Set<Integer> getClassesAsSet(OntologyClassFilter filter) { return toSet(resolveClasses(filter)); }

@Override
public void forEachClassSatisfying(OntologyClassFilter filter, OntologyQueryable.BaseInplaceClass.OntologyClassQuery query) {
    for (int c : resolveClasses(filter)) query.query(c);
}

@Override
public int[] getAllElements(OntologyElementFilter filter) { return resolveElements(filter).toIntArray(); }

@Override
public Set<Integer> getElementsAsSet(OntologyElementFilter filter) { return toSet(resolveElements(filter)); }

@Override
public void forEachElementSatisfying(OntologyElementFilter filter, OntologyInplaceQuerySegmentation.OntologyElementQuery query) {
    for (int e : resolveElements(filter)) query.query(e);
}

/**
 * One node's materialized domain, plus the membership index derived from it on demand. The engine
 * probes a memoized set once per candidate on the fallback path, so membership has to be constant
 * time; the {@code BitSet} is built only if something actually asks. (The deliberately linear scan
 * documented on {@code view.filter}'s enumerating base class is a different cache with a different
 * budget: that one holds a client filter's own matches, which rarely exceed a few dozen entries.)
 */
private static final class Materialized {
    private final IntArrayList list;
    private BitSet bits;
    private Materialized(IntArrayList list) { this.list = list; }
    private boolean contains(int identity) {
        if (bits == null) bits = toBitSet(list);
        return identity >= 0 && bits.get(identity);
    }
}

// --- Top-level dispatch. The one place a universe is ever materialized. ---

private IntArrayList resolveClasses(OntologyClassFilter filter) {
    Map<OntologyClassFilter, Materialized> memo = new HashMap<>();
    if (filter.isEnumeratePreferred()) return enumerateClasses(filter, memo).list;
    return enumerateFromTest(filter, universeClasses(), memo);
}

private IntArrayList resolveElements(OntologyElementFilter filter) {
    Map<OntologyElementFilter, Materialized> memo = new HashMap<>();
    if (filter.isEnumeratePreferred()) return enumerateElements(filter, memo).list;
    return enumerateFromTest(filter, universeElements(), memo);
}

/** Every live class id. Walks the classification service's identity space rather than descending from
 *  root, so the class-side fallback never touches the element side. */
private IntArrayList universeClasses() {
    IntArrayList all = new IntArrayList();
    int last = classification.getLastClassIndex();
    for (int identity = 0; identity <= last; identity++) {
        if (!classification.isClassTombstoned(identity)) all.add(identity);
    }
    return all;
}

/** Every live element id — the element-side counterpart to {@link #universeClasses()}. Includes
 *  elements carrying no classification at all, which a walk down from root could not reach. */
private IntArrayList universeElements() {
    IntArrayList all = new IntArrayList();
    int last = classification.getLastElementIndex();
    for (int identity = 0; identity <= last; identity++) {
        if (!classification.isElementTombstoned(identity)) all.add(identity);
    }
    return all;
}

// --- Class filter resolution ---

private Materialized enumerateClasses(OntologyClassFilter filter, Map<OntologyClassFilter, Materialized> memo) {
    Materialized cached = memo.get(filter);
    if (cached != null) return cached;
    IntArrayList computed = switch (filter) {
        case OntologyClassFilter.ChildOf(int parentIdentity) -> hierarchy.getChildren(parentIdentity);
        case OntologyClassFilter.DescendantOf(int ancestorIdentity) -> hierarchy.getHeritage(ancestorIdentity);
        case OntologyClassFilter.DomainOf(int classIdentity) -> inclusiveHeritage(classIdentity);
        case OntologyClassFilter.ParentOf(int childIdentity) -> hierarchy.getParents(childIdentity);
        case OntologyClassFilter.AncestorOf(int descendantIdentity) -> hierarchy.getAncestors(descendantIdentity);
        // Reaching And guarantees at least one side prefers enumeration; that side drives, the other
        // is used purely as a test against the already-narrowed result.
        case OntologyClassFilter.And(var left, var right) -> left.isEnumeratePreferred()
              ? enumerateFromTest(right, enumerateClasses(left, memo).list, memo)
              : enumerateFromTest(left, enumerateClasses(right, memo).list, memo);
        case OntologyClassFilter.Or(var left, var right) ->
              union(enumerateClasses(left, memo).list, enumerateClasses(right, memo).list);
        // Not enumerateFromTest: Subtract narrows by a *failing* test, keeping what `excluded` rejects.
        case OntologyClassFilter.Subtract(var kept, var excluded) ->
              filterBy(enumerateClasses(kept, memo).list, candidate -> !testClass(excluded, candidate, memo));
        // The implementation hands back its complete domain and the engine narrows it; a client asked
        // to honour a candidate set itself could silently ignore it and over-match.
        case OntologyClassFilter.Custom(var implementation) -> implementation.enumerate();
        case OntologyClassFilter.NameContaining ignored -> throw new IllegalStateException(
              "NameContaining never prefers enumeration; reaching it here means a combinator routed an "
                    + "enumerate into a test-preferring child -- routing invariant violated.");
    };
    Materialized materialized = new Materialized(computed);
    memo.put(filter, materialized);
    return materialized;
}

private boolean testClass(OntologyClassFilter filter, int candidateIdentity, Map<OntologyClassFilter, Materialized> memo) {
    return switch (filter) {
        // The identity guard is not redundant: root is its own sentinel parent, so isParent(0, 0) is
        // true while getChildren(0) excludes root -- without it the two directions would disagree on
        // exactly one candidate, and only when the anchor is root.
        case OntologyClassFilter.ChildOf(int parentIdentity) ->
              candidateIdentity != parentIdentity && hierarchy.isParent(candidateIdentity, parentIdentity);
        // No guard here, deliberately: getParents(0) reports root's self-parent too, so both directions
        // already agree that root is its own parent.
        case OntologyClassFilter.ParentOf(int childIdentity) -> hierarchy.isParent(childIdentity, candidateIdentity);
        case OntologyClassFilter.NameContaining(var query) ->
              matchesDetail(classData, OntologyClassCatalogSegmentation.NAME_DETAIL, candidateIdentity, query.toLowerCase());
        // Ancestry-shaped leaves: membership in their own memoized enumeration beats an undeduplicated
        // isAncestor/isHeritage walk per candidate, and makes both directions agree on the
        // self-identity candidate for free.
        case OntologyClassFilter.DescendantOf ignored -> enumerateClasses(filter, memo).contains(candidateIdentity);
        case OntologyClassFilter.AncestorOf ignored -> enumerateClasses(filter, memo).contains(candidateIdentity);
        case OntologyClassFilter.DomainOf ignored -> enumerateClasses(filter, memo).contains(candidateIdentity);
        case OntologyClassFilter.And(var left, var right) ->
              testClass(left, candidateIdentity, memo) && testClass(right, candidateIdentity, memo);
        case OntologyClassFilter.Or(var left, var right) ->
              testClass(left, candidateIdentity, memo) || testClass(right, candidateIdentity, memo);
        case OntologyClassFilter.Subtract(var kept, var excluded) ->
              testClass(kept, candidateIdentity, memo) && !testClass(excluded, candidateIdentity, memo);
        case OntologyClassFilter.Custom(var implementation) -> implementation.test(candidateIdentity);
    };
}

/** {@code getHeritage(classIdentity)} plus {@code classIdentity} itself — {@code DomainOf}'s native
 *  enumerate, and the self-inclusive counterpart to {@code DescendantOf}. */
private IntArrayList inclusiveHeritage(int classIdentity) {
    IntArrayList descendants = hierarchy.getHeritage(classIdentity);
    IntArrayList result = new IntArrayList(descendants.size() + 1);
    result.add(classIdentity);
    result.addAll(descendants);
    return result;
}

// --- Element filter resolution ---

private Materialized enumerateElements(OntologyElementFilter filter, Map<OntologyElementFilter, Materialized> memo) {
    Materialized cached = memo.get(filter);
    if (cached != null) return cached;
    IntArrayList computed = switch (filter) {
        case OntologyElementFilter.DirectElementOf(int classIdentity) -> classification.getElementsOf(classIdentity);
        case OntologyElementFilter.UnderDomain(int classIdentity) -> analytics.getDomainElements(classIdentity);
        case OntologyElementFilter.And(var left, var right) -> left.isEnumeratePreferred()
              ? enumerateFromTest(right, enumerateElements(left, memo).list, memo)
              : enumerateFromTest(left, enumerateElements(right, memo).list, memo);
        case OntologyElementFilter.Or(var left, var right) ->
              union(enumerateElements(left, memo).list, enumerateElements(right, memo).list);
        // Not enumerateFromTest: Subtract narrows by a *failing* test, keeping what `excluded` rejects.
        case OntologyElementFilter.Subtract(var kept, var excluded) ->
              filterBy(enumerateElements(kept, memo).list, candidate -> !testElement(excluded, candidate, memo));
        // See the class-filter overload: narrowing is the resolver's job, never the client's.
        case OntologyElementFilter.Custom(var implementation) -> implementation.enumerate();
        case OntologyElementFilter.NameContaining ignored -> throw new IllegalStateException(
              "NameContaining never prefers enumeration; reaching it here means a combinator routed an "
                    + "enumerate into a test-preferring child -- routing invariant violated.");
    };
    Materialized materialized = new Materialized(computed);
    memo.put(filter, materialized);
    return materialized;
}

private boolean testElement(OntologyElementFilter filter, int candidateIdentity, Map<OntologyElementFilter, Materialized> memo) {
    return switch (filter) {
        case OntologyElementFilter.DirectElementOf(int classIdentity) -> classification.isConnected(classIdentity, candidateIdentity);
        case OntologyElementFilter.NameContaining(var query) ->
              matchesDetail(elementData, OntologyElementCatalogSegmentation.NAME_DETAIL, candidateIdentity, query.toLowerCase());
        // No cheap per-candidate route: an element is under a class's domain if any class it carries is
        // that class or a descendant, and answering that per candidate means walking ancestry per
        // carried class. Materializing the domain once and probing it is strictly better -- and the
        // memo makes "once" mean once per resolution however many candidates get tested.
        case OntologyElementFilter.UnderDomain ignored -> enumerateElements(filter, memo).contains(candidateIdentity);
        case OntologyElementFilter.And(var left, var right) ->
              testElement(left, candidateIdentity, memo) && testElement(right, candidateIdentity, memo);
        case OntologyElementFilter.Or(var left, var right) ->
              testElement(left, candidateIdentity, memo) || testElement(right, candidateIdentity, memo);
        case OntologyElementFilter.Subtract(var kept, var excluded) ->
              testElement(kept, candidateIdentity, memo) && !testElement(excluded, candidateIdentity, memo);
        case OntologyElementFilter.Custom(var implementation) -> implementation.test(candidateIdentity);
    };
}

/**
 * The narrowing flavour of enumeration, and the counterpart to a node's own native {@code enumerate}:
 * given a candidate set the engine already holds, keep exactly those satisfying {@code filter}. This is
 * the operation that turns a test-preferring node into a set, and it is deliberately engine-internal —
 * a client only ever implements {@code enumerate()} over its complete domain, never against a
 * caller-supplied base it could silently ignore.
 * <p>
 * It composes the whole subtree into one predicate and walks {@code base} <b>once</b>, rather than
 * recursing structurally. That distinction is what keeps the single-scan guarantee: a structural
 * recursion would make {@code Or(a, b)} walk {@code base} once for {@code a} and again for {@code b},
 * which is exactly the "once per subtree, per leaf, or per occurrence" the design forbids.
 * <p>
 * Called with the universe as {@code base} for a tree that prefers testing all the way to its root, and
 * with a driver's already-narrowed result for the non-driving side of an {@code And}.
 */
private IntArrayList enumerateFromTest(OntologyClassFilter filter, IntArrayList base, Map<OntologyClassFilter, Materialized> memo) {
    return filterBy(base, candidate -> testClass(filter, candidate, memo));
}

/** Element counterpart to {@link #enumerateFromTest(OntologyClassFilter, IntArrayList, Map)} — see there. */
private IntArrayList enumerateFromTest(OntologyElementFilter filter, IntArrayList base, Map<OntologyElementFilter, Materialized> memo) {
    return filterBy(base, candidate -> testElement(filter, candidate, memo));
}

// --- Set-op / name-filter helpers, shared by both filter kinds. None of these mutate their inputs,
//     so it's safe for a leaf case above to hand back a live engine list (e.g. hierarchy.getChildren). ---

private static IntArrayList filterBy(IntArrayList source, IntPredicate keep) {
    IntArrayList result = new IntArrayList();
    for (int identity : source) if (keep.test(identity)) result.add(identity);
    return result;
}

private static IntArrayList union(IntArrayList left, IntArrayList right) {
    BitSet seen = new BitSet();
    IntArrayList result = new IntArrayList();
    for (int v : left) if (!seen.get(v)) { seen.set(v); result.add(v); }
    for (int v : right) if (!seen.get(v)) { seen.set(v); result.add(v); }
    return result;
}

private static boolean matchesDetail(AnyCatalogBase catalog, int detailTypeIdentity, int entryIdentity, String lowercaseNeedle) {
    Object detail = catalog.getDetailObject(entryIdentity, detailTypeIdentity);
    return detail != null && detail.toString().toLowerCase().contains(lowercaseNeedle);
}

private static BitSet toBitSet(IntArrayList values) {
    BitSet bits = new BitSet();
    for (int v : values) bits.set(v);
    return bits;
}

private static Set<Integer> toSet(IntArrayList values) {
    Set<Integer> set = new HashSet<>(values.size());
    for (int v : values) set.add(v);
    return set;
}

}
