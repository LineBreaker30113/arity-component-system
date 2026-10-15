package org.halim.ontology.incubation.acse.arity.catalog;

import org.halim.ontology.incubation.acse.arity.ACS_ArityComponentBase;


/**
 * A catalog of integral values. {@link #getData} is the one thing an implementation supplies; the
 * views onto it follow from that and are exact or they throw.
 * <p>
 * The stored width is the implementation's business - the int variants below answer the same
 * {@code getData} as the long ones, widening on the way out, and differ only in what
 * {@link #getEstimatedBytesPerIdentity} reports.
 * <p>
 * Unlike the floating catalogs, narrowing here is not a rounding: a long that does not fit an int
 * wraps, and a wrapped value reads back as a perfectly plausible different value. So the width a
 * catalog was built at is a promise about its contents, and {@link IntSizedIntegerValueCatalog}
 * exists to carry that promise in the type rather than in a comment.
 * */
public interface IntegerValueCatalog extends ACS_ArityCatalogRowBase {

long getData(long identity);


@Override
default int getPreferredType() { return PREFERRED_LONGI; }

@Override
default long getLongFor(long identity) {
	return getData(identity);
}

@Override
default int getIntFor(long identity) {
	return ACS_ArityComponentBase.exactInt(getData(identity));
}

@Override
default double getDoubleFor(long identity) {
	return ACS_ArityComponentBase.exactDouble(getData(identity));
}

/** The reverse of {@link #getDoubleFor}: a double is written only when it is an exact long. */
@Override
default void setDoubleFor(long identity, double value) {
	setLongFor(identity, ACS_ArityComponentBase.exactLong(value));
}


/**
 * An integral catalog whose values are known to fit in an int, which is what
 * {@link ACS_ArityComponentBase#PREFERRED_INTEGER} means: integers can be used carefreely.
 * <p>
 * The promise is structural rather than checked. {@link #getIntData} is the stored value, so there
 * is nothing to verify on the way out - {@link #getIntFor} cannot throw here, and neither can
 * {@link #getDoubleFor}, since every int is an exact double. That is the difference the subtype
 * buys: on a plain {@link IntegerValueCatalog} both of those are conversions that may refuse a
 * value, and a caller has to be ready for it.
 * <p>
 * Both widths are usable from either side. Reading through {@code getData} widens, which is free and
 * exact; writing from a long goes through a check, since that direction is the one that can lose.
 * */
interface IntSizedIntegerValueCatalog extends IntegerValueCatalog {
	
	int getIntData(long identity);
	
	/** Widening, so the wide interface works unchanged on a narrow catalog. Always exact. */
	@Override
	default long getData(long identity) { return getIntData(identity); }
	
	@Override
	default int getPreferredType() { return PREFERRED_INTEGER; }
	
	/** The stored value itself - no conversion, so no refusal. */
	@Override
	default int getIntFor(long identity) { return getIntData(identity); }
	
	/** Every int is an exact double; the 2^53 question the wide form has to ask cannot arise. */
	@Override
	default double getDoubleFor(long identity) { return getIntData(identity); }
	
}


}
