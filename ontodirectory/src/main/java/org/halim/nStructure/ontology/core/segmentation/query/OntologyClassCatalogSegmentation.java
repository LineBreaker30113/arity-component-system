package org.halim.nStructure.ontology.core.segmentation.query;

import org.halim.nStructure.ontology.core.segmentation.helper.OntologyCommandable;
import org.halim.nStructure.ontology.core.segmentation.helper.OntologyQueryable;

/**
 * A query-side port, so this only pulls in {@link AnyCatalogBase} (read). {@code setName}
 * stays its own dedicated abstract method, as it always was — the one deliberate exception, not a
 * blanket write surface.
 */
public interface OntologyClassCatalogSegmentation
      extends OntologyQueryable.ClassFamily, OntologyQueryable.BaseClassAnalytics, AnyCatalogBase {

/** Detail id under which this catalog stores each class's display name. */
int NAME_DETAIL = 0;

@OntologyQueryable.CheapQuery
String getName(int classIdentity);

@OntologyCommandable.BypassNonCommandConcerns
void setName(int classIdentity, String newName);


}
