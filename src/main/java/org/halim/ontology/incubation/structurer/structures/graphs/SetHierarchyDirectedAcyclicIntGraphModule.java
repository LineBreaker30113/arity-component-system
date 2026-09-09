package org.halim.ontology.incubation.structurer.structures.graphs;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.halim.ontology.incubation.structurer.ArityModule;

import java.util.ArrayList;


/**
 * Optimized for the case where the leafs are more than branch nodes,
 * and parent*child count mostly does not exceed a constant.
 * <p>
 * That premise is the whole design. A leaf with one parent is four bytes and nothing else - no list,
 * no object, no slot - because its entire edge set fits in the int that would have held a pointer to
 * one. Only an element that outgrows that shape, by taking a second parent or any child at all, is
 * given a {@link #multiData} entry; and it is given back when it shrinks into the shape again, so a
 * graph that churns at its fringe does not accumulate slots it stopped using.
 * <p>
 * <b>The encoding.</b> {@link #hierarchy} is one int per element. A positive {@code h} is the single
 * parent {@code h}, a negative {@code h} is the multi entry at slot {@code -h}, and zero is the
 * element sitting directly under {@link #ROOT} - which is element 0, and is the one element that has
 * no parent at all. Zero therefore needs no sentinel and no bias: a catalog grows by zero-filling, so
 * a freshly minted identity is already under the root, and that is both the cheapest thing to store
 * and the right thing to mean.
 * <p>
 * <b>Root parentage is the absence of a listed one.</b> An element is under the root exactly when it
 * lists no parent - {@code h == 0}, or a multi entry whose parent count is zero. The root is never
 * written into a parent list, so it never has to be removed from one either: giving an element a real
 * parent moves it off the root, and removing its last one moves it back, with no bookkeeping in
 * between.
 * <p>
 * <b>The root's children live in slot 0</b>, which is free because a slot is only ever reached as
 * {@code -h} and nothing encodes as {@code -0}. It is a normal entry, {@code [0, children...]}, so
 * every accessor reads the root the same way it reads anything else. What is special is how it is
 * maintained: elements are pushed onto it as they become rootless and never searched for as they stop
 * being, so between mutations it is a superset - it may hold elements that have since taken a parent,
 * or hold one twice. A read rebuilds it first when it is dirty, filtering to what is genuinely under
 * the root and occupied. Searching the list on every attachment would be the obvious alternative and
 * is the quadratic one: building a hierarchy attaches every element in turn, and each search would
 * walk what is left of a list that started out holding everything.
 * <p>
 * <b>The multi entry</b> is {@code [parentCount, parents..., children...]} - the count is first
 * because it is the only thing that says where the parents stop and the children start, and children
 * are last because appending a child is then a push rather than an insert. Adding a parent is the
 * insert, which is the right way round: by the premise above, a second parent is rarer than a child.
 * <p>
 * <b>Acyclicity is enforced on the edge, not audited afterwards.</b> An edge is refused when the
 * parent is already below the element, which costs a walk of the element's descendants - bounded, by
 * the premise, and the only moment a cycle can be created. The alternative is to accept edges and
 * detect cycles later, and a cycle discovered later has no honest repair: nothing in the graph records
 * which edge was the wrong one.
 * <p>
 * Identities here are ints, not the longs the catalogs address. A graph edge costs an int on both
 * ends, so widening it would double the structure to reach a size this shape is not the right one for
 * anyway. Not multi thread safe.
 * */
