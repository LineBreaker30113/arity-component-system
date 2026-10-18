package org.halim.acses.daemon;

import org.halim.acs.ACSE_Core;
import org.halim.acs.ACSE_DescriptionManager;
import org.halim.acs.ACSE_PersistenceManager;
import org.halim.acs.arity.ACS_ArityComponentBase;
import org.halim.acs.arity.catalog.ACS_ArityCatalogRowBase;
import org.halim.acs.structure.ACS_StructureComponentBase;
import org.halim.acse.ACSE_CoreBasic;
import org.halim.acse.ACSE_PersistenceManagerBasic;
import org.halim.acses.acsess.ACSES_Request;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;


/**
 * What each command does, over the databases the daemon holds by name. A loaded database's classes are
 * its own, built from its folder's jars, so every command reaches a core through the contracts only: an
 * {@code instanceof} against one of ACSE's classes would be false for every loaded database. ACSE's
 * classes are named only where a database gets them, in {@link #createDB}.
 * <p>
 * A refused command leaves the databases held as they were. Paths must be absolute, since the daemon's
 * working folder is not the caller's and a relative path would name another place.
 * */
class ACSES_Commands {

static final String COMMANDS = "boot, createDB, loadDB, components, storeDB, unloadDB, databases, shutdown";

/** Where this daemon listens, for boot's first answer. */
final Path socket;
/** By name, in the order they came. */
final LinkedHashMap<String, ACSES_Database> databases = new LinkedHashMap<>();
/** Whether a boot was answered yet: the first is told the daemon started, every later one that it is running. */
boolean booted;
/** Set by shutdown; the daemon stops once its answer is sent. */
boolean stopping;

ACSES_Commands(Path socket) { this.socket = socket; }


/** The answer to one request; whatever a command throws becomes a refusal giving its reason. */
ACSES_Request.Answer run(List<String> arguments) {
	try {
		Request request = new Request(arguments);
		return switch(request.command) {
			case "boot" -> boot(request);
			case "createDB" -> createDB(request);
			case "loadDB" -> loadDB(request);
			case "components" -> components(request);
			case "storeDB" -> storeDB(request);
			case "unloadDB" -> unloadDB(request);
			case "databases" -> databases(request);
			case "shutdown" -> shutdown(request);
			default -> refused("unknown command " + request.command + "; the commands: " + COMMANDS);
		};
	} catch(IOException | RuntimeException | LinkageError e) { return refused(reason(e)); }
}

ACSES_Request.Answer boot(Request request) {
	request.expect(0, false, "boot");
	long pid = ProcessHandle.current().pid();
	ACSES_Request.Answer answer = done(booted ? "running, pid " + pid : "started, pid " + pid + ", socket " + socket);
	booted = true;
	return answer;
}

/**
 * Makes a database in a new or empty folder, as persistence.md's first case has it: ACSES writes the
 * manager's jar into the folder, the manager is built from that jar, and a new core - ACSE's basic one
 * with its basic journal, from ACSES's own classes - is stored through it. Every store then puts its own
 * jar of classes into its numbered folder, through the manager. A failure after the folder is made
 * leaves what was written there, and the folder, holding files now, is refused until it is emptied.
 * */
ACSES_Request.Answer createDB(Request request) throws IOException {
	request.expect(1, true, "createDB <path> [--calledBy <name>]");
	Path folder = absolute(request.plain.get(0));
	String name = nameFor(request, folder);
	if(Files.exists(folder) && !Files.isDirectory(folder)) { return refused(folder + " is not a folder"); }
	if(Files.isDirectory(folder) && !isEmpty(folder)) { return refused(folder + " holds files"); }
	Files.createDirectories(folder);
	Path jar = folder.resolve(ACSE_PersistenceManager.MANAGER);
	ACSES_PersistenceJar.write(jar, ACSE_PersistenceManagerBasic.class);
	ACSE_PersistenceManager manager = managerFrom(jar, folder);
	ACSE_CoreBasic core = new ACSE_CoreBasic();
	List<String> warnings;
	try { warnings = manager.store(core); }
	catch(IOException | RuntimeException | LinkageError e) {
		ACSES_Database.close(manager);
		throw e;
	}
	databases.put(name, new ACSES_Database(folder, manager, core));
	return done(withLines("created " + name + " at " + folder, warnings));
}

/** Loads a stored folder through the manager its own jar holds, under the name the request gives, else the folder's own. */
ACSES_Request.Answer loadDB(Request request) throws IOException {
	request.expect(1, true, "loadDB <path> [--calledBy <name>]");
	Path folder = absolute(request.plain.get(0));
	String name = nameFor(request, folder);
	ACSE_PersistenceManager manager = ACSE_PersistenceManager.open(folder);
	ACSE_Core core;
	try { core = manager.load(); }
	catch(IOException | RuntimeException | LinkageError e) {
		ACSES_Database.close(manager);
		throw e;
	}
	databases.put(name, new ACSES_Database(folder, manager, core));
	return done("loaded " + name + " from " + manager.current());
}

/**
 * One line per arity - slot, class, description, identity count - with its catalog rows indented under
 * it - slot, class, description - then one line per structure - slot, class, description, the arities
 * it spans. The fields are tab-separated, "-" stands for no description, and an arity that keeps no row
 * objects shows its column's type where a row's class would be.
 * */
ACSES_Request.Answer components(Request request) {
	request.expect(1, false, "components <name>");
	ACSE_Core core = database(request.plain.get(0)).core;
	ACSE_DescriptionManager descriptions = core instanceof ACSE_DescriptionManager manager ? manager : null;
	StringBuilder lines = new StringBuilder();
	for(int slot = 0; slot < core.getArityCount(); slot++) {
		ACS_ArityComponentBase arity = core.getArity(slot);
		if(arity == null) {
			line(lines, "arity " + slot, "(removed)");
			continue;
		}
		long identities = arity.occupied().size();
		line(lines, "arity " + slot, arity.getClass().getName(), descriptions == null ? null : descriptions.getArityDescription(slot), identities + (identities == 1 ? " identity" : " identities"));
		for(int row = 0; row < arity.catalogCount(); row++) {
			String kind = rowKind(arity, row);
			if(kind == null) { line(lines, "\tcatalog " + row, "(removed)"); }
			else { line(lines, "\tcatalog " + row, kind, descriptions == null ? null : descriptions.getArityCatalogRowDescription(slot, row)); }
		}
	}
	for(int slot = 0; slot < core.getStructureCount(); slot++) {
		ACS_StructureComponentBase structure = core.getStructure(slot);
		if(structure == null) {
			line(lines, "structure " + slot, "(removed)");
			continue;
		}
		StringBuilder spans = new StringBuilder("arities");
		for(int i = 0; i < structure.getArityCount(); i++) { spans.append(' ').append(structure.getArityIdentity(i)); }
		line(lines, "structure " + slot, structure.getClass().getName(), descriptions == null ? null : descriptions.getStructureDescription(slot), spans.toString());
	}
	return done(lines.isEmpty() ? "no components" : lines.toString());
}

/** The row's class; the column's type for an arity that keeps no row objects; null for a removed row. */
static String rowKind(ACS_ArityComponentBase arity, int row) {
	ACS_ArityCatalogRowBase catalog;
	try { catalog = arity.getCatalogRow(row); }
	catch(UnsupportedOperationException e) { return ACS_ArityComponentBase.typeName(arity.getPreferredType(row)); }
	return catalog == null ? null : catalog.getClass().getName();
}

ACSES_Request.Answer storeDB(Request request) throws IOException {
	request.expect(1, false, "storeDB <name>");
	String name = request.plain.get(0);
	ACSES_Database database = database(name);
	List<String> warnings = database.manager.store(database.core);
	return done(withLines("stored " + name + " as " + database.manager.current(), warnings));
}

/** Forgets the database without storing it, and lets go of its jars. */
ACSES_Request.Answer unloadDB(Request request) throws IOException {
	request.expect(1, false, "unloadDB <name>");
	String name = request.plain.get(0);
	ACSES_Database database = database(name);
	databases.remove(name);
	database.close();
	return done("unloaded " + name + ", not stored");
}

ACSES_Request.Answer databases(Request request) {
	request.expect(0, false, "databases");
	StringBuilder lines = new StringBuilder();
	databases.forEach((name, database) -> line(lines, name, database.folder.toString()));
	return done(lines.isEmpty() ? "no databases" : lines.toString());
}

/** Stops the daemon once this answer is sent, storing nothing; the answer names what was not stored. */
ACSES_Request.Answer shutdown(Request request) {
	request.expect(0, false, "shutdown");
	stopping = true;
	return done(databases.isEmpty() ? "stopping" : "stopping; not stored: " + String.join(", ", databases.keySet()));
}


/** The name the request gives, else the folder's own; refuses one in use. */
String nameFor(Request request, Path folder) {
	Path last = folder.getFileName();
	String name = request.calledBy != null ? request.calledBy : last == null ? "" : last.toString();
	if(name.isEmpty()) { throw new IllegalArgumentException("name the database with --calledBy"); }
	ACSES_Database holder = databases.get(name);
	if(holder != null) { throw new IllegalArgumentException(name + " is in use, by " + holder.folder); }
	return name;
}

ACSES_Database database(String name) {
	ACSES_Database database = databases.get(name);
	if(database == null) { throw new IllegalArgumentException("no database is called " + name); }
	return database;
}

/** The path as given, refused when relative: the daemon's working folder is not the caller's. */
static Path absolute(String path) {
	Path given = Path.of(path);
	if(!given.isAbsolute()) { throw new IllegalArgumentException(path + " is relative; give an absolute path"); }
	return given.normalize();
}

static boolean isEmpty(Path folder) throws IOException {
	try(Stream<Path> entries = Files.list(folder)) { return entries.findAny().isEmpty(); }
}

/**
 * The manager built from the folder's jar through its public {@code (Path)} constructor, as
 * {@link ACSE_PersistenceManager#open} builds one; open itself needs the root file the first store
 * writes, which a folder being created has not got yet.
 * */
static ACSE_PersistenceManager managerFrom(Path jar, Path folder) throws IOException {
	ACSE_PersistenceManager.StoredClassLoader loader = new ACSE_PersistenceManager.StoredClassLoader(jar.toUri().toURL(), ACSE_PersistenceManager.class.getClassLoader());
	String name = ACSE_PersistenceManagerBasic.class.getName();
	try { return (ACSE_PersistenceManager) Class.forName(name, true, loader).getConstructor(Path.class).newInstance(folder); }
	catch(ReflectiveOperationException | ClassCastException | LinkageError e) {
		loader.close();
		throw new IOException("cannot build " + name + " from " + jar + ": " + reason(e), e);
	}
}

/** One line of tab-separated fields, "-" standing for a field with nothing in it. */
static void line(StringBuilder lines, String... fields) {
	if(!lines.isEmpty()) { lines.append('\n'); }
	for(int i = 0; i < fields.length; i++) {
		if(i > 0) { lines.append('\t'); }
		lines.append(fields[i] == null ? "-" : fields[i]);
	}
}

static String withLines(String first, List<String> more) { return more.isEmpty() ? first : first + "\n" + String.join("\n", more); }

static String reason(Throwable e) { return e.getMessage() != null ? e.getMessage() : e.getClass().getName(); }

static ACSES_Request.Answer done(String text) { return new ACSES_Request.Answer(ACSES_Request.DONE, text); }

static ACSES_Request.Answer refused(String text) { return new ACSES_Request.Answer(ACSES_Request.REFUSED, text); }


/**
 * A request taken apart: its command, its plain arguments, and the name {@code --calledBy} gives.
 * {@code --comment} is taken and left to the log, since no command here hands one to a core method.
 * */
static final class Request {
	final String command;
	final ArrayList<String> plain = new ArrayList<>();
	String calledBy;

	Request(List<String> arguments) {
		if(arguments.isEmpty()) { throw new IllegalArgumentException("no command; the commands: " + COMMANDS); }
		command = arguments.get(0);
		for(int i = 1; i < arguments.size(); i++) {
			String argument = arguments.get(i);
			if(!argument.equals("--comment") && !argument.equals("--calledBy")) {
				plain.add(argument);
				continue;
			}
			if(++i == arguments.size()) { throw new IllegalArgumentException(argument + " needs a value"); }
			if(argument.equals("--calledBy")) { calledBy = arguments.get(i); }
		}
	}

	/** Refuses a request whose plain arguments are not {@code count} in number, or that gives a name to a command taking none. */
	void expect(int count, boolean named, String usage) {
		if(plain.size() != count || (!named && calledBy != null)) { throw new IllegalArgumentException("usage: " + usage); }
	}
}

}
