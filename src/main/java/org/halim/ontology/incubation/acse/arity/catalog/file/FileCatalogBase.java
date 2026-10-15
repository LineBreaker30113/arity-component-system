package org.halim.ontology.incubation.acse.arity.catalog.file;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * A catalog row whose values all sit in one file, one record an identity: what every width of it shares.
 * The storage is {@link MappedRecordFile}, a record of one field, and lifecycle is the file's - growing
 * maps more of it, truncating gives the file back - so a subclass holds only its accessors, which read and
 * write its width at the record's place in the mapping.
 * <p>
 * A store copies the file into the store's folder and writes the file's layout and the reach; a load puts
 * that copy back at the file's place and maps it, so a generation stays a snapshot, at the cost of copying
 * the whole file at every store. Reading never copies: it is a load from the mapping.
 * */
public abstract class FileCatalogBase {

public final MappedRecordFile storage;

protected FileCatalogBase(MappedRecordFile storage) {
	this.storage = storage;
}


public boolean ensureCapacity(long identity) { return storage.ensureCapacity(identity); }

public void truncate(long capacity) { storage.truncate(capacity); }

/** The layout, with the file copied into the store's folder, then the reach. */
public boolean saveTo(DataOutputStream dos) throws IOException {
	storage.writeLayout(dos);
	dos.writeLong(storage.lastIndex);
	return true;
}

/** The file {@link #saveTo} names, put back from the loaded folder's copy and refused unless its one field is {@code type}. */
protected static MappedRecordFile reopen(DataInputStream dis, byte type) throws IOException {
	MappedRecordFile storage = MappedRecordFile.openLayout(dis);
	storage.requireFields(type);
	return storage;
}

/** The rest of what {@link #saveTo} wrote, after the layout {@link #reopen} read. A column in a file needs neither the core nor the journal. */
protected void readValues(DataInputStream dis) throws IOException {
	storage.reach(dis.readLong());
}


}
