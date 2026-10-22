package org.halim.setSea;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.halim.acs.ACS_Core;
import org.halim.acs.ACS_Journal;
import org.halim.acs.ACS_PersistenceManager;
import org.halim.acs.arity.ACS_ArityComponentBase;
import org.halim.acs.arity.sets.IdentitiesSet;
import org.halim.acs.structure.binary.connector.ACS_ConnectorComponentBase;
import org.halim.acse.ACSE_CoreBasic;
import org.halim.acse.ACSE_PersistenceManagerBasic;
import org.halim.acse.arity.catalog.memory.StringValueCatalogArrays;
import org.halim.acse.structure.binary.connector.DoubleArrayedConnectorComponent;
import org.halim.acse.structure.unary.TightSetHierarchyDirectedAcyclicIntGraphComponent;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Stream;


/**
 * A Set Sea's tags and files, kept in an ACSE core inside this process. Arity {@link #TAGS} holds the
 * tags, with a {@link #NAME} row, and tag {@link #ROOT}, "File", is the first identity it ever gave; arity
 * {@link #FILES} holds the files, with a {@link #NAME} row and a {@link #DISK_NAME} row. Structure
 * {@link #HIERARCHY} is a tight-set hierarchy over the tags, and {@link #TAGGING} connects a tag, its
 * primary, to a file, its complement. The root is never connected: a file is under it by holding no tag,
 * and every file is in its domain.
 * <p>
 * A loaded sea's core is built from the classes its own folder stored, so its hierarchy is not this
 * program's class and cannot be cast to it: the hierarchy implements no contract the program shares
 * beyond {@code ACS_StructureComponentBase}, so it is reached by reflection, its methods found by name
 * once. The connector is reached through {@link ACS_ConnectorComponentBase}, and everything else through
 * the contracts too.
 * <p>
 * Undo works on edits, not on requests. Every request is a transaction of its own in the journal, so
 * creating a tag is two of them, a capture and a name; an edit notes the journal's index before it and
 * after it, and undo walks the journal back to the first. One lock guards every method, since the
 * autosave stores from another thread and the core is not multi thread safe.
 * */
