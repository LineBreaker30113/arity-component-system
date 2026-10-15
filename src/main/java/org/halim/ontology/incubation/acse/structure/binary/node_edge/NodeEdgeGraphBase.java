package org.halim.ontology.incubation.acse.structure.binary.node_edge;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.halim.ontology.incubation.acse.ACSE_Core;
import org.halim.ontology.incubation.acse.ACSE_Journal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


/**
 * What every stored form of the node-edge graph shares, so that each rule is written once and holds on
 * every storage: the rules themselves, checked when an edge is made; the edge → nodes side, two ints an
 * edge, which every form keeps because it costs eight bytes an edge and turns every question about an edge
 * into a lookup; and the core's part - capturing an edge, removing one, forgetting a node - with the
 * journal steps. A subclass is a storage: it keeps only the node → edges side, through the hooks below,
 * the way a chunked catalog keeps only its array hooks over {@code ChunkedCatalogBase}.
 * <p>
 * An edge is recorded as one step, {@link #LINK}, holding the edge and its two nodes, and the hooks are
 * called below every check: the request that recorded the step already passed them, so undo and redo
 * replay it as it is. The acyclic rule walks the graph before an edge is made - along edges going out on a
 * directed graph, along every edge on a directionless one - so it costs what the storage's node side costs
 * to walk. Not multi thread safe.
 * */
