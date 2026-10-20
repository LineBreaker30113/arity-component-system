package org.halim.nStructure.ontology.core.segmentation.query;

import org.halim.nStructure.ontology.core.segmentation.helper.OntologyQueryable;

public interface OntologyInplaceQuerySegmentation extends OntologyQueryable.BaseInplaceHierarchy {
    
    @FunctionalInterface
    interface OntologyElementQuery { void query(int elementIdentity); }

    @OntologyQueryable.BypassNonQueryConcerns
    void forEachSpecificallySelfElement(int classIdentity, OntologyElementQuery eq);
    @OntologyQueryable.BypassNonQueryConcerns
    void forEachElementUnderDomain(int classIdentity, OntologyElementQuery eq);

    @OntologyQueryable.BypassNonQueryConcerns
    void forEachClassSatisfyingFilter(OntologyClassFilter ocf, OntologyClassQuery cq);
    @OntologyQueryable.BypassNonQueryConcerns
    void forEachElementSatisfyingFilter(OntologyElementFilter oef, OntologyElementQuery eq);

}
