/**
 * Of-by-to structures: each identity of a by arity takes a list from an of arity and gives a list to a to
 * arity, so a transition carries data of its own. What kind it is is a set of restrictions fixed at
 * construction; how it is stored is the type's.
 * <ul>
 *   <li>{@link org.halim.acs.structure.ternary.of_by_to.ACS_OfByToComponentBase} - the contract: the
 *       restrictions, the bys and their lists, both sides read, and the tos an of leads to.</li>
 * </ul>
 * */
package org.halim.acs.structure.ternary.of_by_to;
