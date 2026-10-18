package org.halim.ontology.legacy.core.segments.data;

import org.halim.ontology.legacy.core.segmentation.command.AnyCatalogMutableBase;
import org.halim.ontology.legacy.core.segmentation.query.AnyCatalogBase;

import java.util.ArrayList;

/**
 * Default engine-owned storage backing {@link OntologyClassCatalog}/{@link OntologyElementCatalog}
 * when a client doesn't supply its own. Structure of Arrays: detail types are rows, entries are
 * columns — and since this store's only job is holding the name detail, it's exactly one row (row
 * 0): a single growable {@code Object} array, indexed by {@code entryIdentity}. Any other
 * {@code detailTypeIdentity} is rejected rather than silently read/written, since there is no such
 * row. The typed {@code long}/{@code double}/{@code boolean} accessors on {@link AnyCatalogBase}/
 * {@link AnyCatalogMutableBase} exist so a <em>custom</em> store can back some other detail with
 * unboxed primitive storage instead of paying {@code Object} boxing overhead for it (e.g. a file
 * size as {@code long}) — this default never populates such a detail, so those three types throw
 * unconditionally here.
 */
public class OntologyCatalogDetailStore implements AnyCatalogBase, AnyCatalogMutableBase {

private final ArrayList<Object> nameRow = new ArrayList<>();

@Override
public Object getDetailObject(int entryIdentity, int detailTypeIdentity) {
    if (detailTypeIdentity > 0) throw unsupported();
    return entryIdentity < nameRow.size() ? nameRow.get(entryIdentity) : null;
}

@Override
public void setDetailObject(int entryIdentity, int detailTypeIdentity, Object value) {
    if(detailTypeIdentity > 0) throw unsupported();
    while (nameRow.size() <= entryIdentity) nameRow.add(null);
    nameRow.set(entryIdentity, value);
}

@Override
public long getDetailLong(int entryIdentity, int detailTypeIdentity) { throw unsupported(); }

@Override
public double getDetailDouble(int entryIdentity, int detailTypeIdentity) { throw unsupported(); }

@Override
public boolean getDetailBoolean(int entryIdentity, int detailTypeIdentity) { throw unsupported(); }

@Override
public void setDetailLong(int entryIdentity, int detailTypeIdentity, long value) { throw unsupported(); }

@Override
public void setDetailDouble(int entryIdentity, int detailTypeIdentity, double value) { throw unsupported(); }

@Override
public void setDetailBoolean(int entryIdentity, int detailTypeIdentity, boolean value) { throw unsupported(); }

private static UnsupportedOperationException unsupported() {
    return new UnsupportedOperationException(
          "OntologyCatalogDetailStore only stores the name detail (Object); "
                + "supply a custom CatalogModuleBase/AnyCatalogMutableBase for typed numeric/boolean details.");
}

}
