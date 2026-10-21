/**
 * Node-cluster structures: a connector whose primaries, the nodes, form a graph through its complements,
 * the clusters, each joining the nodes connected to it. Symmetric by nature, directed as a restriction.
 * <ul>
 *   <li>{@link org.halim.acs.structure.binary.node_cluster.ACS_NodeClusterComponentBase} - the contract:
 *       the connector's requests and reads, and the graph's rules, reads and analyses over the nodes.</li>
 *   <li>{@link org.halim.acs.structure.binary.node_edge.ACS_NodeEdgeComponentBase} - the kind whose every
 *       cluster joins exactly two nodes.</li>
 * </ul>
 * */
package org.halim.acs.structure.binary.node_cluster;
