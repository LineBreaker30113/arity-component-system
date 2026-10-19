package org.halim.acses.daemon;

import org.halim.acs.ACS_Core;
import org.halim.acs.ACS_DescriptionManager;
import org.halim.acs.ACS_Journal;
import org.halim.acs.ACS_PersistenceManager;
import org.halim.acs.arity.ACS_ArityComponentBase;
import org.halim.acs.arity.catalog.ACS_ArityCatalogRowBase;
import org.halim.acs.structure.ACS_StructureComponentBase;
import org.halim.acse.ACSE_CoreBasic;
import org.halim.acse.ACSE_PersistenceManagerBasic;
import org.halim.acses.acsess.ACSES_Request;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
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

static final String COMMANDS = "boot, createDB, loadDB, components, storeDB, migrate, inspectAt, endInspection, inspectionState, waitInspectionReturn, acquire, release, unloadDB, databases, shutdown, Commit; and beginSession, which the acses program runs";
/**
 * The flags every command's reader knows: a valued one is its name and "=" joined to its value in one
 * argument, a switch is "+" and its name; any other argument is plain.
 * */
static final List<String> VALUED = List.of("calledBy=", "comment=", "toPath=", "newName="),
	SWITCHES = List.of("+ignoreInspection", "+WithFormerVersions", "+WithOtherFiles", "+deleteOld", "+changesOnly", "+Duplicate", "+force");
/** The commands that only read, which an acquisition taken with +changesOnly lets go without its key. */
static final List<String> READING = List.of("boot", "components", "databases", "inspectionState", "waitInspectionReturn");
/** The commands whose first plain argument names a database, so an acquisition on that database covers them. */
static final List<String> NAMED = List.of("components", "storeDB", "migrate", "inspectAt", "endInspection", "inspectionState", "waitInspectionReturn", "acquire", "release", "unloadDB");

/** Where this daemon listens, for boot's first answer. */
final Path socket;
/** By name, in the order they came. */
final LinkedHashMap<String, ACSES_Database> databases = new LinkedHashMap<>();
/** Whether a boot was answered yet: the first is told the daemon started, every later one that it is running. */
boolean booted;
/** Set by shutdown; the daemon stops once its answer is sent. */
boolean stopping;
/** The clients a wait holds open, by database name; the daemon owns the connections. */
final LinkedHashMap<String, ArrayList<Held>> waiting = new LinkedHashMap<>();
/** Held clients whose answer is ready, for the daemon to send once the current request is answered. */
final ArrayList<Held> ready = new ArrayList<>();
/** The name the last wait holds for, until the daemon hands its client to {@link #hold}. */
String holding;
/** The acquisition on the whole daemon, null when there is none; it and a database's acquisition never stand together, since a request carries one key. */
Acquisition daemonAcquisition;
/** The acquisitions on databases, by name. */
final HashMap<String, Acquisition> acquisitions = new HashMap<>();
final Random random = new Random();
/** Set while a Commit runs its requests, so a wait among them cannot hold. */
boolean committing;

ACSES_Commands(Path socket) { this.socket = socket; }


/** The answer to one request, a Commit among them; whatever a command throws becomes a refusal giving its reason. */
ACSES_Request.Answer run(List<String> arguments) {
	if(!arguments.isEmpty() && arguments.get(0).equals("Commit")) {
		try { return commit(arguments); }
		catch(RuntimeException e) { return refused(reason(e)); }
	}
	return runOne(arguments);
}

