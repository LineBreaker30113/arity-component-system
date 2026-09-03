package org.halim.ontology.core.segmentation.query;

/**
 * Describes a predicate over ontology classes as a small AST of building blocks. Instances are plain
 * data — resolving a filter into actual matching classes is
 * {@link OntologyFilteringServiceSegmentation}'s job, not this type's, so building a filter never
 * touches the engine.
 * <p>
 * Every node conceptually supports two operations: <b>enumerate</b> (produce the matching set) and
 * <b>test</b> (judge one candidate). {@link #isEnumeratePreferred()} reports which one is a node's
 * cheap direction; the resolver anchors on enumeration wherever a tree allows it, and only walks the
 * whole class universe when nothing in the tree can anchor one. Testing is the default, and a node has
 * to prove the opposite — which is why {@code Or} demands it of both children while {@code And} and
 * {@code Subtract} need it of only one: {@code And} and {@code Subtract} remove instances from an
 * already-valid set, so one branch can drive the enumeration while the other is used purely as a test,
 * whereas {@code Or} can grow the set and so cannot be restricted to either side's enumeration.
 * <p>
 * {@link Custom} is the extension point: it carries a client-supplied
 * {@link CustomOntologyFilterBase}, which is how filtering reaches data the engine knows nothing
 * about.
 */
public sealed interface OntologyClassFilter {

	/**
	 * Whether producing this node's matching set is cheaper than judging candidates one at a time.
	 * Recursive for combinators. Implemented per node rather than decided by the resolver, so a new
	 * building block declares its own strategy instead of being classified from outside.
	 */
	boolean isEnumeratePreferred();

	/** The direct children of {@code parentIdentity}. */
	record ChildOf(int parentIdentity) implements OntologyClassFilter {
		@Override public boolean isEnumeratePreferred() { return true; }
	}

	/** Every descendant of {@code ancestorIdentity} (its heritage), not including itself. */
	record DescendantOf(int ancestorIdentity) implements OntologyClassFilter {
		@Override public boolean isEnumeratePreferred() { return true; }
	}

	/** {@code classIdentity} together with every one of its descendants — the self-inclusive
	 *  counterpart to {@link DescendantOf}, and the class-side mirror of
	 *  {@link OntologyElementFilter.UnderDomain}. */
	record DomainOf(int classIdentity) implements OntologyClassFilter {
		@Override public boolean isEnumeratePreferred() { return true; }
	}

	/** The direct parents of {@code childIdentity}. */
	record ParentOf(int childIdentity) implements OntologyClassFilter {
		@Override public boolean isEnumeratePreferred() { return true; }
	}

	/** Every ancestor of {@code descendantIdentity}, not including itself. */
	record AncestorOf(int descendantIdentity) implements OntologyClassFilter {
		@Override public boolean isEnumeratePreferred() { return true; }
	}

	/** Intersection: classes satisfying both {@code left} and {@code right}. Prefers enumeration as
	 *  soon as either side can drive one; when both can, {@code left} drives (no cost estimation). */
	record And(OntologyClassFilter left, OntologyClassFilter right) implements OntologyClassFilter {
		@Override public boolean isEnumeratePreferred() {
			return left.isEnumeratePreferred() || right.isEnumeratePreferred();
		}
	}

	/** Union: classes satisfying either {@code left} or {@code right}. Can only be enumerated when
	 *  both sides can be — a union grows the set, so neither side alone bounds it. */
	record Or(OntologyClassFilter left, OntologyClassFilter right) implements OntologyClassFilter {
		@Override public boolean isEnumeratePreferred() {
			return left.isEnumeratePreferred() && right.isEnumeratePreferred();
		}
	}

	/** Relative complement: classes satisfying {@code kept} but not {@code excluded}. {@code excluded}
	 *  only ever removes members, so it is used purely as a test and its own preference is irrelevant. */
	record Subtract(OntologyClassFilter kept, OntologyClassFilter excluded) implements OntologyClassFilter {
		@Override public boolean isEnumeratePreferred() { return kept.isEnumeratePreferred(); }
	}

	/** Classes whose name contains {@code query} (case-insensitive). A bare predicate with no domain
	 *  of its own: to narrow some other filter by name, compose {@code And(base, NameContaining(q))}. */
	record NameContaining(String query) implements OntologyClassFilter {
		@Override public boolean isEnumeratePreferred() { return false; }
	}

	/** Client-supplied filtering logic — see {@link CustomOntologyFilterBase}. The implementation
	 *  declares its own preferred direction, so this node just forwards it. */
	record Custom(CustomOntologyFilterBase implementation) implements OntologyClassFilter {
		@Override public boolean isEnumeratePreferred() { return implementation.isEnumeratePreferred(); }
	}

}
