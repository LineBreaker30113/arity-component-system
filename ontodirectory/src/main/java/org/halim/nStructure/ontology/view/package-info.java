/**
 * Home for the hexagonal architecture's client-facing ports: the query and command contracts an
 * external caller (a GUI, a CLI, tests) uses to reach the ontology engine, plus — later — a driven
 * port for persisting the engine's state to an arbitrary data stream. Callers depend on these
 * contracts, never on {@code core} directly; {@code core}'s segmentation services back them.
 */
package org.halim.nStructure.ontology.view;