/** One request, its key first when it gives one: a first argument matching an acquisition's key is taken off before the command is read. */
ACSES_Request.Answer runOne(List<String> arguments) {
	try {
		String key = !arguments.isEmpty() && isKey(arguments.get(0)) ? arguments.get(0) : null;
		Request request = new Request(key == null ? arguments : arguments.subList(1, arguments.size()));
		checkKey(request, key);
		return switch(request.command) {
			case "boot" -> boot(request);
			case "createDB" -> createDB(request);
			case "loadDB" -> loadDB(request);
			case "components" -> components(request);
			case "storeDB" -> storeDB(request);
			case "migrate" -> migrate(request);
			case "inspectAt" -> inspectAt(request);
			case "endInspection" -> endInspection(request);
			case "inspectionState" -> inspectionState(request);
			case "waitInspectionReturn" -> waitInspectionReturn(request);
			case "acquire" -> acquire(request);
			case "release" -> release(request);
			case "Commit" -> refused("a Commit cannot run inside a Commit");
			case "beginSession" -> refused("beginSession runs in the acses program, which sends the session at its Commit");
			case "unloadDB" -> unloadDB(request);
			case "databases" -> databases(request);
			case "shutdown" -> shutdown(request);
			default -> refused("unknown command " + request.command + "; the commands: " + COMMANDS);
		};
	} catch(IOException | RuntimeException | LinkageError e) { return refused(reason(e)); }
}

