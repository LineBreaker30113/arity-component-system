package org.halim.ontology.incubation.acse_beta;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * One value per identity - a column - and the half of the contract that does not depend on what the
 * value is. This is what a column looks like when it is genuinely its own object; the enclosing
 * interface is what the same data looks like when it is not.
 * <p>
 * The five accessors are widened views over one stored value. A catalog implements the ones it can
 * answer exactly and inherits the rest, and every inherited one throws - so asking a floating catalog
 * for an object is a failure at the call, not a zero. That is the whole reason they are defaults
 * rather than abstract: a new catalog supplies what it has and is refused on the rest for free.
 * <p>
 * The two failures and the exactness rule are the enclosing interface's, unchanged: refusal by type
 * is {@link UnsupportedOperationException}, refusal by value is {@link ArithmeticException}, and
 * conversion is exact or it throws.
 *
 */
public interface ACSE_ArityCatalogRowBase {

    /**
     * The same constants as the table form, inherited here so an implementer need not qualify them.
     */
    int PREFERRED_INTEGER = ACSE_ArityComponentBase.PREFERRED_INTEGER;
    int PREFERRED_LONGI = ACSE_ArityComponentBase.PREFERRED_LONGI;
    int PREFERRED_FLOATING = ACSE_ArityComponentBase.PREFERRED_FLOATING;
    int PREFERRED_BOOL = ACSE_ArityComponentBase.PREFERRED_BOOL;
    int PREFERRED_OBJECT = ACSE_ArityComponentBase.PREFERRED_OBJECT;


    double getEstimatedBytesPerIdentity();

    int getPreferredType();


    /**
     * Makes {@code identity} addressable, growing storage if it is not already. True when anything had
     * to grow.
     * <p>
     * This and {@link #truncate} are the only part of the contract that does not depend on what a
     * catalog stores, and that is exactly why they are here: an arity mints an identity once and every
     * catalog attached to it has to follow, without the arity knowing any of their widths. Nothing in
     * this pair touches a value, so nothing in it boxes one.
     *
     */
    boolean ensureCapacity(long identity);

    /**
     * Lowers the ceiling to {@code capacity} and gives back what sat above it.
     */
    void truncate(long capacity);


    /**
     * Writes the column's values, or what it calculates them from, so that the class's
     * {@code public static loadFrom(ACSE_Core, ACSE_Journal, DataInputStream)} rebuilds the same column;
     * the core and the journal are there for a column calculated from other components. The arity
     * holding it calls this, inside the arity's own file.
     */
    boolean saveTo(DataOutputStream dos) throws IOException;


    default int getIntFor(long identity) {
        throw cannotAnswer("int");
    }

    default long getLongFor(long identity) {
        throw cannotAnswer("long");
    }

    default double getDoubleFor(long identity) {
        throw cannotAnswer("double");
    }

    default boolean getBooleanFor(long identity) {
        throw cannotAnswer("boolean");
    }

    default Object getObjectFor(long identity) {
        throw cannotAnswer("Object");
    }


    /**
     * The writes, the other side of the views: one value at an identity this column already reaches,
     * exact or refused the way a read is - refusal by type is {@link UnsupportedOperationException},
     * refusal by value is {@link ArithmeticException}. Only the two numeric families are written, since
     * they are the only ones stored; Java widens an int or a float argument on the way in.
     * <p>
     * Nothing here grows the column. Lifecycle stays with {@link #ensureCapacity}, which the arity drives
     * for every column at once, so a write cannot leave one column reaching further than the rest. Nor is
     * anything journaled: a column is storage, and the arity holding it records each write.
     */
    default void setLongFor(long identity, long value) {
        throw cannotAnswer("a written long");
    }

    default void setDoubleFor(long identity, double value) {
        throw cannotAnswer("a written double");
    }


    /**
     * The range views, the column form of the enclosing interface's. Same bargain: the defaults are the
     * per-element loop, and what they are worth is that a storage form can override them and copy whole
     * runs of its own array instead.
     * <p>
     * A column knows its own layout, so the override here can go further than a table's - a chunked
     * catalog whose stored width matches the destination copies a run per chunk and touches no element
     * individually at all.
     *
     */
    default void copyIntsInto(long from, long to, int[] dest, int offset) {
        for (long identity = from; identity < to; identity++) {
            dest[offset++] = getIntFor(identity);
        }
    }

    default void copyLongsInto(long from, long to, long[] dest, int offset) {
        for (long identity = from; identity < to; identity++) {
            dest[offset++] = getLongFor(identity);
        }
    }

    default void copyDoublesInto(long from, long to, double[] dest, int offset) {
        for (long identity = from; identity < to; identity++) {
            dest[offset++] = getDoubleFor(identity);
        }
    }

    default void copyBooleansInto(long from, long to, boolean[] dest, int offset) {
        for (long identity = from; identity < to; identity++) {
            dest[offset++] = getBooleanFor(identity);
        }
    }


    /**
     * The refusal every unimplemented accessor raises, naming what this catalog does hold.
     */
    default UnsupportedOperationException cannotAnswer(String asked) {
        return new UnsupportedOperationException(
                getClass().getSimpleName() + " holds " + ACSE_ArityComponentBase.typeName(getPreferredType()) + ", asked for " + asked);
    }


}
