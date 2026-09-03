package org.halim.ontology.core.segmentation.query;

import org.halim.ontology.core.segmentation.helper.OntologyQueryable;

/**
 * Generic read side of a "catalog", addressed by {@code (entryIdentity, detailTypeIdentity)}
 * instead of one dedicated field per attribute. Structure of Arrays: each detail type is a row —
 * one array spanning every entry — and each entry (class or element) is a column, i.e. one slot
 * within that row; {@code detailTypeIdentity} selects the row, {@code entryIdentity} selects the
 * column within it. Lets a client plug in its own storage for {@link OntologyClassCatalogSegmentation}
 * or {@link OntologyElementCatalogSegmentation} (e.g. a real SoA-backed store) instead of always
 * getting the engine's built-in default, and lets query code (like the filter resolver) evaluate
 * any detail generically instead of needing one accessor binding per catalog kind.
 */
public interface AnyCatalogBase {

@OntologyQueryable.CheapQuery
Object getDetailObject(int entryIdentity, int detailTypeIdentity);
@OntologyQueryable.CheapQuery
long getDetailLong(int entryIdentity, int detailTypeIdentity);
@OntologyQueryable.CheapQuery
double getDetailDouble(int entryIdentity, int detailTypeIdentity);
@OntologyQueryable.CheapQuery
boolean getDetailBoolean(int entryIdentity, int detailTypeIdentity);

}
