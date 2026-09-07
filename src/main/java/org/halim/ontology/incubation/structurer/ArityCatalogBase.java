package org.halim.ontology.incubation.structurer;

/**
 * What every catalog answers to, whatever it stores. A catalog is one value per identity; this is
 * the part of that contract which does not depend on what the value is.
 * <p>
 * The five accessors are widened views over one stored value, not five stored values. A catalog
 * implements the ones it can answer exactly and inherits the rest, and every inherited one throws -
 * so asking a floating catalog for an object is a failure at the call, not a zero. That is the whole
 * reason they are defaults rather than abstract: a new catalog supplies what it has and is refused
 * on the rest for free.
 * <p>
 * Two failures live here and they are not the same failure:
 * <ul>
 *   <li>{@link UnsupportedOperationException} - this catalog cannot answer that type at all. The
 *       question was wrong, and no identity would have answered it.</li>
 *   <li>{@link ArithmeticException} - the catalog can answer that type, but this value does not
 *       survive the conversion. The question was fine; the value did not fit.</li>
 * </ul>
 * The engine narrows nothing silently - a wrapped identity or a rounded-away digit is
 * indistinguishable from a real one - so conversion is exact or it throws. {@link #getDoubleFor} on
 * an integral catalog is the case worth remembering: it is exact well past 2^53 for some values and
 * not for others, so the check is per value rather than per catalog.
 * */
public interface ArityCatalogBase {

/** Can use integers carefreely. */
public static final int PREFERRED_INTEGER = -1;
/** Cannot use integers carefreely. */
public static final int PREFERRED_LONGI = -2;

public static final int PREFERRED_FLOATING = 0;
public static final int PREFERRED_BOOL = 1;
public static final int PREFERRED_OBJECT = 2;


double getEstimatedBytesPerIdentity(int catalog);
int getPreferredType(int catalog);


default int getIntData(int catalog, long element) { throw cannotAnswer(catalog, "int"); }
default long getLongFor(int catalog, long identity) { throw cannotAnswer(catalog, "long"); }
default double getDoubleFor(int catalog, long identity) { throw cannotAnswer(catalog, "double"); }
default boolean getBooleanFor(int catalog, long identity) { throw cannotAnswer(catalog, "boolean"); }
default Object getObjectFor(int catalog, long identity) { throw cannotAnswer(catalog, "Object"); }


/**
 * What every catalog answers to, whatever it stores. A catalog is one value per identity; this is
 * the part of that contract which does not depend on what the value is.
 * <p>
 * The five accessors are widened views over one stored value, not five stored values. A catalog
 * implements the ones it can answer exactly and inherits the rest, and every inherited one throws -
 * so asking a floating catalog for an object is a failure at the call, not a zero. That is the whole
 * reason they are defaults rather than abstract: a new catalog supplies what it has and is refused
 * on the rest for free.
 * <p>
 * Two failures live here and they are not the same failure:
 * <ul>
 *   <li>{@link UnsupportedOperationException} - this catalog cannot answer that type at all. The
 *       question was wrong, and no identity would have answered it.</li>
 *   <li>{@link ArithmeticException} - the catalog can answer that type, but this value does not
 *       survive the conversion. The question was fine; the value did not fit.</li>
 * </ul>
 * The engine narrows nothing silently - a wrapped identity or a rounded-away digit is
 * indistinguishable from a real one - so conversion is exact or it throws. {@link #getDoubleFor} on
 * an integral catalog is the case worth remembering: it is exact well past 2^53 for some values and
 * not for others, so the check is per value rather than per catalog.
 * */
interface AnyCatalogBase {
	
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
	 * */
	boolean ensureCapacity(long identity);
	
	/** Lowers the ceiling to {@code capacity} and gives back what sat above it. */
	void truncate(long capacity);
	
	
	default int getIntFor(long identity) { throw cannotAnswer("int"); }
	default long getLongFor(long identity) { throw cannotAnswer("long"); }
	default double getDoubleFor(long identity) { throw cannotAnswer("double"); }
	default boolean getBooleanFor(long identity) { throw cannotAnswer("boolean"); }
	default Object getObjectFor(long identity) { throw cannotAnswer("Object"); }
	
	
	/** The refusal every unimplemented accessor raises, naming what this catalog does hold. */
	default UnsupportedOperationException cannotAnswer(String asked) {
		return new UnsupportedOperationException(
			getClass().getSimpleName() + " holds " + typeName(getPreferredType()) + ", asked for " + asked);
	}
	
