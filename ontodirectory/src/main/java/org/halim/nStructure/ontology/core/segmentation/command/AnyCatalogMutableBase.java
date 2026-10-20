package org.halim.nStructure.ontology.core.segmentation.command;

import org.halim.nStructure.ontology.core.segmentation.helper.OntologyCommandable;
import org.halim.nStructure.ontology.core.segmentation.query.AnyCatalogBase;

/**
 * Write side of {@link AnyCatalogBase} — sets a value in an entry's (column) slot of a detail
 * type's (row) array, addressed by {@code (entryIdentity, detailTypeIdentity)}. A catalog's
 * default storage implements both this and {@link AnyCatalogBase}; a client supplying its own
 * storage to the engine does the same.
 */
public interface AnyCatalogMutableBase {

@OntologyCommandable.BypassNonCommandConcerns
void setDetailObject(int entryIdentity, int detailTypeIdentity, Object value);
@OntologyCommandable.BypassNonCommandConcerns
void setDetailLong(int entryIdentity, int detailTypeIdentity, long value);
@OntologyCommandable.BypassNonCommandConcerns
void setDetailDouble(int entryIdentity, int detailTypeIdentity, double value);
@OntologyCommandable.BypassNonCommandConcerns
void setDetailBoolean(int entryIdentity, int detailTypeIdentity, boolean value);

}
