package org.halim.acs.arity.catalog;


/**
 * A catalog of booleans. {@link #getData} is the one thing an implementation supplies. A boolean is
 * one bit of difference, so the arity journals a write as {@code old ^ new} over it, the same step an
 * integral column's write takes.
 * */
public interface BooleanValueCatalog extends ACS_ArityCatalogRowBase {

boolean getData(long identity);


@Override
default int getPreferredType() { return PREFERRED_BOOL; }

@Override
default boolean getBooleanFor(long identity) {
	return getData(identity);
}


}
