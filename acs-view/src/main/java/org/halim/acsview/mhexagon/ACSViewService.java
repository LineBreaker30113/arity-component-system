package org.halim.acsview.mhexagon;

import java.nio.file.Path;
import java.util.List;


/**
 * The port the GUI programs against, in the place OntoDirectory's {@code OntoDirectoryService} has: the
 * GUI never sees how a request reaches acsed. Every method may take as long as a program run, so the GUI
 * calls them off the event thread, and every one may throw {@link Refusal}, carrying acsed's or acsess's
 * own words.
 * <p>
 * The databases are acsed's, not this program's: one a terminal loaded is listed and shown as well, and
 * one this program loaded stays loaded for the terminal until it is closed.
 * */
public interface ACSViewService {

/** Starts acsed when nothing answers on its socket, and answers how it stands, as {@code acsess boot} prints it. */
String boot();

/** Makes a database in the folder and loads it, named after the folder. */
void createDatabase(Path folder);

void loadDatabase(Path folder);

/** Stores the database, then unloads it from acsed. */
void closeDatabase(DatabaseService database);

/** The database the workspace shows, or null on the welcome page. */
DatabaseService getActiveDatabase();

void dispatchDatabaseChooseRequest(String name);

/** acsed's databases, as {@code databases} lists them. */
List<DatabaseService> getLoadedDatabases();

void addACSViewServiceListener(ACSViewServiceListener listener);

/**
 * Lines as a person types them into an acsess session, sent as one connection when they are all queued:
 * the console's way, as an SQL tool's editor runs a script. A line acsess cannot translate refuses the
 * whole run, since nothing has been sent then.
 * */
List<Answer> runSession(List<String> lines, boolean continueOnRefusal);

/** One request of a session and its answer: done, refused, or not run after a refusal. */
record Answer(String typed, boolean done, String text) {}

/** Tells the listeners an action was refused; the GUI shows it and goes on. */
void reportRefusal(String action, RuntimeException refusal);

interface ACSViewServiceListener {
	void onGUI_Change();
	void onDatabaseLoad(DatabaseService database);
	void showRefusal(String action, String reason);
}

/**
 * One database acsed holds, addressed by its name. Each change is one request, so one journal activity,
 * and carries the comment {@code acs-view}.
 * */
interface DatabaseService {
	String getName();
	Path getFolder();
	Components getComponents();
	long[] getIdentities(int arity);
	/** A value per identity and row, as acsess prints it; null where the row's type cannot be read through acsess. */
	String[][] getValues(int arity, List<Row> rows, long[] identities);
	void setValue(int arity, Row row, long identity, String value);
	long captureIdentity(int arity);
	void deleteIdentity(int arity, long identity);
	void addArity(String description);
	int addCatalogRow(int arity, String className, String description);
	void undo();
	void redo();
	int getCurrentActivityIndex();
	int getPendingActivityCount();
	String store();
}

/** What a row's values are read and written as: the get and set requests of the arity that take them. */
enum ValueType {
	LONG("Long"), DOUBLE("Double"), BOOLEAN("Boolean"), STRING("String"), OBJECT(null);

	/** The part of {@code get<part>For} and {@code set<part>For}; null for a row acsess cannot read. */
	public final String requestPart;

	ValueType(String requestPart) { this.requestPart = requestPart; }

	/**
	 * From what {@code components} prints for a row: the row's class, named by ACSE's convention after the
	 * contract it implements, or the column's type for an arity that keeps no row objects. A class the
	 * convention does not cover is OBJECT.
	 * */
	public static ValueType of(String kind) {
		String simple = kind.substring(kind.lastIndexOf('.') + 1);
		if(simple.startsWith("IntegerValueCatalog") || kind.equals("int") || kind.equals("long")) { return LONG; }
		if(simple.startsWith("FloatingValueCatalog") || kind.equals("floating")) { return DOUBLE; }
		if(simple.startsWith("BooleanValueCatalog") || kind.equals("boolean")) { return BOOLEAN; }
		if(simple.startsWith("StringValueCatalog") || kind.equals("String")) { return STRING; }
		return OBJECT;
	}
}

/** The database's components, as {@code components} lists them; a removed slot keeps its place, with null fields. */
record Components(List<Arity> arities, List<Structure> structures) {}

record Arity(int slot, String className, String description, long identities, List<Row> rows) {
	public boolean isRemoved() { return className == null; }
}

record Row(int slot, String kind, String description) {
	public boolean isRemoved() { return kind == null; }
	public ValueType type() { return isRemoved() ? ValueType.OBJECT : ValueType.of(kind); }
}

record Structure(int slot, String className, String description, int[] arities) {
	public boolean isRemoved() { return className == null; }
}

/** A request acsess or acsed refused, or a program run that went wrong; the message is their own text. */
class Refusal extends RuntimeException {
	public Refusal(String message) { super(message); }
}

}
