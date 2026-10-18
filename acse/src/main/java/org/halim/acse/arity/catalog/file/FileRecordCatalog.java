package org.halim.acse.arity.catalog.file;

import org.halim.acse.arity.ACSE_ComposableArityCatalog;
import org.halim.acs.arity.catalog.LongChunkInt;

import java.io.IOException;
import java.nio.file.Path;


/**
 * Several catalog rows in one file, an identity's values for every row side by side as one record. A value
 * is one position away - the record's start plus the field's offset - and a new identity is one more record
 * at the file's end, so the file grows without moving anything. The layout, how many rows and each one's
 * type, is fixed when the file is made: another row would move every record, which is making another file.
 * <p>
 * This one object serves as several rows, but an arity takes its catalog rows one object at a time, so a
 * row reaches an arity through its adapter, {@link FileRecordCatalogRow}. {@link #row} gives one per field;
 * added to an {@link ACSE_ComposableArityCatalog}, each field takes a slot of its own and its writes are
 * journaled there like any row's. The arity grows every row to each identity it mints, and every row here
 * grows the same file, so the first does the work and the rest find it done.
 * <p>
 * What a record buys over a file a row: an identity's values are one place, read together. What it costs:
 * reading one row over a range reads the pages of every row. A row read alone and in bulk wants a file of
 * its own, as {@link org.halim.acse.arity.catalog.file.IntegerValueCatalogFileLong} and its
 * siblings are.
 * */
public class FileRecordCatalog {

/** The file, shared by every row of this catalog. */
public final MappedRecordFile storage;

/** A new file of records with a field per type - {@link MappedRecordFile#LONG}, {@link MappedRecordFile#FLOAT}, ... - refused if it already holds data. */
public FileRecordCatalog(Path file, byte... types) throws IOException {
	this(file, MappedRecordFile.DEFAULT_CHUNKING, types);
}

public FileRecordCatalog(Path file, LongChunkInt chunking, byte... types) throws IOException {
	this(MappedRecordFile.create(file, chunking, types));
}

/** The catalog a file already holds, as a loaded row's {@link FileRecordCatalogRow#catalog} gives it. */
public FileRecordCatalog(MappedRecordFile storage) {
	this.storage = storage;
}


public int rowCount() { return storage.fieldCount(); }

public byte typeOf(int row) { return storage.typeOf(row); }

/** The adapter for one row. Two for the same row are the same row: an adapter holds nothing but where its field is. */
public FileRecordCatalogRow row(int row) { return new FileRecordCatalogRow(storage, row); }


}
