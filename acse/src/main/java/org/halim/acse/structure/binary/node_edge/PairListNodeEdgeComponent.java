package org.halim.acse.structure.binary.node_edge;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.halim.acs.ACSE_Core;
import org.halim.acs.ACSE_Journal;
import org.halim.acs.structure.binary.node_edge.ACS_NodeEdgeComponentBase;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


/**
 * The graph as one int list, two ints an edge: the nodes of edge {@code e} sit at {@code 2e} and
 * {@code 2e + 1}, the smaller first, so the edge's identity is where its pair is. Edges are the edge
 * arity's own identities, captured as its lowest free one when a connection is made, so they stay dense
 * and the list stays as long as the edges are many; a removed edge leaves {@code -1, -1} in its place, and
 * the next capture takes the place back.
 * <p>
 * The list is the whole of it, so the cost is the search: finding the edge of two nodes, a node's edges, or
 * whether two nodes are connected walks every edge. Edges are stored by their own place, never searched
 * for when made, so parallel edges cost nothing extra when allowed. Not multi thread safe.
 * */
public class PairListNodeEdgeComponent implements ACS_NodeEdgeComponentBase {

/** The edge (edge, smaller node, larger node); the negative triple removes it. */
static final byte LINK = 1;

final ACSE_Core core;
final ACSE_Journal journal;
final int nodeArity, edgeArity;
final boolean allowsLoops, allowsParallelEdges;
final int[] params = new int[3];

/** Two ints an edge, {@code -1, -1} where there is none. */
final IntArrayList pairs = new IntArrayList();


public PairListNodeEdgeComponent(ACSE_Core core, ACSE_Journal journal, int nodeArity, int edgeArity, boolean allowsLoops, boolean allowsParallelEdges) {
	if(nodeArity == edgeArity) { throw new IllegalArgumentException("nodes and edges in one arity " + nodeArity); }
	this.core = core;
	this.journal = journal;
	this.nodeArity = nodeArity;
	this.edgeArity = edgeArity;
	this.allowsLoops = allowsLoops;
	this.allowsParallelEdges = allowsParallelEdges;
}


@Override
public int getArityIdentity(int index) {
	return switch(index) {
		case 0 -> nodeArity;
		case 1 -> edgeArity;
		default -> throw new IndexOutOfBoundsException("arity " + index + " of a graph over two");
	};
}

@Override
public boolean allowsLoops() { return allowsLoops; }

@Override
public boolean allowsParallelEdges() { return allowsParallelEdges; }


/** A walk of every edge. */
@Override
public int edgeOf(int node, int otherNode) {
	int smaller = Math.min(node, otherNode), larger = Math.max(node, otherNode);
	for(int at = 0; at < pairs.size(); at += 2) {
		if(pairs.getInt(at) == smaller && pairs.getInt(at + 1) == larger && smaller >= 0) { return at / 2; }
	}
	return -1;
}

@Override
public int firstNodeOf(int edge) { return edge >= 0 && 2 * edge < pairs.size() ? pairs.getInt(2 * edge) : -1; }

@Override
public int secondNodeOf(int edge) { return edge >= 0 && 2 * edge < pairs.size() ? pairs.getInt(2 * edge + 1) : -1; }

/** A walk of every edge. */
@Override
public int edgeCount(int node) { return edgesOf(node).size(); }

/** A walk of every edge, so ascending. */
@Override
public void copyEdgesInto(int node, int[] dest, int offset) {
	IntArrayList held = edgesOf(node);
	held.getElements(0, dest, offset, held.size());
}

IntArrayList edgesOf(int node) {
	IntArrayList held = new IntArrayList();
	if(node < 0) { return held; }
	for(int at = 0; at < pairs.size(); at += 2) {
		if(pairs.getInt(at) == node || pairs.getInt(at + 1) == node) { held.add(at / 2); }
	}
	return held;
}


/** A new edge is the edge arity's lowest free identity, captured through the core, nested in this request so undo removes it with the connection. */
@Override
public int ensureConnection(int node, int otherNode, String comment) {
	requireLive(node);
	requireLive(otherNode);
	if(node == otherNode && !allowsLoops) { throw new IllegalArgumentException("no loops: " + node + " to itself"); }
	if(!allowsParallelEdges) {
		int held = edgeOf(node, otherNode);
		if(held >= 0) { return held; }
	}
	int request = journal.beginRecordingActivity(this, "ensureConnection", params, comment);
	int edge;
	try {
		edge = Math.toIntExact(core.captureIdentity(edgeArity, null));
		link(edge, Math.min(node, otherNode), Math.max(node, otherNode));
	} catch(RuntimeException | Error e) {
		abort(request);
		throw e;
	}
	complete(request);
	return edge;
}

/** Every edge between the two, removed through the core, which forgets each here. */
@Override
public boolean breakConnection(int node, int otherNode, String comment) {
	int smaller = Math.min(node, otherNode), larger = Math.max(node, otherNode);
	IntArrayList edges = new IntArrayList();
	for(int at = 0; at < pairs.size(); at += 2) {
		if(smaller >= 0 && pairs.getInt(at) == smaller && pairs.getInt(at + 1) == larger) { edges.add(at / 2); }
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

/** An edge leaving its arity clears its place; a node leaving its arity removes its edges through the core. */
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


/** Puts the pair in the edge's place and records it. */
void link(int edge, int smaller, int larger) {
	linkUnchecked(edge, smaller, larger);
	params[0] = edge;
	params[1] = smaller;
	params[2] = larger;
	journal.recordActivityStep(LINK, 3);
}

void unlink(int edge) {
	params[0] = edge;
	params[1] = pairs.getInt(2 * edge);
	params[2] = pairs.getInt(2 * edge + 1);
	linkUnchecked(edge, -1, -1);
	journal.recordActivityStep((byte) -LINK, 3);
}

@Override
public void doTraceableStep(byte stepPair, int[] params, int index) {
	switch(stepPair) {
		case LINK -> linkUnchecked(params[index], params[index + 1], params[index + 2]);
		case -LINK -> linkUnchecked(params[index], -1, -1);
		default -> throw new IllegalArgumentException("no step " + stepPair);
	}
}

/** Grows the list to the edge's place, the places between left empty. */
void linkUnchecked(int edge, int smaller, int larger) {
	while(pairs.size() < 2 * edge + 2) { pairs.add(-1); }
	pairs.set(2 * edge, smaller);
	pairs.set(2 * edge + 1, larger);
}


/** The two arity slots and the two rules, then each edge as (edge, smaller, larger), so the file carries the graph and not the list's empty places. */
@Override
public boolean saveTo(DataOutputStream dos) throws IOException {
	dos.writeInt(nodeArity);
	dos.writeInt(edgeArity);
	dos.writeBoolean(allowsLoops);
	dos.writeBoolean(allowsParallelEdges);
	int count = 0;
	for(int at = 0; at < pairs.size(); at += 2) { if(pairs.getInt(at) >= 0) { count++; } }
	dos.writeInt(count);
	for(int at = 0; at < pairs.size(); at += 2) {
		if(pairs.getInt(at) < 0) { continue; }
		dos.writeInt(at / 2);
		dos.writeInt(pairs.getInt(at));
		dos.writeInt(pairs.getInt(at + 1));
	}
	return true;
}

/** Rebuilds what {@link #saveTo} wrote, attached to the core, outside the journal; refuses an edge written twice, a pair out of order, or one the rules forbid. */
public static PairListNodeEdgeComponent loadFrom(ACSE_Core core, ACSE_Journal journal, DataInputStream dis) throws IOException {
	int nodeArity = dis.readInt();
	int edgeArity = dis.readInt();
	boolean allowsLoops = dis.readBoolean();
	PairListNodeEdgeComponent graph = new PairListNodeEdgeComponent(core, journal, nodeArity, edgeArity, allowsLoops, dis.readBoolean());
	// The pairs met so far, so refusing a parallel edge is a lookup rather than a walk of the list.
	LongOpenHashSet met = new LongOpenHashSet();
	for(int i = 0, count = dis.readInt(); i < count; i++) {
		int edge = dis.readInt();
		int smaller = dis.readInt();
		int larger = dis.readInt();
		if(edge < 0 || smaller < 0 || smaller > larger) { throw new IOException("edge " + edge + " (" + smaller + ", " + larger + ") is not a stored edge"); }
		if(graph.firstNodeOf(edge) >= 0) { throw new IOException("edge " + edge + " is written twice"); }
		if(smaller == larger && !allowsLoops) { throw new IOException("loop on " + smaller + " in a graph without loops"); }
		if(!graph.allowsParallelEdges && !met.add((long) smaller << 32 | larger)) { throw new IOException("parallel edge " + edge + " in a graph without them"); }
		graph.linkUnchecked(edge, smaller, larger);
	}
	return graph;
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
