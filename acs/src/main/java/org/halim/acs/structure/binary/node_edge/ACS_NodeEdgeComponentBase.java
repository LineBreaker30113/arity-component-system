package org.halim.acs.structure.binary.node_edge;

import org.halim.acs.ACS_Core;
import org.halim.acs.structure.ACS_GraphComponentBase;
import org.halim.acs.structure.binary.node_cluster.ACS_NodeClusterComponentBase;

/**
 * A graph over two arities: the arity at index 0 holds the nodes and the one at index 1 the edges, so a
 * node and an edge each carry data of their own, and an edge is an identity like any other. The nodes are
 * the graph's elements, a node's parents the first nodes of the edges ending at it, so the reads, edits
 * and analyses of {@link ACS_GraphComponentBase} hold here. {@link #addParent} captures an edge only when
 * none joins the two, and {@link #removeParent} removes every edge from the parent, as
 * {@link #breakEdges} does.
 * <p>
 * It is the node-cluster whose every cluster, an edge, joins exactly two nodes, the same node twice for a
 * loop; with {@link #DIRECTED}, the first goes to the second. The connector's requests take a node and an
 * edge, so one that would leave an edge with other than two nodes is refused.
 * <p>
 * What kind of graph it is is the restrictions of {@link ACS_NodeClusterComponentBase} plus
 * {@link #NO_PARALLEL_EDGES}, so 0 is any symmetric graph, with loops and parallel edges. How the graph is
 * stored is the implementation's, and says nothing about which restrictions it takes.
 * {@link #NO_IMPLIED_EDGES} refuses rather than drops: an edge another path implies is not added, and one
 * that would imply an edge already there is refused, since dropping an edge would forget its data.
 * <p>
 * An edge's two nodes are its first and its second: where it starts and where it ends on a directed
 * graph, the smaller and the larger on a directionless one. Edges are made and removed with their
 * connections. Connecting two nodes captures the edge on the edge arity through the core, as a request
 * nested in the connection's, so undoing the connection removes the edge with it. An edge leaving its arity
 * - through the core, or by its connection breaking - is forgotten here, and a node leaving its arity
 * removes every edge on it through {@link ACS_Core#delegateRemoval}, which is the recursive step. An edge
 * identity captured any other way is no connection. The two arities must be different slots: an edge that
 * was also a node would be forgotten as both.
 * */
public interface ACS_NodeEdgeComponentBase extends ACS_NodeClusterComponentBase {

/** No more than one edge between the same two nodes, the same way round on a directed graph. */
int NO_PARALLEL_EDGES = 1 << 18;

@Override
default int getArityCount() { return 2; }

@Override
default boolean isRelatedArity(int identity) { return identity == getArityIdentity(0) || identity == getArityIdentity(1); }

default int getEdgeArity() { return getArityIdentity(1); }

default boolean allowsParallelEdges() { return (getRestrictions() & NO_PARALLEL_EDGES) == 0; }


/**
 * An edge from {@code node} to {@code otherNode}, or between them on a directionless graph, given back: a
 * new one captured on the edge arity, or, with {@link #NO_PARALLEL_EDGES}, the one already there. Refuses
 * a node its arity does not hold and an edge a restriction forbids.
 * */
int ensureEdge(int node, int otherNode, String comment);

/**
 * As {@link ACS_GraphComponentBase#setParent}; with {@link #AT_MOST_ONE_FROM}, the edge from its one parent
 * is kept and re-pointed, so its identity and its data stay and only its first node changes.
 * */
@Override
boolean setParent(int element, int parent, String comment);

/** Removes every edge from {@code node} to {@code otherNode}, or between them on a directionless graph, through the core. False when there was none. */
boolean breakEdges(int node, int otherNode, String comment);

/** An edge from {@code node} to {@code otherNode}, or between them on a directionless graph, or -1 for none; with parallel edges, the lowest. */
int edgeOf(int node, int otherNode);

default boolean hasEdge(int node, int otherNode) { return edgeOf(node, otherNode) >= 0; }

/** Where the edge starts, or its smaller node on a directionless graph; -1 when the edge is no connection here. */
int firstNodeOf(int edge);

/** Where the edge ends, or its larger node on a directionless graph; -1 when the edge is no connection here. */
int secondNodeOf(int edge);

/** How many edges the node is on, either way round; a loop counts once. */
int edgeCount(int node);

/** The node's edges, {@link #edgeCount} of them, ascending. */
void copyEdgesInto(int node, int[] dest, int offset);


/** The edges starting at the node; on a directionless graph, all of its edges. */
default int outEdgeCount(int node) {
	if(!isDirected()) { return edgeCount(node); }
	int[] all = new int[edgeCount(node)];
	copyEdgesInto(node, all, 0);
	int count = 0;
	for(int edge : all) { if(firstNodeOf(edge) == node) { count++; } }
	return count;
}

/** The edges starting at the node, {@link #outEdgeCount} of them, ascending. */
default void copyOutEdgesInto(int node, int[] dest, int offset) {
	int[] all = new int[edgeCount(node)];
	copyEdgesInto(node, all, 0);
	for(int edge : all) { if(!isDirected() || firstNodeOf(edge) == node) { dest[offset++] = edge; } }
}

/** The edges ending at the node; on a directionless graph, all of its edges. */
default int inEdgeCount(int node) {
	if(!isDirected()) { return edgeCount(node); }
	int[] all = new int[edgeCount(node)];
	copyEdgesInto(node, all, 0);
	int count = 0;
	for(int edge : all) { if(secondNodeOf(edge) == node) { count++; } }
	return count;
}

/** The edges ending at the node, {@link #inEdgeCount} of them, ascending. */
default void copyInEdgesInto(int node, int[] dest, int offset) {
	int[] all = new int[edgeCount(node)];
	copyEdgesInto(node, all, 0);
	for(int edge : all) { if(!isDirected() || secondNodeOf(edge) == node) { dest[offset++] = edge; } }
}


}
