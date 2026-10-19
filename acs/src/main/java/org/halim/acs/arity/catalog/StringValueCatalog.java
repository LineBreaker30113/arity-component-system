package org.halim.acs.arity.catalog;


/**
 * A catalog of strings. {@link #getData} is the one thing an implementation supplies, and a string is
 * an Object too, so {@link #getObjectFor} answers with the same value. A string never written reads as
 * "", the zero of strings: a catalog is total, and which identities exist is the occupied set's
 * question.
 * <p>
 * The arity journals a write by its difference, as it does a number's, but a string changes length:
 * the difference is the two strings' chars XORed - the shorter read as zeros past its end - and the
 * change in length, which one step applies both ways. A {@link CodedStringValueCatalog} stores
 * fixed-width codes instead, and its difference is the code's {@code old ^ new}, like an integral
 * column's.
 * */
public interface StringValueCatalog extends ACS_ArityCatalogRowBase {

String getData(long identity);


@Override
default int getPreferredType() { return PREFERRED_STRING; }

@Override
default String getStringFor(long identity) {
	return getData(identity);
}

@Override
default Object getObjectFor(long identity) {
	return getData(identity);
}


/**
 * A string catalog over a dictionary: each identity holds the code of its string among the distinct
 * strings written so far, code 0 being "". The code is fixed-width, so the arity journals a write as
 * the code's {@code old ^ new} and undoes it through {@link #setCode}, never touching the dictionary;
 * a string stays in the dictionary once added, as capacity stays after an undo.
 * */
interface CodedStringValueCatalog extends StringValueCatalog {
	int getCode(long identity);

	/** Sets a code the dictionary already holds; the arity's undo and redo write through this. */
	void setCode(long identity, int code);
}


}
