package org.halim.acsview.acsess;

import org.halim.acsview.mhexagon.ACSViewService;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/**
 * The service over acsess, in the place OntoDirectory's {@code ArchiverServer} has. It holds no database
 * of its own: the list is whatever {@code databases} prints, so a database a terminal loaded shows up at
 * the next refresh, and one a terminal unloaded is gone from it.
 * */
public class AcsessServer implements ACSViewService {

public final Acsess acsess;
final List<ACSViewServiceListener> listeners = new ArrayList<>();
volatile AcsessDatabase active;

public AcsessServer(Acsess acsess) { this.acsess = acsess; }

@Override public String boot() { return acsess.command("boot", AcsessDatabase.comment()); }

@Override
public void createDatabase(Path folder) {
	acsess.command("createDB", "path=" + folder.toAbsolutePath().normalize(), AcsessDatabase.comment());
	choose(byFolder(folder));
}

@Override
public void loadDatabase(Path folder) {
	acsess.command("loadDB", "path=" + folder.toAbsolutePath().normalize(), AcsessDatabase.comment());
	choose(byFolder(folder));
}

@Override
public void closeDatabase(DatabaseService database) {
	AcsessDatabase closed = (AcsessDatabase) database;
	closed.store();
	closed.unload();
	if(closed.equals(active)) { active = null; }
	for(ACSViewServiceListener listener : listeners) { listener.onGUI_Change(); }
}

@Override public DatabaseService getActiveDatabase() { return active; }

@Override
public void dispatchDatabaseChooseRequest(String name) {
	for(DatabaseService database : getLoadedDatabases()) {
		if(database.getName().equals(name)) {
			choose((AcsessDatabase) database);
			return;
		}
	}
	throw new Refusal("acsed holds no database named " + name);
}

void choose(AcsessDatabase database) {
	active = database;
	for(ACSViewServiceListener listener : listeners) { listener.onDatabaseLoad(database); }
}

AcsessDatabase byFolder(Path folder) {
	Path wanted = folder.toAbsolutePath().normalize();
	for(DatabaseService database : getLoadedDatabases()) {
		if(database.getFolder().equals(wanted)) { return (AcsessDatabase) database; }
	}
	throw new Refusal("acsed lists no database in " + wanted);
}

/** One line per database, its name and its folder, tab-separated, the folder last. */
@Override
public List<DatabaseService> getLoadedDatabases() {
	String printed = acsess.command("databases");
	ArrayList<DatabaseService> databases = new ArrayList<>();
	if(printed.equals("no databases") || printed.isEmpty()) { return databases; }
	for(String line : printed.split("\n")) {
		int tab = line.lastIndexOf('\t');
		if(tab < 0) { throw new Refusal("databases printed a line this program does not read: " + line); }
		databases.add(new AcsessDatabase(acsess, line.substring(0, tab), Path.of(line.substring(tab + 1))));
	}
	return databases;
}

@Override public List<Answer> runSession(List<String> lines, boolean continueOnRefusal) { return acsess.typedSession(lines, continueOnRefusal); }

@Override public void addACSViewServiceListener(ACSViewServiceListener listener) { listeners.add(listener); }

@Override
public void reportRefusal(String action, RuntimeException refusal) {
	String reason = refusal.getMessage() != null ? refusal.getMessage() : refusal.getClass().getName();
	for(ACSViewServiceListener listener : listeners) { listener.showRefusal(action, reason); }
}

}
