package org.halim.ontology.incubation.acse_beta.catalogs;

import org.halim.ontology.incubation.acse_beta.ACSE_ArityCatalogRowBase;
import org.halim.ontology.incubation.acse_beta.ACSE_ArityComponentBase;

/**
 * A catalog of integral values. {@link #getData} is the one thing an implementation supplies; the
 * views onto it follow from that and are exact or they throw.
 * <p>
 * The stored width is the implementation's business, not this interface's - a catalog backed by
 * bytes and one backed by longs answer the same {@code getData}, and differ only in what
 * {@link #getPreferredType} and {@link #getEstimatedBytesPerIdentity} report.
 * */
public interface LongIntCatalog extends ACSE_ArityCatalogRowBase {

long getData(long identity);


@Override
default int getPreferredType() { return PREFERRED_LONGI; }

@Override
default long getLongFor(long identity) {
	return getData(identity);
}

@Override
default int getIntFor(long identity) {
	return ACSE_ArityComponentBase.exactInt(getData(identity));
}

@Override
default double getDoubleFor(long identity) {
	return ACSE_ArityComponentBase.exactDouble(getData(identity));
}


}
