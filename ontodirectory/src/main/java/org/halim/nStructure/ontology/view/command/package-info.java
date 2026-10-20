/**
 * Client-facing command services: what other modules that use the ontology engine call to mutate
 * it — hierarchy structure, elements and their class connections, and filter-driven bulk
 * classification — as single atomic, undoable operations, without depending on {@code core} directly.
 */
package org.halim.nStructure.ontology.view.command;
