package org.halim.ontology.legacy.core.segmentation.query;

/**
 * Describes a predicate over ontology elements as a small AST of building blocks. Instances are plain
 * data — resolving a filter into actual matching elements is
 * {@link OntologyFilteringServiceSegmentation}'s job, not this type's, so building a filter never
 * touches the engine. Native, int-identity port of the combinators
 * {@code legacyDelete.OntologyFilter} implemented against the legacy engine.
 * <p>
 * Every node conceptually supports two operations: <b>enumerate</b> (produce the matching set) and
 * <b>test</b> (judge one candidate). {@link #isEnumeratePreferred()} reports which one is a node's
 * cheap direction; the resolver anchors on enumeration wherever a tree allows it, and only walks the
 * whole element universe when nothing in the tree can anchor one. Testing is the default, and a node
 * has to prove the opposite — which is why {@code Or} demands it of both children while {@code And}
 * and {@code Subtract} need it of only one: {@code And} and {@code Subtract} remove instances from an
 * already-valid set, so one branch can drive the enumeration while the other is used purely as a test,
 * whereas {@code Or} can grow the set and so cannot be restricted to either side's enumeration.
 * <p>
 * {@link Custom} is the extension point: it carries a client-supplied
 * {@link CustomOntologyFilterBase}, which is how filtering reaches data the engine knows nothing
 * about.
 */
public sealed interface OntologyElementFilter {

	/**
	 * Whether producing this node's matching set is cheaper than judging candidates one at a time.
	 * Recursive for combinators. Implemented per node rather than decided by the resolver, so a new
	 * building block declares its own strategy instead of being classified from outside.
	 */
	boolean isEnumeratePreferred();

	/** Only the elements explicitly tagged with {@code classIdentity}. */
	record DirectElementOf(int classIdentity) implements OntologyElementFilter {
		@Override public boolean isEnumeratePreferred() { return true; }
	}

	/** Elements tagged with {@code classIdentity} or any of its descendant classes. */
	record UnderDomain(int classIdentity) implements OntologyElementFilter {
		@Override public boolean isEnumeratePreferred() { return true; }
	}

	/** Intersection: elements satisfying both {@code left} and {@code right}. Prefers enumeration as
	 *  soon as either side can drive one; when both can, {@code left} drives (no cost estimation). */
	record And(OntologyElementFilter left, OntologyElementFilter right) implements OntologyElementFilter {
		@Override public boolean isEnumeratePreferred() {
			return left.isEnumeratePreferred() || right.isEnumeratePreferred();
		}
	}

	/** Union: elements satisfying either {@code left} or {@code right}. Can only be enumerated when
	 *  both sides can be — a union grows the set, so neither side alone bounds it. */
	record Or(OntologyElementFilter left, OntologyElementFilter right) implements OntologyElementFilter {
		@Override public boolean isEnumeratePreferred() {
			return left.isEnumeratePreferred() && right.isEnumeratePreferred();
		}
	}

	/** Relative complement: elements satisfying {@code kept} but not {@code excluded}. {@code excluded}
	 *  only ever removes members, so it is used purely as a test and its own preference is irrelevant. */
	record Subtract(OntologyElementFilter kept, OntologyElementFilter excluded) implements OntologyElementFilter {
		@Override public boolean isEnumeratePreferred() { return kept.isEnumeratePreferred(); }
	}

	/** Elements whose name contains {@code query} (case-insensitive). A bare predicate with no domain
	 *  of its own: to narrow some other filter by name, compose {@code And(base, NameContaining(q))}. */
	record NameContaining(String query) implements OntologyElementFilter {
		@Override public boolean isEnumeratePreferred() { return false; }
	}

	/** Client-supplied filtering logic — see {@link CustomOntologyFilterBase}. The implementation
	 *  declares its own preferred direction, so this node just forwards it. */
	record Custom(CustomOntologyFilterBase implementation) implements OntologyElementFilter {
		@Override public boolean isEnumeratePreferred() { return implementation.isEnumeratePreferred(); }
	}

}
