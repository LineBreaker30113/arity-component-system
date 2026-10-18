package org.halim.acses.daemon;

import org.halim.acs.ACS_Core;
import org.halim.acs.ACS_PersistenceManager;

import java.io.IOException;
import java.nio.file.Path;


/** One database the daemon holds: its folder, the manager that stores it there, and its core. */
class ACSES_Database {

final Path folder;
final ACS_PersistenceManager manager;
final ACS_Core core;

ACSES_Database(Path folder, ACS_PersistenceManager manager, ACS_Core core) {
	this.folder = folder;
	this.manager = manager;
	this.core = core;
}

/**
 * Closes the loaders the folder's jars were opened through, the manager's and the core's, so the jars
 * are let go; the program's own loader, which a created database's core comes from, is left alone.
 * Nothing may use the database afterwards.
 * */
void close() throws IOException {
	try { close(manager); }
	finally { close(core); }
}

static void close(Object built) throws IOException {
	if(built.getClass().getClassLoader() instanceof ACS_PersistenceManager.StoredClassLoader loader) { loader.close(); }
}

}