public abstract class NodeEdgeGraphBase implements ACS_NodeEdgeComponentBase {

/** The edge (edge, first node, second node); the negative triple removes it. */
static final byte LINK = 1;

final ACSE_Core core;
final ACSE_Journal journal;
final int nodeArity, edgeArity;
final boolean directed, acyclic, allowsLoops, allowsParallelEdges;
final int[] params = new int[3];

/** Two ints an edge, its first node then its second; {@code -1, -1} where there is none. */
final IntArrayList ends = new IntArrayList();


protected NodeEdgeGraphBase(ACSE_Core core, ACSE_Journal journal, int nodeArity, int edgeArity, boolean directed, boolean acyclic, boolean allowsLoops, boolean allowsParallelEdges) {
	if(nodeArity == edgeArity) { throw new IllegalArgumentException("nodes and edges in one arity " + nodeArity); }
	if(acyclic && allowsLoops) { throw new IllegalArgumentException("an acyclic graph has no loops: a loop is a cycle"); }
	this.core = core;
	this.journal = journal;
	this.nodeArity = nodeArity;
	this.edgeArity = edgeArity;
	this.directed = directed;
	this.acyclic = acyclic;
	this.allowsLoops = allowsLoops;
	this.allowsParallelEdges = allowsParallelEdges;
}

NodeEdgeGraphBase(ACSE_Core core, ACSE_Journal journal, Rules rules) {
	this(core, journal, rules.nodeArity, rules.edgeArity, rules.directed, rules.acyclic, rules.allowsLoops, rules.allowsParallelEdges);
}


/** Puts the edge on its nodes in the storage; its ends are already in place. */
abstract void index(int edge, int first, int second);

/** Takes the edge off its nodes in the storage; its ends are still in place. */
abstract void unindex(int edge, int first, int second);

/** The node's edges, in any order, as a list of the caller's own. */
abstract IntArrayList edgesOf(int node);

/** Refuses, before anything is recorded, an edge this storage cannot hold. */
void checkIndexable(int first, int second) { }

/** Builds the node side from the ends after a load; the default puts each edge on its nodes. */
void rebuildIndex() throws IOException {
	for(int at = 0; at < ends.size(); at += 2) { if(ends.getInt(at) >= 0) { index(at / 2, ends.getInt(at), ends.getInt(at + 1)); } }
}

/** The storage's own settings, written after the rules, which its loadFrom reads back. */
void writeSettings(DataOutputStream dos) throws IOException { }


@Override
public int getArityIdentity(int index) {
	return switch(index) {
		case 0 -> nodeArity;
		case 1 -> edgeArity;
		default -> throw new IndexOutOfBoundsException("arity " + index + " of a graph over two");
	};
}

@Override
public boolean isDirected() { return directed; }

@Override
public boolean isAcyclic() { return acyclic; }

@Override
public boolean allowsLoops() { return allowsLoops; }

@Override
public boolean allowsParallelEdges() { return allowsParallelEdges; }


@Override
public int firstNodeOf(int edge) { return edge >= 0 && 2L * edge < ends.size() ? ends.getInt(2 * edge) : -1; }

@Override
public int secondNodeOf(int edge) { return edge >= 0 && 2L * edge < ends.size() ? ends.getInt(2 * edge + 1) : -1; }

/** The two nodes in the order an edge keeps them: as given on a directed graph, the smaller first on a directionless one. */
int firstOf(int node, int otherNode) { return directed ? node : Math.min(node, otherNode); }
int secondOf(int node, int otherNode) { return directed ? otherNode : Math.max(node, otherNode); }

/** A search of the first node's own edges. */
@Override
public int edgeOf(int node, int otherNode) {
	if(node < 0 || otherNode < 0) { return -1; }
	int first = firstOf(node, otherNode), second = secondOf(node, otherNode);
	IntArrayList held = edgesOf(first);
	int found = -1;
	for(int i = 0; i < held.size(); i++) {
		int edge = held.getInt(i);
		if(firstNodeOf(edge) == first && secondNodeOf(edge) == second && (found < 0 || edge < found)) { found = edge; }
	}
	return found;
}

@Override
public int edgeCount(int node) { return edgesOf(node).size(); }

@Override
public void copyEdgesInto(int node, int[] dest, int offset) {
	IntArrayList held = edgesOf(node);
	IntArrays.quickSort(held.elements(), 0, held.size());
	held.getElements(0, dest, offset, held.size());
}

/**
 * Whether a path leads from {@code start} to {@code target}: along edges going out on a directed graph,
 * along every edge on a directionless one. A walk with a seen set, since a graph can hold many paths
 * between two nodes and a walk that forgets where it has been takes them all.
 * */
public boolean reaches(int start, int target) {
	if(start == target) { return true; }
	IntArrayList stack = new IntArrayList();
	IntOpenHashSet seen = new IntOpenHashSet();
	stack.add(start);
	seen.add(start);
	while(!stack.isEmpty()) {
		int node = stack.popInt();
		IntArrayList held = edgesOf(node);
		for(int i = 0; i < held.size(); i++) {
			int edge = held.getInt(i), first = firstNodeOf(edge);
			if(directed && first != node) { continue; }
			int other = first == node ? secondNodeOf(edge) : first;
			if(other == target) { return true; }
			if(seen.add(other)) { stack.add(other); }
		}
	}
	return false;
}


/** Every rule is checked before anything is recorded; the edge is the edge arity's lowest free identity, captured nested in this request. */
@Override
public int ensureConnection(int node, int otherNode, String comment) {
	requireLive(node);
	requireLive(otherNode);
	if(node == otherNode && !allowsLoops) { throw new IllegalArgumentException("no loops: " + node + " to itself"); }
	int first = firstOf(node, otherNode), second = secondOf(node, otherNode);
	if(!allowsParallelEdges) {
		int held = edgeOf(first, second);
		if(held >= 0) { return held; }
	}
	if(acyclic && reaches(second, first)) { throw new IllegalArgumentException("an edge from " + first + " to " + second + " closes a cycle"); }
	checkIndexable(first, second);
	int request = journal.beginRecordingActivity(this, "ensureConnection", params, comment);
	int edge;
	try {
		edge = Math.toIntExact(core.captureIdentity(edgeArity, null));
		link(edge, first, second);
	} catch(RuntimeException | Error e) {
		abort(request);
		throw e;
	}
	complete(request);
	return edge;
}

/** Every such edge, removed through the core, which forgets each here. */
@Override
public boolean breakConnection(int node, int otherNode, String comment) {
	if(node < 0 || otherNode < 0) { return false; }
	int first = firstOf(node, otherNode), second = secondOf(node, otherNode);
	IntArrayList held = edgesOf(first), edges = new IntArrayList();
	for(int i = 0; i < held.size(); i++) {
		int edge = held.getInt(i);
		if(firstNodeOf(edge) == first && secondNodeOf(edge) == second) { edges.add(edge); }
	}
	if(edges.isEmpty()) { return false; }
	int request = journal.beginRecordingActivity(this, "breakConnection", params, comment);
	try { for(int i = 0; i < edges.size(); i++) { core.delegateRemoval(edgeArity, edges.getInt(i), null); } }
	catch(RuntimeException | Error e) {
		abort(request);
		throw e;
	}
	complete(request);
	return true;
}

/** An edge leaving its arity is taken off its nodes; a node leaving its arity removes its edges through the core. */
@Override
public boolean forget(int arity, int identity) {
	if(arity == edgeArity) {
		if(firstNodeOf(identity) < 0) { return false; }
		int request = journal.beginRecordingActivity(this, "forget", params, null);
		unlink(identity);
		complete(request);
		return true;
	}
	if(arity != nodeArity) { return false; }
	IntArrayList edges = edgesOf(identity);
	if(edges.isEmpty()) { return false; }
	int request = journal.beginRecordingActivity(this, "forget", params, null);
	for(int i = 0; i < edges.size(); i++) { core.delegateRemoval(edgeArity, edges.getInt(i), null); }
	complete(request);
	return true;
}


/** Puts the edge in place and records it. */
void link(int edge, int first, int second) {
	linkUnchecked(edge, first, second);
	params[0] = edge;
	params[1] = first;
	params[2] = second;
	journal.recordActivityStep(LINK, 3);
}

void unlink(int edge) {
	params[0] = edge;
	params[1] = firstNodeOf(edge);
	params[2] = secondNodeOf(edge);
	unlinkUnchecked(edge);
	journal.recordActivityStep((byte) -LINK, 3);
}

@Override
public void doTraceableStep(byte stepPair, int[] params, int index) {
	switch(stepPair) {
		case LINK -> linkUnchecked(params[index], params[index + 1], params[index + 2]);
		case -LINK -> unlinkUnchecked(params[index]);
		default -> throw new IllegalArgumentException("no step " + stepPair);
	}
}

/** The ends first, then the storage, which may read them. The places up to the edge's are grown empty. */
void linkUnchecked(int edge, int first, int second) {
	while(ends.size() < 2 * edge + 2) { ends.add(-1); }
	ends.set(2 * edge, first);
	ends.set(2 * edge + 1, second);
	index(edge, first, second);
}

void unlinkUnchecked(int edge) {
	unindex(edge, firstNodeOf(edge), secondNodeOf(edge));
	ends.set(2 * edge, -1);
	ends.set(2 * edge + 1, -1);
}


/** The arity slots and the rules, the storage's settings, then each edge as (edge, first, second), so the file carries the graph and not a storage's layout. */
@Override
public boolean saveTo(DataOutputStream dos) throws IOException {
	new Rules(nodeArity, edgeArity, directed, acyclic, allowsLoops, allowsParallelEdges).write(dos);
	writeSettings(dos);
	int count = 0;
	for(int at = 0; at < ends.size(); at += 2) { if(ends.getInt(at) >= 0) { count++; } }
	dos.writeInt(count);
	for(int at = 0; at < ends.size(); at += 2) {
		if(ends.getInt(at) < 0) { continue; }
		dos.writeInt(at / 2);
		dos.writeInt(ends.getInt(at));
		dos.writeInt(ends.getInt(at + 1));
	}
	return true;
}

/**
 * The edges {@link #saveTo} wrote, after the rules and the settings a subclass's loadFrom read to build
 * itself, then the node side built once from all of them. Refuses an edge written twice, a pair out of
 * order on a directionless graph, and one the loop or parallel rule forbids.
 * */
void readEdges(DataInputStream dis) throws IOException {
	LongOpenHashSet met = new LongOpenHashSet();
	for(int i = 0, count = dis.readInt(); i < count; i++) {
		int edge = dis.readInt(), first = dis.readInt(), second = dis.readInt();
		if(edge < 0 || first < 0 || second < 0 || (!directed && first > second)) { throw new IOException("edge " + edge + " (" + first + ", " + second + ") is not a stored edge"); }
		if(firstNodeOf(edge) >= 0) { throw new IOException("edge " + edge + " is written twice"); }
		if(first == second && !allowsLoops) { throw new IOException("loop on " + first + " in a graph without loops"); }
		if(!allowsParallelEdges && !met.add((long) first << 32 | second)) { throw new IOException("parallel edge " + edge + " in a graph without them"); }
		while(ends.size() < 2 * edge + 2) { ends.add(-1); }
		ends.set(2 * edge, first);
		ends.set(2 * edge + 1, second);
	}
	rebuildIndex();
}

/** The arity slots and the four rules, as a store writes them and a loadFrom reads them back before its own settings. */
static final class Rules {
	final int nodeArity, edgeArity;
	final boolean directed, acyclic, allowsLoops, allowsParallelEdges;

