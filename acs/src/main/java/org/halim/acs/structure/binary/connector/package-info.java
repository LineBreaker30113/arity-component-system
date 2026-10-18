/**
 * Connectors: an identity of one arity connected to an identity of another, or not, and nothing more. One
 * relation, so each type here is one way of storing it, fast for a different case.
 * <ul>
 *   <li>{@link org.halim.acs.structure.binary.connector.ACS_ConnectorComponentBase} -
 *       the contract: the primary arity and the complement, connections made and broken, both sides read.</li>
 *   <li>{@link org.halim.acse.structure.binary.connector.DoubleArrayedConnectorComponent} -
 *       an int list a side, both directions read without a scan.</li>
 *   <li>{@link org.halim.acse.structure.binary.connector.SingleRoaringConnectorComponent} -
 *       one bitmap over a stride of the persistent side, recalculated when a connection does not fit; cheap
 *       from the volatile side.</li>
 * </ul>
 * */
package org.halim.acs.structure.binary.connector;
