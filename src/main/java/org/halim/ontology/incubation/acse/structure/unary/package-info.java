/**
 * Structures over one arity, which are always some kind of graph: here, hierarchies whose element 0 is the
 * root, every element hanging from it until it is given a parent. Neither holds data on its edges; a graph
 * whose edges carry data is a node-edge structure over two arities.
 * <ul>
 *   <li>{@link org.halim.ontology.incubation.acse.structure.unary.TightSetHierarchyDirectedAcyclicIntGraphComponent} -
 *       a DAG of many parents, kept to its transitive reduction, for many leaves and few branches.</li>
 *   <li>{@link org.halim.ontology.incubation.acse.structure.unary.FolderHierarchyDirectedAcyclicIntGraphComponent} -
 *       its single-parent sibling: a tree hanging from the root.</li>
 * </ul>
 * */
package org.halim.ontology.incubation.acse.structure.unary;
