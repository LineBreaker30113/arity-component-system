package org.halim.nStructure.archive.ahexagon.deprecated;

/**
 * A primitive functional interface representing a mathematical transition in the Data Lake.
 * Used for zero-allocation Method References in the Undo/Redo stack.
 */
@FunctionalInterface
public interface OntologyAction {
    void execute(int classIdentity, int elementIdentity);
}