ACSES_Request.Answer boot(Request request) {
	request.expect(0, "boot");
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
	request.expect(1, "createDB <path> [calledBy=<name>]", "calledBy=");
	Path folder = absolute(request.plain.get(0));
	String name = nameFor(request, folder);
	if(Files.exists(folder) && !Files.isDirectory(folder)) { return refused(folder + " is not a folder"); }
	if(Files.isDirectory(folder) && !isEmpty(folder)) { return refused(folder + " holds files"); }
	Files.createDirectories(folder);
	Path jar = folder.resolve(ACS_PersistenceManager.MANAGER);
	ACSES_PersistenceJar.write(jar, ACSE_PersistenceManagerBasic.class);
	ACS_PersistenceManager manager = managerFrom(jar, folder);
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

/**
 * Loads a stored folder through the manager its own jar holds, under the name the request gives, else
 * the folder's own. A folder another name holds is refused without +Duplicate, since two cores storing
 * into one folder is likely a mistake; +Duplicate where nothing else holds the folder only warns.
 * */
ACSES_Request.Answer loadDB(Request request) throws IOException {
	request.expect(1, "loadDB <path> [calledBy=<name>] [+Duplicate]", "calledBy=", "+Duplicate");
	Path folder = absolute(request.plain.get(0));
	String name = nameFor(request, folder);
	String holder = holderOf(folder, null);
	if(holder != null && !request.has("+Duplicate")) { return refused(folder + " is held already, by " + holder + "; loadDB +Duplicate loads it again"); }
	ACS_PersistenceManager manager = ACS_PersistenceManager.open(folder);
	ACS_Core core;
	try { core = manager.load(); }
	catch(IOException | RuntimeException | LinkageError e) {
		ACSES_Database.close(manager);
		throw e;
	}
	databases.put(name, new ACSES_Database(folder, manager, core));
	String warning = holder == null && request.has("+Duplicate") ? "\nwarning: +Duplicate given, but nothing else holds " + folder : "";
	return done("loaded " + name + " from " + manager.current() + warning);
}

/**
 * One line per arity - slot, class, description, identity count - with its catalog rows indented under
 * it - slot, class, description - then one line per structure - slot, class, description, the arities
 * it spans. The fields are tab-separated, "-" stands for no description, and an arity that keeps no row
 * objects shows its column's type where a row's class would be.
 * */
ACSES_Request.Answer components(Request request) {
	request.expect(1, "components <name>");
	ACS_Core core = database(request.plain.get(0)).core;
	ACS_DescriptionManager descriptions = core instanceof ACS_DescriptionManager manager ? manager : null;
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

/**
 * Stores into the database's folder; during an inspection, only with {@code +ignoreInspection}, since
 * the store would be of the inspected state. {@code toPath=} stores a copy into another folder, absent or
 * empty, made as createDB makes one, and is never refused for an inspection: the database stays on its own
 * folder.
 * */
ACSES_Request.Answer storeDB(Request request) throws IOException {
	request.expect(1, "storeDB <name> [toPath=<path>] [+ignoreInspection]", "toPath=", "+ignoreInspection");
	String name = request.plain.get(0);
	ACSES_Database database = database(name);
	String toPath = request.value("toPath=");
	if(toPath != null) {
		Path folder = absolute(toPath);
		makeEmptyFolder(folder);
		ACS_PersistenceManager manager = newManager(folder);
		try {
			List<String> warnings = manager.store(database.core);
			return done(withLines("stored " + name + " as " + manager.current() + ", a copy", warnings));
		} finally { ACSES_Database.close(manager); }
	}
	ACS_Journal journal = database.core.getJournal();
	if(journal.isInspecting() && !request.has("+ignoreInspection")) { return refused(underInspection(name, "storeDB") + "; storeDB +ignoreInspection stores it, toPath= stores it elsewhere"); }
	List<String> warnings = database.manager.store(database.core);
	return done(withLines("stored " + name + " as " + database.manager.current(), warnings));
}

/**
 * Moves the database to another folder, absent or empty and outside its own. Without
 * {@code +WithFormerVersions} the new folder is made as {@code storeDB toPath=} makes one; with it, the
 * pointer, the manager's jar and the stored versions are copied first, then a store is made through the
 * copied manager. {@code +WithOtherFiles} also copies every other entry at the root, folders whole.
 * The new folder is then loaded back, and that core is the database from here on, under
 * {@code newName=} if given: its undo history begins there. {@code +deleteOld} deletes the old folder
 * only after the load back, and is refused while another name holds it. A failure before the load back leaves the database as it was and what was
 * written into the new folder.
 * */
ACSES_Request.Answer migrate(Request request) throws IOException {
	request.expect(2, "migrate <name> <path> [newName=<newName>] [+WithFormerVersions] [+WithOtherFiles] [+deleteOld] [+ignoreInspection]",
		"newName=", "+WithFormerVersions", "+WithOtherFiles", "+deleteOld", "+ignoreInspection");
	String name = request.plain.get(0);
	ACSES_Database database = database(name);
	Path folder = absolute(request.plain.get(1));
	String newName = request.value("newName=") != null ? request.value("newName=") : name;
	ACSES_Database holder = databases.get(newName);
	if(holder != null && holder != database) { return refused(newName + " is in use, by " + holder.folder); }
	if(database.core.getJournal().isInspecting() && !request.has("+ignoreInspection")) { return refused(underInspection(name, "migrate") + "; migrate +ignoreInspection moves it"); }
	String other = holderOf(database.folder, database);
	if(other != null && request.has("+deleteOld")) { return refused(other + " holds " + database.folder + " too, so +deleteOld would delete its folder"); }
	if(folder.startsWith(database.folder)) { return refused(folder + " is inside " + database.folder); }
	makeEmptyFolder(folder);
	boolean former = request.has("+WithFormerVersions");
	try(Stream<Path> entries = Files.list(database.folder)) {
		for(Path entry : (Iterable<Path>) entries::iterator) {
			if(isRecognized(entry) ? former : request.has("+WithOtherFiles")) { copyTree(entry, folder.resolve(entry.getFileName())); }
		}
	}
	ACS_PersistenceManager manager = former ? ACS_PersistenceManager.open(folder) : newManager(folder);
	List<String> warnings;
	ACS_Core core;
	try {
		warnings = manager.store(database.core);
		core = manager.load();
	} catch(IOException | RuntimeException | LinkageError e) {
		ACSES_Database.close(manager);
		throw e;
	}
	databases.remove(name);
	databases.put(newName, new ACSES_Database(folder, manager, core));
	Acquisition acquisition = acquisitions.remove(name);
	if(acquisition != null) { acquisitions.put(newName, acquisition); }
	answerWaits(name, refused(name + " was migrated to " + folder + " before an endInspection"));
	String old;
	try {
		database.close();
		if(request.has("+deleteOld")) {
			deleteTree(database.folder);
			old = "deleted the old folder " + database.folder;
		} else { old = "the old folder " + database.folder + " is kept"; }
	} catch(IOException e) { old = "cannot let go of or delete the old folder " + database.folder + ": " + reason(e); }
	return done(withLines("migrated " + name + " to " + manager.current() + " as " + newName + "; " + old, warnings));
}

/** Leaves the state right after the transaction holding the activity; the first inspectAt keeps the way back, which endInspection takes. */
ACSES_Request.Answer inspectAt(Request request) {
	request.expect(2, "inspectAt <name> <activityIndex>");
	String name = request.plain.get(0);
	ACS_Journal journal = database(name).core.getJournal();
	int activityIndex;
	try { activityIndex = Integer.parseInt(request.plain.get(1)); }
	catch(NumberFormatException e) { return refused(request.plain.get(1) + " is no activity index"); }
	journal.inspectAt(activityIndex);
	return done(name + " under inspection: " + position(journal));
}

/** Returns to where the first inspectAt left, and answers every wait held for the database. */
ACSES_Request.Answer endInspection(Request request) {
	request.expect(1, "endInspection <name>");
	String name = request.plain.get(0);
	ACS_Journal journal = database(name).core.getJournal();
	journal.endInspection();
	ACSES_Request.Answer answer = done(name + " returned: " + position(journal));
	answerWaits(name, answer);
	return answer;
}

/** Whether the database is under inspection, its current activity index and its pending count, at once. */
ACSES_Request.Answer inspectionState(Request request) {
	request.expect(1, "inspectionState <name>");
	String name = request.plain.get(0);
	ACS_Journal journal = database(name).core.getJournal();
	return done(name + (journal.isInspecting() ? " is under inspection: " : " is not under inspection: ") + position(journal));
}

/**
 * During an inspection, gives back null, and the daemon holds the client through {@link #hold} until
 * endInspection answers it, or unloadDB, migrate or shutdown refuses it; holding takes no thread,
 * so other requests go on meanwhile. Without an inspection, the return has happened, and it answers at
 * once. Inside a Commit it cannot hold, so it is refused there during an inspection.
 * */
ACSES_Request.Answer waitInspectionReturn(Request request) {
	request.expect(1, "waitInspectionReturn <name>");
	String name = request.plain.get(0);
	ACS_Journal journal = database(name).core.getJournal();
	if(!journal.isInspecting()) { return done(name + " is not under inspection: " + position(journal)); }
	if(committing) { return refused(name + " is under inspection, and a wait inside a Commit cannot hold"); }
	holding = name;
	return null;
}

/**
 * Acquires the database, or without a name the whole daemon, and answers the key alone: from here on
 * every request the acquisition covers must give the key as its first argument, so a request meant for
 * another state of things is refused rather than run. It is for operational safety, not security: the
 * log shows the key. With +changesOnly the commands that only read go without it. An acquisition
 * where one stands already is refused, the daemon's and a database's included, since a request
 * carries one key.
 * */
ACSES_Request.Answer acquire(Request request) {
	request.expect(Math.min(request.plain.size(), 1), "acquire [<name>] [+changesOnly]", "+changesOnly");
	if(daemonAcquisition != null) { return refused("the daemon is acquired already"); }
	String key;
	do { key = String.format("%08x", random.nextInt()); } while(isKey(key));
	if(request.plain.isEmpty()) {
		if(!acquisitions.isEmpty()) { return refused("databases are acquired: " + String.join(", ", acquisitions.keySet())); }
		daemonAcquisition = new Acquisition(key, request.has("+changesOnly"));
	} else {
		String name = request.plain.get(0);
		database(name);
		if(acquisitions.containsKey(name)) { return refused(name + " is acquired already"); }
		acquisitions.put(name, new Acquisition(key, request.has("+changesOnly")));
	}
	return done(key);
}

/**
 * Ends the acquisition of the database, or without a name of the daemon. The request gives that
 * acquisition's key, which {@link #checkKey} saw to, or +force, for a key lost: the requester is taken
 * to mean well, and an acquisition guards against mistakes only.
 * */
ACSES_Request.Answer release(Request request) {
	request.expect(Math.min(request.plain.size(), 1), "[<key>] release [<name>] [+force]", "+force");
	String forced = request.has("+force") ? ", without its key" : "";
	if(request.plain.isEmpty()) {
		if(daemonAcquisition == null) { return refused("the daemon is not acquired"); }
		daemonAcquisition = null;
		return done("released the daemon" + forced);
	}
	String name = request.plain.get(0);
	if(acquisitions.remove(name) == null) { return refused(name + " is not acquired"); }
	return done("released " + name + forced);
}

/**
 * Runs the requests a session queued back to back, so no other request comes between them: after
 * "Commit" and an optional +continueOnRefusal, each request is its argument count, then its
 * arguments, its key first when it gives one. The first refusal stops the rest unless
 * +continueOnRefusal. The answer gives each request and its answer in order, and is a refusal when
 * any request was refused or not run.
 * */
ACSES_Request.Answer commit(List<String> arguments) {
	int at = 1;
	boolean continueOnRefusal = at < arguments.size() && arguments.get(at).equals("+continueOnRefusal");
	if(continueOnRefusal) { at++; }
	ArrayList<List<String>> queued = new ArrayList<>();
	while(at < arguments.size()) {
		int count;
		try { count = Integer.parseInt(arguments.get(at)); }
		catch(NumberFormatException e) { throw new IllegalArgumentException("Commit: " + arguments.get(at) + " is no argument count"); }
		if(count < 0 || count > arguments.size() - at - 1) { throw new IllegalArgumentException("Commit: a request of " + count + " arguments runs past the end"); }
		queued.add(arguments.subList(at + 1, at + 1 + count));
		at += 1 + count;
	}
	if(queued.isEmpty()) { return done("committed nothing"); }
	StringBuilder lines = new StringBuilder();
	boolean stopped = false, refusedAny = false;
	committing = true;
	try {
		for(List<String> one : queued) {
			line(lines, "> " + one.stream().map(ACSES_Daemon::quoted).collect(Collectors.joining(" ")));
			if(stopped || stopping) {
				line(lines, "  not run");
				refusedAny = true;
				continue;
			}
			ACSES_Request.Answer answer = runOne(one);
			String text = answer.status == ACSES_Request.DONE ? answer.text : "refused: " + answer.text;
			line(lines, "  " + text.replace("\n", "\n  "));
			if(answer.status != ACSES_Request.DONE) {
				refusedAny = true;
				stopped = !continueOnRefusal;
			}
		}
	} finally { committing = false; }
	return refusedAny ? refused(lines.toString()) : done(lines.toString());
}

/** Forgets the database without storing it, lets go of its jars, and refuses the waits held for it. */
ACSES_Request.Answer unloadDB(Request request) throws IOException {
	request.expect(1, "unloadDB <name>");
	String name = request.plain.get(0);
	ACSES_Database database = database(name);
	databases.remove(name);
	acquisitions.remove(name);
	answerWaits(name, refused(name + " was unloaded before an endInspection"));
	database.close();
	return done("unloaded " + name + ", not stored");
}

ACSES_Request.Answer databases(Request request) {
	request.expect(0, "databases");
	StringBuilder lines = new StringBuilder();
	databases.forEach((name, database) -> line(lines, name, database.folder.toString()));
	return done(lines.isEmpty() ? "no databases" : lines.toString());
}

/** Stops the daemon once this answer is sent, storing nothing, and refuses every wait held; the answer names what was not stored. */
ACSES_Request.Answer shutdown(Request request) {
	request.expect(0, "shutdown");
	stopping = true;
	for(String name : List.copyOf(waiting.keySet())) { answerWaits(name, refused("the daemon stopped before an endInspection of " + name)); }
	return done(databases.isEmpty() ? "stopping" : "stopping; not stored: " + String.join(", ", databases.keySet()));
}


/** Takes the client of the wait just answered with null. */
void hold(Held held) {
	waiting.computeIfAbsent(holding, name -> new ArrayList<>()).add(held);
	holding = null;
}

/** Makes the waits held for the database ready, each with the answer. */
void answerWaits(String name, ACSES_Request.Answer answer) {
	ArrayList<Held> held = waiting.remove(name);
	if(held == null) { return; }
	for(Held one : held) { one.answer = answer; }
	ready.addAll(held);
}

boolean isKey(String argument) {
	if(daemonAcquisition != null && daemonAcquisition.key.equals(argument)) { return true; }
	for(Acquisition acquisition : acquisitions.values()) { if(acquisition.key.equals(argument)) { return true; } }
	return false;
}

/** Refuses a request an acquisition covers that does not give the acquisition's key; an acquisition taken with +changesOnly lets the reading commands go, and release +force goes always. */
void checkKey(Request request, String key) {
	if(request.command.equals("release") && request.has("+force")) { return; }
	boolean reading = READING.contains(request.command);
	if(daemonAcquisition != null && !(reading && daemonAcquisition.changesOnly) && !daemonAcquisition.key.equals(key)) { throw new IllegalStateException("the daemon is acquired; give its key as the first argument"); }
	if(!NAMED.contains(request.command) || request.plain.isEmpty()) { return; }
	String name = request.plain.get(0);
	Acquisition acquisition = acquisitions.get(name);
	if(acquisition != null && !(reading && acquisition.changesOnly) && !acquisition.key.equals(key)) { throw new IllegalStateException(name + " is acquired; give its key as the first argument"); }
}

/** What inspectAt, endInspection, inspectionState and waitInspectionReturn answer with. */
static String position(ACS_Journal journal) { return "current activity index " + journal.getCurrentActivityIndex() + ", " + journal.getPendingActivityCount() + " pending"; }

static String underInspection(String name, String command) { return name + " is under inspection, so " + command + " would keep the inspected state; endInspection ends it"; }

/** The name the request gives, else the folder's own; refuses one in use. */
String nameFor(Request request, Path folder) {
	Path last = folder.getFileName();
	String calledBy = request.value("calledBy=");
	String name = calledBy != null ? calledBy : last == null ? "" : last.toString();
	if(name.isEmpty()) { throw new IllegalArgumentException("name the database with calledBy="); }
	ACSES_Database holder = databases.get(name);
	if(holder != null) { throw new IllegalArgumentException(name + " is in use, by " + holder.folder); }
	return name;
}

/** The name of a database other than {@code except} that holds the folder, or null. */
String holderOf(Path folder, ACSES_Database except) {
	for(var entry : databases.entrySet()) { if(entry.getValue() != except && entry.getValue().folder.equals(folder)) { return entry.getKey(); } }
	return null;
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

/** Makes the folder, refusing a path that is not a folder or a folder holding files, as createDB does. */
static void makeEmptyFolder(Path folder) throws IOException {
	if(Files.exists(folder) && !Files.isDirectory(folder)) { throw new IllegalArgumentException(folder + " is not a folder"); }
	if(Files.isDirectory(folder) && !isEmpty(folder)) { throw new IllegalArgumentException(folder + " holds files"); }
	Files.createDirectories(folder);
}

/** A manager for a new folder, from the manager's jar ACSES writes into it, as createDB makes one. */
static ACS_PersistenceManager newManager(Path folder) throws IOException {
	Path jar = folder.resolve(ACS_PersistenceManager.MANAGER);
	ACSES_PersistenceJar.write(jar, ACSE_PersistenceManagerBasic.class);
	return managerFrom(jar, folder);
}

/** What ACS keeps at a database's root: the pointer, the manager's jar, and the numbered stored versions. */
static boolean isRecognized(Path entry) {
	String name = entry.getFileName().toString();
	return name.equals(ACS_PersistenceManager.POINTER) || name.equals(ACS_PersistenceManager.MANAGER) || (name.matches("\\d{1,9}") && Files.isDirectory(entry));
}

/** Copies a file, or a folder with everything in it; a link is copied as a link. */
static void copyTree(Path from, Path to) throws IOException {
	try(Stream<Path> paths = Files.walk(from)) {
		for(Path path : (Iterable<Path>) paths::iterator) { Files.copy(path, to.resolve(from.relativize(path).toString()), StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS); }
	}
}

static void deleteTree(Path folder) throws IOException {
	try(Stream<Path> paths = Files.walk(folder)) {
		for(Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) { Files.delete(path); }
	}
}

/**
 * The manager built from the folder's jar through its public {@code (Path)} constructor, as
 * {@link ACS_PersistenceManager#open} builds one; open itself needs the root file the first store
 * writes, which a folder being created has not got yet.
 * */
static ACS_PersistenceManager managerFrom(Path jar, Path folder) throws IOException {
	ACS_PersistenceManager.StoredClassLoader loader = new ACS_PersistenceManager.StoredClassLoader(jar.toUri().toURL(), ACS_PersistenceManager.class.getClassLoader());
	String name = ACSE_PersistenceManagerBasic.class.getName();
	try { return (ACS_PersistenceManager) Class.forName(name, true, loader).getConstructor(Path.class).newInstance(folder); }
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
 * A request taken apart: its command, its plain arguments, and its flags, by their shape - {@code toPath=},
 * {@code +force} - a switch standing with an empty value; a later copy of a flag wins. {@code comment=}
 * is taken by every command and left to the log,
 * since no command here hands one to a core method.
 * */
static final class Request {
	final String command;
	final ArrayList<String> plain = new ArrayList<>();
	final HashMap<String, String> flags = new HashMap<>();

	Request(List<String> arguments) {
		if(arguments.isEmpty()) { throw new IllegalArgumentException("no command; the commands: " + COMMANDS); }
		command = arguments.get(0);
		for(int i = 1; i < arguments.size(); i++) {
			String argument = arguments.get(i);
			String valued = valuedFlagOf(argument);
			if(SWITCHES.contains(argument)) { flags.put(argument, ""); }
			else if(valued == null) { plain.add(argument); }
			else {
				if(argument.length() == valued.length()) { throw new IllegalArgumentException(valued + " needs a value"); }
				flags.put(valued, argument.substring(valued.length()));
			}
		}
	}

	static String valuedFlagOf(String argument) {
		for(String flag : VALUED) { if(argument.startsWith(flag)) { return flag; } }
		return null;
	}

	String value(String flag) { return flags.get(flag); }

	boolean has(String flag) { return flags.containsKey(flag); }

	/** Refuses a request whose plain arguments are not {@code count} in number, or that gives a flag the command does not take; the usage lists the ones it takes. */
	void expect(int count, String usage, String... takes) {
		if(plain.size() != count) { throw new IllegalArgumentException("usage: " + usage); }
		for(String flag : flags.keySet()) {
			if(!flag.equals("comment=") && !List.of(takes).contains(flag)) { throw new IllegalArgumentException(command + " takes no " + flag + "; usage: " + usage); }
		}
	}
}

/** An acquisition's key, and whether the reading commands go without it. */
record Acquisition(String key, boolean changesOnly) {}

/** A client a wait holds open, with its request for the log, and the answer once one is ready. */
static final class Held {
	final SocketChannel client;
	final OffsetDateTime time;
	final List<String> arguments;
	ACSES_Request.Answer answer;

	Held(SocketChannel client, OffsetDateTime time, List<String> arguments) {
		this.client = client;
		this.time = time;
		this.arguments = arguments;
	}
}

}
