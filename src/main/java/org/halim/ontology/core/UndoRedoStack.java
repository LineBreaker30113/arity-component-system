package org.halim.ontology.core;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Per-engine undo/redo history of committed {@link TransactionContext.Transaction}s. Not
 * internally synchronized — like {@code core.segments}' implementations, it relies entirely on the
 * shared {@link java.util.concurrent.locks.ReentrantReadWriteLock} write-lock discipline already
 * enforced by {@link OntologyServiceFactory}.
 */
public class UndoRedoStack {

private final Deque<TransactionContext.Transaction> undoStack = new ArrayDeque<>();
private final Deque<TransactionContext.Transaction> redoStack = new ArrayDeque<>();

public void pushCommitted(TransactionContext.Transaction tx) {
    undoStack.push(tx);
    redoStack.clear();
}

public boolean canUndo() { return !undoStack.isEmpty(); }
public boolean canRedo() { return !redoStack.isEmpty(); }

public String peekUndoActionName() { return undoStack.isEmpty() ? null : undoStack.peek().actionName; }
public String peekRedoActionName() { return redoStack.isEmpty() ? null : redoStack.peek().actionName; }

public void undo() {
    if (undoStack.isEmpty()) return;
    TransactionContext.Transaction tx = undoStack.pop();
    tx.undo();
    redoStack.push(tx);
}

public void redo() {
    if (redoStack.isEmpty()) return;
    TransactionContext.Transaction tx = redoStack.pop();
    tx.redo();
    undoStack.push(tx);
}

}
