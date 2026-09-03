package org.halim.ontology.view.filter;

import org.halim.ontology.core.segmentation.query.CustomOntologyFilterBase;

/**
 * Base class for a filter whose cheap direction is judging one candidate at a time. A subclass
 * answers exactly one question: does this one class, or this one element, satisfy me.
 * <p>
 * It is never asked to produce a matching set — not from a candidate set, and not from the whole
 * ontology. Turning this filter into a set of matches, when a filter tree genuinely leaves no cheaper
 * route, is the engine's job: it runs {@link #test(int)} across the candidates it already holds, or at
 * worst across the whole universe, which it materializes itself and at most once per resolution.
 * <p>
 * This is the right base class whenever answering "which ones match?" would mean walking everything
 * anyway. Choosing it over {@link AbstractEnumeratingFilterBase} <em>is</em> the strategy declaration
 * — a filter can never claim a direction it does not actually implement.
 * <p>
 * Adds nothing to {@link CustomOntologyFilterBase}, which is a functional interface: a test-preferring
 * filter with no state of its own is just as well written as a lambda,
 * {@code new OntologyClassFilter.Custom(id -> ...)}. This class exists for symmetry with
 * {@link AbstractEnumeratingFilterBase} and as the discoverable place to document the testing
 * strategy. See {@link CustomOntologyFilterBase} for the concurrency contract every custom filter runs
 * under.
 */
public abstract class AbstractTestingFilterBase implements CustomOntologyFilterBase {

	@Override
	public abstract boolean test(int candidateIdentity);

}
