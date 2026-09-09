package org.halim.ontology.incubation.structurer;

import org.halim.ontology.incubation.structurer.sets.IdentitiesSet;

import java.util.ArrayList;

/**
 * The arity whose columns really are objects: a list of {@link CatalogModuleBase}, addressed by the
 * slot they sit at. This is the general case, the one that takes any storage form the catalogs
 * package offers and mixes widths and types freely in one arity.
 * <p>
 * What it costs is the thing {@link ArityModule} exists to let a client avoid. Every read is two
 * hops - table to module, module to storage - and the second is an interface call at a site that
 * sees every catalog implementation the program loaded, so it stops inlining early. A client whose
 * arity is three int columns pays that for nothing; they should implement {@code ArityModule}
 * directly over three arrays and skip this class. It earns its place when the columns are genuinely
 * heterogeneous, or when they already exist as objects and would only have to be unpacked.
 * <p>
 * The bulk copies below are where the two hops stop mattering: each forwards the whole range to the
 * module's own copy, so the pair of dispatches is paid once for the range rather than once for every
 * element. That is the only reason a modular arity can be read at speed at all, and it is why the
 * overrides are here rather than left to the interface defaults - the defaults would loop on
 * {@link #getIntFor}, which is exactly the two hops again.
 * <p>
 * Nothing here is multi thread safe.
 * */
public class ModularArityCatalog implements ArityModule {

/** Which identities exist. The columns are total over it; none of them decides membership. */
public IdentitiesSet occupied;
public ArrayList<CatalogModuleBase> catalogModules;
/**
 * The highest identity every column has been made to reach, not a count and not the occupied size -
 * an unsigned identity leaves a hole in the set without lowering what the columns address.
 * */
public long lastIndex = -1;

public ModularArityCatalog(IdentitiesSet occupied) {
	this.occupied = occupied;
	this.catalogModules = new ArrayList<>();
}


@Override
public IdentitiesSet occupied() { return occupied; }

@Override
public int catalogCount() { return catalogModules.size(); }

@Override
public CatalogModuleBase getCatalogModule(int catalog) { return catalogModules.get(catalog); }

/**
 * Appends a column and gives back the slot it landed at. The slot is the address from here on -
 * name lookup, if it comes, is for wiring and not for a loop over identities.
 * <p>
 * The new column is grown to the arity's current reach before it is added, so it is addressable for
 * every identity that already exists rather than only for the ones minted after it.
 * */
public int addCatalogModule(CatalogModuleBase module) {
	if(lastIndex >= 0) { module.ensureCapacity(lastIndex); }
	catalogModules.add(module);
	return catalogModules.size() - 1;
}


@Override
public double getEstimatedBytesPerIdentity(int catalog) {
	return catalogModules.get(catalog).getEstimatedBytesPerIdentity();
}

@Override
public int getPreferredType(int catalog) {
	return catalogModules.get(catalog).getPreferredType();
}


/** Every column follows the identity, and the answer is whether any of them had to move. */
@Override
public boolean ensureCapacity(long identity) {
	boolean grown = false;
	for(int i = 0; i < catalogModules.size(); i++) { grown |= catalogModules.get(i).ensureCapacity(identity); }
	lastIndex = identity;
	return grown;
}

@Override
public void truncate(long capacity) {
	for(int i = 0; i < catalogModules.size(); i++) { catalogModules.get(i).truncate(capacity); }
	lastIndex = Math.max(capacity, -1);
}


@Override
public int getIntFor(int catalog, long identity) { return catalogModules.get(catalog).getIntFor(identity); }
@Override
public long getLongFor(int catalog, long identity) { return catalogModules.get(catalog).getLongFor(identity); }
@Override
public double getDoubleFor(int catalog, long identity) { return catalogModules.get(catalog).getDoubleFor(identity); }
@Override
public boolean getBooleanFor(int catalog, long identity) { return catalogModules.get(catalog).getBooleanFor(identity); }
@Override
public Object getObjectFor(int catalog, long identity) { return catalogModules.get(catalog).getObjectFor(identity); }


/** One dispatch pair for the range, where the interface default would have paid one per element. */
@Override
public void copyIntsInto(int catalog, long from, long to, int[] dest, int offset) {
	catalogModules.get(catalog).copyIntsInto(from, to, dest, offset);
}

@Override
public void copyLongsInto(int catalog, long from, long to, long[] dest, int offset) {
	catalogModules.get(catalog).copyLongsInto(from, to, dest, offset);
}

@Override
public void copyDoublesInto(int catalog, long from, long to, double[] dest, int offset) {
	catalogModules.get(catalog).copyDoublesInto(from, to, dest, offset);
}

@Override
public void copyBooleansInto(int catalog, long from, long to, boolean[] dest, int offset) {
	catalogModules.get(catalog).copyBooleansInto(from, to, dest, offset);
}


}
