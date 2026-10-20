package org.halim.nStructure.ontology.core.segmentation.command;

import org.halim.nStructure.ontology.core.segmentation.query.OntologyInplaceQuerySegmentation;
import org.halim.nStructure.ontology.core.segmentation.query.OntologyClassFilter;
import org.halim.nStructure.ontology.core.segmentation.query.OntologyElementFilter;
import org.halim.nStructure.ontology.core.segmentation.helper.OntologyCommandable;

public interface OntologyInplaceCommandingSegmentation extends OntologyInplaceQuerySegmentation, OntologyCommandable.BaseInplaceHierarchy {

    @FunctionalInterface
    interface OntologyElementAction { void act(int elementIdentity); }

    @OntologyCommandable.BypassNonCommandConcerns
    void forEachSpecificallySelfElement(int classIdentity, OntologyElementAction ea);
    @OntologyCommandable.BypassNonCommandConcerns
    void forEachElementUnderDomain(int classIdentity, OntologyElementAction ea);

    @OntologyCommandable.BypassNonCommandConcerns
    void forEachClassSatisfyingFilter(OntologyClassFilter ocf, OntologyClassAction ca);
    @OntologyCommandable.BypassNonCommandConcerns
    void forEachElementSatisfyingFilter(OntologyElementFilter oef, OntologyElementAction ea);

}