public class SetSeaDatabase {

public static final int TAGS = 0, FILES = 1;
/** The rows: a name on both arities, then the disk name on files. */
public static final int NAME = 0, DISK_NAME = 1;
public static final int HIERARCHY = 0, TAGGING = 1;
public static final int ROOT = 0;
public static final String ROOT_NAME = "File";
/** How many stored generations a store keeps; the older ones are deleted, since each holds a full copy and the autosave stores every few seconds. */
public static final int KEPT_GENERATIONS = 3;

record Edit(String name, int before, int after) {}

final Path folder;
final ACS_Core core;
final ACS_Journal journal;
final ACS_ArityComponentBase tags, files;
final Hierarchy hierarchy;
final ACS_ConnectorComponentBase tagging;
final ReentrantLock lock = new ReentrantLock();
final ArrayDeque<Edit> done = new ArrayDeque<>(), undone = new ArrayDeque<>();
boolean dirty;


SetSeaDatabase(Path folder, ACS_Core core) throws IOException {
	this.folder = folder;
	this.core = core;
	this.journal = core.getJournal();
	if(core.getArityCount() <= FILES || core.getStructureCount() <= TAGGING) { throw new IOException(folder + " holds no Set Sea: it has " + core.getArityCount() + " arities and " + core.getStructureCount() + " structures"); }
	this.tags = core.getArity(TAGS);
	this.files = core.getArity(FILES);
	this.hierarchy = new Hierarchy(core.getStructure(HIERARCHY));
	if(!(core.getStructure(TAGGING) instanceof ACS_ConnectorComponentBase connector) || connector.getPrimaryArity() != TAGS || connector.getComplementArity() != FILES) {
		throw new IOException(folder + " holds no Set Sea: structure " + TAGGING + " is no connector from the tags to the files");
	}
	this.tagging = connector;
}

/** Whether something was ever stored in {@code folder}. */
public static boolean isStored(Path folder) { return Files.exists(folder.resolve(ACS_PersistenceManager.POINTER)); }

/** A new sea in {@code folder}, holding only the root, stored once. */
public static SetSeaDatabase create(Path folder) throws IOException {
	ACSE_CoreBasic core = new ACSE_CoreBasic();
	ACS_Journal journal = core.getJournal();
	core.addArity("tags", null);
	core.addArity("files", null);
	core.addCatalogRow(TAGS, new StringValueCatalogArrays(), "name", null);
	core.addCatalogRow(FILES, new StringValueCatalogArrays(), "name", null);
	core.addCatalogRow(FILES, new StringValueCatalogArrays(), "disk name", null);
	core.addStructure(new TightSetHierarchyDirectedAcyclicIntGraphComponent(core, journal, TAGS), "tag hierarchy", null);
	core.addStructure(new DoubleArrayedConnectorComponent(core, journal, TAGS, FILES), "tagging", null);
	core.captureIdentity(TAGS, null);
	core.getArity(TAGS).setStringFor(NAME, ROOT, ROOT_NAME, null);
	SetSeaDatabase database = new SetSeaDatabase(folder, core);
	database.store();
	return database;
}

/** The current generation of {@code folder}, built from the classes it stored. Undo history begins here. */
public static SetSeaDatabase load(Path folder) throws IOException {
	return new SetSeaDatabase(folder, ACS_PersistenceManager.open(folder).load());
}


/** Stores a new generation, then deletes the generations older than the last {@link #KEPT_GENERATIONS}. */
public List<String> store() throws IOException {
	lock.lock();
	try {
		ACS_PersistenceManager manager = isStored(folder) ? ACS_PersistenceManager.open(folder) : new ACSE_PersistenceManagerBasic(folder);
		List<String> warnings = manager.store(core);
		prune(Integer.parseInt(manager.current().getFileName().toString()));
		dirty = false;
		return warnings;
	} finally { lock.unlock(); }
}

void prune(int current) throws IOException {
	try(DirectoryStream<Path> entries = Files.newDirectoryStream(folder)) {
		for(Path entry : entries) {
			String name = entry.getFileName().toString();
			if(name.matches("\\d{1,9}") && Files.isDirectory(entry) && Integer.parseInt(name) <= current - KEPT_GENERATIONS) { deleteTree(entry); }
		}
	}
}

static void deleteTree(Path tree) throws IOException {
	try(Stream<Path> walk = Files.walk(tree)) {
		for(Path path : walk.sorted(Comparator.reverseOrder()).toList()) { Files.delete(path); }
	}
}

public boolean isDirty() {
	lock.lock();
	try { return dirty; } finally { lock.unlock(); }
}


/**
 * Runs {@code body} as one undo step. A failure walks the journal back to where the edit began and
 * rethrows. An edit inside an edit is part of the outer one. Any edit that recorded something drops what
 * redo held, as the journal itself does.
 * */
public <T> T edit(String name, Supplier<T> body) {
	lock.lock();
	try {
		if(lock.getHoldCount() > 1) { return body.get(); }
		int before = journal.getCurrentActivityIndex(), pending = journal.getPendingActivityCount();
		try {
			T result = body.get();
			int after = journal.getCurrentActivityIndex();
			if(after != before) {
				done.push(new Edit(name, before, after));
				undone.clear();
				dirty = true;
			}
			return result;
		} catch(RuntimeException | Error e) {
			walkBackTo(before);
			if(journal.getPendingActivityCount() != pending) { undone.clear(); }
			throw e;
		}
	} finally { lock.unlock(); }
}

public void edit(String name, Runnable body) { edit(name, () -> { body.run(); return null; }); }

void walkBackTo(int activity) {
	while(journal.getCurrentActivityIndex() > activity) { journal.undoLastActivity(); }
}

public boolean canUndo() {
	lock.lock();
	try { return !done.isEmpty(); } finally { lock.unlock(); }
}

public boolean canRedo() {
	lock.lock();
	try { return !undone.isEmpty(); } finally { lock.unlock(); }
}

public void undo() {
	lock.lock();
	try {
		if(done.isEmpty()) { return; }
		Edit last = done.pop();
		walkBackTo(last.before());
		undone.push(last);
		dirty = true;
	} finally { lock.unlock(); }
}

public void redo() {
	lock.lock();
	try {
		if(undone.isEmpty()) { return; }
		Edit next = undone.pop();
		while(journal.getCurrentActivityIndex() < next.after()) { journal.redoLastActivity(); }
		done.push(next);
		dirty = true;
	} finally { lock.unlock(); }
}


// --- Tags ---

public int createTag(String name) {
	return edit("create tag " + name, () -> {
		int tag = (int) core.captureIdentity(TAGS, null);
		tags.setStringFor(NAME, tag, name, null);
		return tag;
	});
}

/** Its edges and its files' connections go with it; its children fall back to the root when it was their only parent. */
public void removeTag(int tag) {
	if(tag == ROOT) { throw new IllegalArgumentException("the root tag \"" + ROOT_NAME + "\" cannot be deleted"); }
	edit("delete tag " + tag, () -> core.delegateRemoval(TAGS, tag, null));
}

public void renameTag(int tag, String name) { edit("rename tag " + tag, () -> tags.setStringFor(NAME, tag, name, null)); }

/** Refuses an edge the hierarchy already holds or implies, and one closing a cycle. */
public void addParent(int tag, int parent) {
	edit("add parent " + parent + " to " + tag, () -> {
		if(!hierarchy.addParent(tag, parent)) { throw new IllegalArgumentException(tagName(tag) + " already has " + tagName(parent) + " as a parent or an ancestor"); }
	});
}

/** Nothing happens when there was no such edge. */
public void removeParent(int tag, int parent) { edit("remove parent " + parent + " of " + tag, () -> { hierarchy.removeParent(tag, parent); }); }

public String tagName(int tag) {
	lock.lock();
	try { return tags.getStringFor(NAME, tag); } finally { lock.unlock(); }
}

public boolean isTag(int tag) {
	lock.lock();
	try { return tag >= 0 && tags.occupied().contains(tag); } finally { lock.unlock(); }
}

public int[] tagIdentities() {
	lock.lock();
	try { return identities(tags.occupied()); } finally { lock.unlock(); }
}

public int lastTag() {
	lock.lock();
	try { return last(tags.occupied()); } finally { lock.unlock(); }
}

/** None for the root; the root alone for a tag listing no parent. */
public int[] parentsOf(int tag) {
	lock.lock();
	try { return hierarchy.parents(tag); } finally { lock.unlock(); }
}

public int[] childrenOf(int tag) {
	lock.lock();
	try { return hierarchy.children(tag); } finally { lock.unlock(); }
}

/** Whether a path of edges leads from {@code from} down to {@code to}; true when they are the same tag. */
public boolean reaches(int from, int to) {
	lock.lock();
	try { return hierarchy.reaches(from, to); } finally { lock.unlock(); }
}

/** Every tag above {@code tag}, the root included, in the order a walk up first meets them. */
public int[] ancestorsOf(int tag) {
	lock.lock();
	try { return walk(tag, true); } finally { lock.unlock(); }
}

/** Every tag below {@code tag}, in the order a walk down first meets them. */
public int[] descendantsOf(int tag) {
	lock.lock();
	try { return walk(tag, false); } finally { lock.unlock(); }
}

int[] walk(int tag, boolean up) {
	IntArrayList found = new IntArrayList(), stack = new IntArrayList();
	IntOpenHashSet seen = new IntOpenHashSet();
	stack.add(tag);
	while(!stack.isEmpty()) {
		for(int next : up ? hierarchy.parents(stack.popInt()) : hierarchy.children(stack.popInt())) {
			if(next != tag && seen.add(next)) {
				found.add(next);
				stack.add(next);
			}
		}
	}
	return found.toIntArray();
}


// --- Files ---

public int createFile(String name) {
	return edit("create file " + name, () -> {
		int file = (int) core.captureIdentity(FILES, null);
		files.setStringFor(NAME, file, name, null);
		return file;
	});
}

public void setDiskName(int file, String diskName) { edit("disk name of " + file, () -> files.setStringFor(DISK_NAME, file, diskName, null)); }

public void renameFile(int file, String name) { edit("rename file " + file, () -> files.setStringFor(NAME, file, name, null)); }

/** Its connections go with it. Its file on disk is the caller's. */
public void removeFile(int file) { edit("delete file " + file, () -> core.delegateRemoval(FILES, file, null)); }

public String fileName(int file) {
	lock.lock();
	try { return files.getStringFor(NAME, file); } finally { lock.unlock(); }
}

/** "" for a file never given one. */
public String diskName(int file) {
	lock.lock();
	try { return files.getStringFor(DISK_NAME, file); } finally { lock.unlock(); }
}

public boolean isFile(int file) {
	lock.lock();
	try { return file >= 0 && files.occupied().contains(file); } finally { lock.unlock(); }
}

public int[] fileIdentities() {
	lock.lock();
	try { return identities(files.occupied()); } finally { lock.unlock(); }
}

public int lastFile() {
	lock.lock();
	try { return last(files.occupied()); } finally { lock.unlock(); }
}

/** Tagging with the root does nothing: every file is under it already. */
public void tag(int tag, int file) {
	if(tag == ROOT) { return; }
	edit("tag " + file + " with " + tag, () -> { tagging.ensureConnection(tag, file, null); });
}

public void untag(int tag, int file) {
	if(tag == ROOT) { return; }
	edit("untag " + file + " from " + tag, () -> { tagging.breakConnection(tag, file, null); });
}

/** The tags the file holds; none for a file under the root only. */
public int[] tagsOf(int file) {
	lock.lock();
	try {
		int[] held = new int[tagging.primaryCount(file)];
		tagging.copyPrimariesInto(file, held, 0);
		return held;
	} finally { lock.unlock(); }
}

/** The files holding the tag itself; for the root, the files holding no tag. */
public int[] directFilesOf(int tag) {
	lock.lock();
	try {
		if(tag == ROOT) {
			IntArrayList untagged = new IntArrayList();
			for(int file : identities(files.occupied())) { if(tagging.primaryCount(file) == 0) { untagged.add(file); } }
			return untagged.toIntArray();
		}
		int[] held = new int[tagging.complementCount(tag)];
		tagging.copyComplementsInto(tag, held, 0);
		return held;
	} finally { lock.unlock(); }
}

/** The files holding the tag or a tag below it, each once; for the root, every file. */
public int[] domainFilesOf(int tag) {
	lock.lock();
	try {
		if(tag == ROOT) { return identities(files.occupied()); }
		IntOpenHashSet seen = new IntOpenHashSet();
		IntArrayList found = new IntArrayList();
		for(int file : directFilesOf(tag)) { if(seen.add(file)) { found.add(file); } }
		for(int below : walk(tag, false)) { for(int file : directFilesOf(below)) { if(seen.add(file)) { found.add(file); } } }
		return found.toIntArray();
	} finally { lock.unlock(); }
}

/** Whether the file holds the tag itself; for the root, whether it holds none. */
public boolean holds(int tag, int file) {
	lock.lock();
	try { return tag == ROOT ? tagging.primaryCount(file) == 0 : tagging.isConnected(tag, file); } finally { lock.unlock(); }
}


static int[] identities(IdentitiesSet set) {
	int[] result = new int[Math.toIntExact(set.size())];
	for(int i = 0; i < result.length; i++) { result[i] = Math.toIntExact(set.identityAt(i)); }
	return result;
}

static int last(IdentitiesSet set) { return set.isEmpty() ? -1 : Math.toIntExact(set.identityAt(set.size() - 1)); }


/**
 * The tight-set hierarchy through its methods' names, since a loaded one is a class of its folder's
 * jar. A refusal it throws comes out unwrapped.
 * */
static final class Hierarchy {

