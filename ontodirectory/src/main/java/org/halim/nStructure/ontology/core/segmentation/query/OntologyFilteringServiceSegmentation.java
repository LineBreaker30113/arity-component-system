package org.halim.nStructure.ontology.core.segmentation.query;

import org.halim.nStructure.ontology.core.segmentation.helper.OntologyQueryable;

import java.util.Set;

/**
 * Resolves {@link OntologyClassFilter}/{@link OntologyElementFilter} trees into actual matches.
 * Filters are plain data describing what to match; this service does the matching, computing each
 * building block directly against the hierarchy/classification/analytics engines instead of
 * naively testing every class or element against a predicate.
 */
public interface OntologyFilteringServiceSegmentation {

	@OntologyQueryable.BypassNonQueryConcerns
	int[] getAllClasses(OntologyClassFilter filter);

	@OntologyQueryable.BypassNonQueryConcerns
	Set<Integer> getClassesAsSet(OntologyClassFilter filter);

	@OntologyQueryable.BypassNonQueryConcerns
	void forEachClassSatisfying(OntologyClassFilter filter, OntologyQueryable.BaseInplaceClass.OntologyClassQuery query);

	@OntologyQueryable.BypassNonQueryConcerns
	int[] getAllElements(OntologyElementFilter filter);

	@OntologyQueryable.BypassNonQueryConcerns
	Set<Integer> getElementsAsSet(OntologyElementFilter filter);

	@OntologyQueryable.BypassNonQueryConcerns
	void forEachElementSatisfying(OntologyElementFilter filter, OntologyInplaceQuerySegmentation.OntologyElementQuery query);

}