	static String typeName(int preferredType) {
		return switch(preferredType) {
			case PREFERRED_INTEGER -> "int";
			case PREFERRED_LONGI -> "long";
			case PREFERRED_FLOATING -> "floating";
			case PREFERRED_BOOL -> "boolean";
			case PREFERRED_OBJECT -> "Object";
			default -> "type " + preferredType;
		};
	}
	
	
	/**
	 * The exact conversions, kept here so every catalog fails the same way on the same value.
	 * <p>
	 * Each is a round trip: convert, convert back, and refuse if the value moved. The magnitude guards
	 * are there because a narrowing cast saturates rather than wrapping, which can make a round trip
	 * agree on a value that never fitted - {@code (long)(double) Long.MAX_VALUE} is the case.
	 * */
	static int exactInt(long value) {
		return Math.toIntExact(value);
	}
	
	static short exactShort(long value) {
		short result = (short) value;
		if(result != value) { throw new ArithmeticException(value + " is not an exact short"); }
		return result;
	}
	
	static byte exactByte(long value) {
		byte result = (byte) value;
		if(result != value) { throw new ArithmeticException(value + " is not an exact byte"); }
		return result;
	}
	
	static int exactInt(double value) {
		int result = (int) value;
		if(result != value) { throw new ArithmeticException(value + " is not an exact int"); }
		return result;
	}
	
	static long exactLong(double value) {
		long result = (long) value;
		if(result != value || value >= 9.223372036854776E18) {
			throw new ArithmeticException(value + " is not an exact long");
		}
		return result;
	}
	
	static double exactDouble(long value) {
		double result = value;
		if((long) result != value || result >= 9.223372036854776E18) {
			throw new ArithmeticException(value + " has no exact double");
		}
		return result;
	}
	
	
}

/** The refusal every unimplemented accessor raises, naming what this catalog does hold. */
default UnsupportedOperationException cannotAnswer(int catalog, String asked) {
	return new UnsupportedOperationException(
		getClass().getSimpleName() + " holds " + typeName(getPreferredType(catalog)) + ", asked for " + asked);
}

static String typeName(int preferredType) {
	return switch(preferredType) {
		case PREFERRED_INTEGER -> "int";
		case PREFERRED_LONGI -> "long";
		case PREFERRED_FLOATING -> "floating";
		case PREFERRED_BOOL -> "boolean";
		case PREFERRED_OBJECT -> "Object";
		default -> "type " + preferredType;
	};
}


/**
 * The exact conversions, kept here so every catalog fails the same way on the same value.
 * <p>
 * Each is a round trip: convert, convert back, and refuse if the value moved. The magnitude guards
 * are there because a narrowing cast saturates rather than wrapping, which can make a round trip
 * agree on a value that never fitted - {@code (long)(double) Long.MAX_VALUE} is the case.
 * */
static int exactInt(long value) {
	return Math.toIntExact(value);
}

static short exactShort(long value) {
	short result = (short) value;
	if(result != value) { throw new ArithmeticException(value + " is not an exact short"); }
	return result;
}

static byte exactByte(long value) {
	byte result = (byte) value;
	if(result != value) { throw new ArithmeticException(value + " is not an exact byte"); }
	return result;
}

static int exactInt(double value) {
	int result = (int) value;
	if(result != value) { throw new ArithmeticException(value + " is not an exact int"); }
	return result;
}

static long exactLong(double value) {
	long result = (long) value;
	if(result != value || value >= 9.223372036854776E18) {
		throw new ArithmeticException(value + " is not an exact long");
	}
	return result;
}

static double exactDouble(long value) {
	double result = value;
	if((long) result != value || result >= 9.223372036854776E18) {
		throw new ArithmeticException(value + " has no exact double");
	}
	return result;
}


}