public class SetHierarchyDirectedAcyclicIntGraphModule {

/**
 * The root, which is element 0 and has no parent. Everything that lists no parent hangs off it, so
 * the zero a grown slot already holds is the correct edge rather than a sentinel standing in for one.
 * */
public static final int ROOT = 0;

/** The root's entry. Nothing encodes as {@code -0}, so no other element can want this slot. */
static final int ROOT_SLOT = 0;

/** Module, defines the element count and the tombstones. */
ArityModule arity;

/**
 * Every element points to it's parent, unless they have children or multiple parents,
 * then they would point to negative of their slot, slot 0 is not valid for any of them.
 * */
IntArrayList hierarchy;
/** In case of multiple edges: first integer is the parent count, following are parents followed by children. */
ArrayList<IntArrayList> multiData;
/** Slots given back by elements that shrank into the inline shape, to be handed out again. */
IntArrayList freeSlots;
/** Whether slot 0 has been pushed to since it was last rebuilt, so a read knows to filter it. */
boolean rootChildrenDirty;

/**
 * Declared, and not yet consulted by anything here. It is meant to say whether the root counts as an
 * element in its own right or is only the place rootless elements hang from - a distinction that
 * belongs to whatever enumerates or closes over the graph rather than to the storage, which is why
 * flipping it changes nothing at this level.
 * If the Root is nothing, then it is an easy list of root elements.
 * */
public boolean isRootExcluded;

/**
 * Whether the parent set is kept to its covering relation - the transitive reduction - rather than to
 * whatever was asked for. Under it a parent already implied by a nearer one is not added, and a parent
 * that a newly added one supersedes is dropped, so no edge in the structure is derivable from the
 * others.
 * <p>
 * A new edge can also make an edge <em>below</em> it redundant, so every successful
 * {@link #addParent} under this flag ends in {@link #tighten} over the element's descendants. That is
 * a walk of the whole cone per attachment, which is the wrong price to pay a million times while
 * loading: clear the flag, load, and call {@code tighten(ROOT)} once at the end. The result is the
 * same relation - reduction does not depend on the order edges arrived in.
 * */
public boolean isTight;


public SetHierarchyDirectedAcyclicIntGraphModule(ArityModule arity) {
	this.arity = arity;
	this.hierarchy = new IntArrayList();
	this.multiData = new ArrayList<>();
	this.multiData.add(new IntArrayList());
	this.multiData.get(ROOT_SLOT).add(0);
	this.freeSlots = new IntArrayList();
}


/**
 * Makes {@code identity} addressable, growing the hierarchy if it is not already. True when it had to
 * grow. A new element reads as under the root without being written, so the only cost of minting one
 * is pushing it onto the root's list.
 * */
public boolean ensureCapacity(long identity) {
	int want = Math.toIntExact(identity + 1);
	if(hierarchy.size() >= want) { return false; }
	IntArrayList rootChildren = multiData.get(ROOT_SLOT);
	for(int i = Math.max(hierarchy.size(), 1); i < want; i++) { rootChildren.add(i); }
	hierarchy.size(want);
	rootChildrenDirty = true;
	return true;
}

/** How many identities the arity holds. The graph is defined over them and does not count separately. */
public long elementCount() {
	return arity.occupied().size();
}

/** Whether the element hangs directly off the root, which is to say it lists no parent of its own. */
public boolean isUnderRoot(int element) {
	if(element == ROOT) { return false; }
	int h = hierarchy.getInt(element);
	return h == 0 || (h < 0 && multiData.get(-h).getInt(0) == 0);
}


public int parentCount(int element) {
	if(element == ROOT) { return 0; }
	int h = hierarchy.getInt(element);
	if(h < 0) {
		int count = multiData.get(-h).getInt(0);
		return count == 0 ? 1 : count;
	}
	return 1;
}

public int childCount(int element) {
	if(element == ROOT) { refreshRootChildren(); }
	int h = element == ROOT ? -ROOT_SLOT : hierarchy.getInt(element);
	if(element != ROOT && h >= 0) { return 0; }
	IntArrayList entry = multiData.get(element == ROOT ? ROOT_SLOT : -h);
	return entry.size() - 1 - entry.getInt(0);
}

public int parentAt(int element, int at) {
	if(element != ROOT && at == 0) {
		int h = hierarchy.getInt(element);
		if(h > 0) { return h; }
		if(h == 0) { return ROOT; }
		IntArrayList entry = multiData.get(-h);
		return entry.getInt(0) == 0 ? ROOT : entry.getInt(1);
	}
	if(element != ROOT && at > 0) {
		int h = hierarchy.getInt(element);
		if(h < 0 && at < multiData.get(-h).getInt(0)) { return multiData.get(-h).getInt(1 + at); }
	}
	throw new IndexOutOfBoundsException("parent " + at + " of " + element);
}

public int childAt(int element, int at) {
	if(element == ROOT) { refreshRootChildren(); }
	int slot = slotOf(element);
	if(slot > 0 || element == ROOT) {
		IntArrayList entry = multiData.get(slot);
		int count = entry.getInt(0);
		if(at >= 0 && 1 + count + at < entry.size()) { return entry.getInt(1 + count + at); }
	}
	throw new IndexOutOfBoundsException("child " + at + " of " + element);
}

/**
 * The edges of one element copied out in one call. Both runs are contiguous inside the entry, so a
 * caller walking a branch node's children copies them and then loops on a plain array - which is the
 * same bargain the catalogs make, for the same reason.
 * */
public void copyChildrenInto(int element, int[] dest, int offset) {
	if(element == ROOT) { refreshRootChildren(); }
	int slot = slotOf(element);
	if(slot <= 0 && element != ROOT) { return; }
	IntArrayList entry = multiData.get(slot);
	for(int i = 1 + entry.getInt(0); i < entry.size(); i++) { dest[offset++] = entry.getInt(i); }
}

public void copyParentsInto(int element, int[] dest, int offset) {
	for(int i = 0, count = parentCount(element); i < count; i++) { dest[offset + i] = parentAt(element, i); }
}


public boolean hasParent(int element, int parent) {
	if(element == ROOT) { return false; }
	if(parent == ROOT) { return isUnderRoot(element); }
	int h = hierarchy.getInt(element);
	if(h < 0) {
		IntArrayList entry = multiData.get(-h);
		return indexOf(entry, 1, 1 + entry.getInt(0), parent) >= 0;
	}
	return h == parent;
}

public boolean hasChild(int element, int child) { return hasParent(child, element); }

/**
 * Whether {@code to} is reachable from {@code from} by following children. Walks descendants with a
 * seen set rather than plain recursion: a directed acyclic graph can hold exponentially many distinct
 * paths between two nodes, and a walk that does not remember where it has been will take them all.
 * <p>
 * From the root the answer needs no walk. Every chain of parents is finite and acyclic, so it ends at
 * an element listing none - which is a child of the root - and every element is therefore below it.
 * */
public boolean reaches(int from, int to) {
	if(from == to) { return true; }
	if(from == ROOT) { return true; }
	if(to == ROOT) { return false; }
	IntArrayList stack = new IntArrayList();
	IntOpenHashSet seen = new IntOpenHashSet();
	stack.add(from);
	seen.add(from);
	while(!stack.isEmpty()) {
		int at = stack.popInt();
		int h = hierarchy.getInt(at);
		if(h >= 0) { continue; }
		IntArrayList entry = multiData.get(-h);
		for(int i = 1 + entry.getInt(0); i < entry.size(); i++) {
			int next = entry.getInt(i);
			if(next == to) { return true; }
			if(seen.add(next)) { stack.add(next); }
		}
	}
	return false;
}


/**
 * Gives {@code element} a parent. False when it already had it, and a refusal when the edge would
 * close a cycle - the graph is acyclic by construction rather than by convention, so this is the one
 * place that has to pay for it.
 * <p>
 * Under {@link #isTight} this also keeps the parent set reduced: false when the new parent is already
 * an ancestor of one the element has, since that edge would say nothing the existing one does not, and
 * any parent the new one is below is dropped, since it has stopped being the nearest. The element's
 * descendants are tightened afterwards, because the new edge reaches them too.
 * <p>
 * The root is not addable. An element is under it by listing nothing, so one already there needs no
 * edge, and one with a real parent cannot take the root as a second - the root is where an element
 * falls back to, not something it can be beside.
 * */
public boolean addParent(int element, int parent) {
	requireLive(element);
	requireLive(parent);
	if(element == ROOT) { throw new IllegalArgumentException("the root has no parent"); }
	if(element == parent) { throw new IllegalArgumentException("an element cannot be its own parent: " + element); }
	if(hasParent(element, parent)) { return false; }
	if(parent == ROOT) {
		throw new IllegalArgumentException(element + " has a parent, so it is not under the root; remove that parent instead");
	}
	if(reaches(element, parent)) { throw new IllegalArgumentException("parent " + parent + " of " + element + " closes a cycle"); }
	if(isTight) {
		// Nothing is removed until the whole parent set has been checked - a new parent that turns out
		// to be redundant must leave the set exactly as it was.
		for(int i = parentCount(element) - 1; i >= 0; i--) {
			int held = parentAt(element, i);
			if(held != ROOT && reaches(parent, held)) { return false; }
		}
		for(int i = parentCount(element) - 1; i >= 0; i--) {
			int held = parentAt(element, i);
			if(held != ROOT && reaches(held, parent)) { removeParent(element, held); }
		}
	}
	// The child is a push onto the tail; the parent is an insert at the parent run's end. That
	// asymmetry is the layout paying off for the case the class is built around.
	multiData.get(promote(parent)).add(element);
	int h = hierarchy.getInt(element);
	// An element that was under the root takes its first real parent inline, without a slot at all -
	// which is the common case, and the one the whole encoding is arranged around.
	if(h == 0) {
		hierarchy.set(element, parent);
		rootChildrenDirty = true;
	}
	else {
		IntArrayList entry = multiData.get(promote(element));
		int count = entry.getInt(0);
		if(count == 0) { rootChildrenDirty = true; }
		entry.add(1 + count, parent);
		entry.set(0, count + 1);
	}
	if(isTight) { tighten(element); }
	return true;
}

/** The same edge from the other end. Nothing is stored differently, so this is the call reversed. */
public boolean addChild(int element, int child) { return addParent(child, element); }

/**
 * Removes a parent, and gives back either end's slot if the edge was the last one holding it. An
 * element that loses its last parent falls back under the root, which is the same thing as listing
 * none.
 * */
public boolean removeParent(int element, int parent) {
	if(element == ROOT || parent == ROOT) { return false; }
	int ph = hierarchy.getInt(parent);
	if(ph >= 0) { return false; }
	IntArrayList parentEntry = multiData.get(-ph);
	int at = indexOf(parentEntry, 1 + parentEntry.getInt(0), parentEntry.size(), element);
	if(at < 0) { return false; }
	parentEntry.removeInt(at);
	int h = hierarchy.getInt(element);
	if(h >= 0) { hierarchy.set(element, 0); }
	else {
		IntArrayList entry = multiData.get(-h);
		int count = entry.getInt(0);
		int held = indexOf(entry, 1, 1 + count, parent);
		if(held >= 0) {
			entry.removeInt(held);
			entry.set(0, count - 1);
		}
	}
	demoteIfInline(parent);
	demoteIfInline(element);
	if(isUnderRoot(element)) {
		multiData.get(ROOT_SLOT).add(element);
		rootChildrenDirty = true;
	}
	return true;
}

public boolean removeChild(int element, int child) { return removeParent(child, element); }

/**
 * Drops every edge touching {@code element}, which is what an identity being unsigned has to mean
 * here. A tombstone that kept its edges would leave the elements around it pointing at something the
 * occupied set says is not there, and no read of this structure would catch it.
 * */
public void detach(int element) {
	if(element == ROOT) { return; }
	while(!isUnderRoot(element)) { removeParent(element, parentAt(element, 0)); }
	while(childCount(element) > 0) { removeParent(childAt(element, 0), element); }
}


/**
 * Reduces the parent sets of everything below {@code element} to the covering relation, dropping every
 * edge another path already implies. Answers how many were dropped. The element itself is left alone -
 * whoever called it has just settled that part, and the root, which is the argument for tightening
 * everything, has no parents to settle.
 * <p>
 * This is what a new edge propagates. Giving {@code B} the parent {@code A} does not only concern
 * {@code B}: anything under {@code B} that also held {@code A} directly now reaches {@code A} the long
 * way round, so its direct edge has become redundant. The elements that can be affected are exactly
 * the descendants of {@code B} - a new edge only adds reachability into that cone, and a parent set
 * can only go redundant when one of its parents starts reaching another - which is why tightening the
 * cone is enough and a rescan of the graph is not needed.
 * <p>
 * One pass suffices, and the order within it does not matter. An edge is only ever removed when a path
 * already implies it, so removing it leaves the reachability relation exactly as it was - the reason a
 * transitive reduction can be taken apart edge by edge without the ground moving underneath.
 * */
public int tighten(int element) {
	IntArrayList descendants = new IntArrayList();
	collectDescendants(element, descendants);
	int removed = 0;
	for(int i = 0; i < descendants.size(); i++) { removed += reduceParents(descendants.getInt(i)); }
	return removed;
}

/** Drops the parents of {@code element} that another of its parents already reaches. */
int reduceParents(int element) {
	int count = parentCount(element);
	if(count < 2) { return 0; }
	int[] held = new int[count];
	copyParentsInto(element, held, 0);
	int removed = 0;
	for(int i = 0; i < held.length; i++) {
		if(held[i] == ROOT) { continue; }
		for(int j = 0; j < held.length; j++) {
			if(i == j || held[j] == ROOT) { continue; }
			if(reaches(held[i], held[j])) {
				if(removeParent(element, held[i])) { removed++; }
				held[i] = ROOT;
				break;
			}
		}
	}
	return removed;
}

/**
 * Every element below {@code element}, collected before anything is changed. Reducing a parent set
 * edits the entries of the parents it drops, which are elements this walk may not have reached yet, so
 * the walk finishes first rather than reading a list that is being written.
 * */
void collectDescendants(int element, IntArrayList into) {
	IntOpenHashSet seen = new IntOpenHashSet();
	IntArrayList stack = new IntArrayList();
	stack.add(element);
	while(!stack.isEmpty()) {
		int at = stack.popInt();
		for(int i = 0, count = childCount(at); i < count; i++) {
			int child = childAt(at, i);
			if(seen.add(child)) {
				into.add(child);
				stack.add(child);
			}
		}
	}
}

/**
 * Rebuilds slot 0 into exactly the root's children. Elements are pushed on as they become rootless and
 * left there when they stop being, so this is where the list is filtered - to what still lists no
 * parent, is still occupied, and has not been pushed twice. Doing it here rather than on every
 * departure is what keeps attaching an element constant time.
 * */
void refreshRootChildren() {
	if(!rootChildrenDirty) { return; }
	IntArrayList entry = multiData.get(ROOT_SLOT);
	IntOpenHashSet seen = new IntOpenHashSet();
	int keep = 1;
	for(int i = 1; i < entry.size(); i++) {
		int candidate = entry.getInt(i);
		if(isUnderRoot(candidate) && arity.occupied().contains(candidate) && seen.add(candidate)) {
			entry.set(keep++, candidate);
		}
	}
	entry.size(keep);
	rootChildrenDirty = false;
}

/** Where the element's entry sits, or a non-positive number when it has none. */
int slotOf(int element) {
	if(element == ROOT) { return ROOT_SLOT; }
	return -hierarchy.getInt(element);
}

/** Gives the element a multi entry, keeping whatever single parent it had, and answers its slot. */
int promote(int element) {
	if(element == ROOT) { return ROOT_SLOT; }
	int h = hierarchy.getInt(element);
	if(h < 0) { return -h; }
	int slot = takeSlot();
	IntArrayList entry = multiData.get(slot);
	entry.clear();
	if(h == 0) { entry.add(0); }
	else {
		entry.add(1);
		entry.add(h);
	}
	hierarchy.set(element, -slot);
	return slot;
}

/**
 * Puts the element back in the inline shape when it fits in it again - at most one parent and no
 * children - and returns the slot to the free list. Without this the structure would only ever grow:
 * an element that briefly held a child would keep a list for the rest of the program.
 * */
void demoteIfInline(int element) {
	if(element == ROOT) { return; }
	int h = hierarchy.getInt(element);
	if(h >= 0) { return; }
	int slot = -h;
	IntArrayList entry = multiData.get(slot);
	int count = entry.getInt(0);
	if(count > 1 || entry.size() - 1 - count > 0) { return; }
	hierarchy.set(element, count == 0 ? 0 : entry.getInt(1));
	entry.clear();
	freeSlots.add(slot);
}

int takeSlot() {
	if(!freeSlots.isEmpty()) { return freeSlots.popInt(); }
	multiData.add(new IntArrayList());
	return multiData.size() - 1;
}

static int indexOf(IntArrayList entry, int from, int to, int value) {
	for(int i = from; i < to; i++) {
		if(entry.getInt(i) == value) { return i; }
	}
	return -1;
}

void requireLive(int element) {
	if(element < 0 || element >= hierarchy.size()) {
		throw new IndexOutOfBoundsException(element + " is not addressable in this graph");
	}
	if(!arity.occupied().contains(element)) {
		throw new IllegalArgumentException(element + " is not an occupied identity");
	}
}


}
