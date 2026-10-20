/**
 * Two early-design artifacts with zero references anywhere else in the codebase: {@code
 * OntologyAction}, a zero-allocation functional interface for undo/redo closures, and {@code
 * BulkActionService}, a method annotation naming bulk-action strategies ({@code ARRAY}, {@code
 * ONE_ARRAY}, {@code ARRAY_ONE}, {@code A_A_ZIP}, {@code A_A_Cartesian}) with no processor reading
 * it. Both predate and appear superseded by {@code
 * org.halim.nStructure.ontology.core.TransactionContext}'s closure-recording approach to undo/redo.
 * Kept for historical reference; confirm with the project owner before building anything new
 * against them, and consider deletion instead.
 */
package org.halim.nStructure.archive.ahexagon.deprecated;