	final Object graph;
	final Method addParent, removeParent, parentCount, childCount, copyParentsInto, copyChildrenInto, reaches;

	Hierarchy(Object graph) throws IOException {
		this.graph = graph;
		Class<?> type = graph.getClass();
		addParent = method(type, "addParent", int.class, int.class, String.class);
		removeParent = method(type, "removeParent", int.class, int.class, String.class);
		parentCount = method(type, "parentCount", int.class);
		childCount = method(type, "childCount", int.class);
		copyParentsInto = method(type, "copyParentsInto", int.class, int[].class, int.class);
		copyChildrenInto = method(type, "copyChildrenInto", int.class, int[].class, int.class);
		reaches = method(type, "reaches", int.class, int.class);
	}

	static Method method(Class<?> type, String name, Class<?>... parameters) throws IOException {
		try { return type.getMethod(name, parameters); }
		catch(NoSuchMethodException e) { throw new IOException(type.getName() + " has no public " + name + ", so it is no Set Sea hierarchy", e); }
	}

	boolean addParent(int tag, int parent) { return (boolean) call(addParent, tag, parent, null); }
	boolean removeParent(int tag, int parent) { return (boolean) call(removeParent, tag, parent, null); }
	boolean reaches(int from, int to) { return (boolean) call(reaches, from, to); }

	int[] parents(int tag) {
		int[] held = new int[(int) call(parentCount, tag)];
		call(copyParentsInto, tag, held, 0);
		return held;
	}

	int[] children(int tag) {
		int[] held = new int[(int) call(childCount, tag)];
		call(copyChildrenInto, tag, held, 0);
		return held;
	}

	Object call(Method method, Object... arguments) {
		try { return method.invoke(graph, arguments); }
		catch(IllegalAccessException e) { throw new IllegalStateException("cannot call " + method, e); }
		catch(InvocationTargetException e) {
			if(e.getCause() instanceof RuntimeException runtime) { throw runtime; }
			if(e.getCause() instanceof Error error) { throw error; }
			throw new IllegalStateException(method.getName() + " failed", e.getCause());
		}
	}

}

}