	Rules(int nodeArity, int edgeArity, boolean directed, boolean acyclic, boolean allowsLoops, boolean allowsParallelEdges) {
		this.nodeArity = nodeArity;
		this.edgeArity = edgeArity;
		this.directed = directed;
		this.acyclic = acyclic;
		this.allowsLoops = allowsLoops;
		this.allowsParallelEdges = allowsParallelEdges;
	}

	void write(DataOutputStream dos) throws IOException {
		dos.writeInt(nodeArity);
		dos.writeInt(edgeArity);
		dos.writeBoolean(directed);
		dos.writeBoolean(acyclic);
		dos.writeBoolean(allowsLoops);
		dos.writeBoolean(allowsParallelEdges);
	}

	static Rules read(DataInputStream dis) throws IOException {
		int nodeArity = dis.readInt(), edgeArity = dis.readInt();
		boolean directed = dis.readBoolean(), acyclic = dis.readBoolean(), allowsLoops = dis.readBoolean();
		return new Rules(nodeArity, edgeArity, directed, acyclic, allowsLoops, dis.readBoolean());
	}
}


void requireLive(int node) {
	if(node < 0 || !core.getArity(nodeArity).occupied().contains(node)) {
		throw new IllegalArgumentException(node + " is not an occupied node of arity " + nodeArity);
	}
}

/** Aborts nested requests left open by the failure too, up to and including {@code request}. */
void abort(int request) {
	while(journal.abortRecordingActivity() != request) { }
}

void complete(int request) {
	int completed = journal.completeRecordingActivity();
	if(completed != request) { throw new IllegalStateException("began request " + request + ", completed " + completed); }
}


}
