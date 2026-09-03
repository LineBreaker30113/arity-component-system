package org.halim.ontology.core;

import java.util.ArrayList;
import java.util.List;

/**
 * A ThreadLocal Side-Channel that captures Deterministic Closures (Lambdas)
 * defining the exact mathematical inverses of Data Lake operations. Nesting-aware: only the
 * outermost {@link #begin}/{@link #commit}/{@link #rollback} in a call chain actually starts,
 * finalizes, or reverts a {@link Transaction} — nested calls (e.g. one proxied service method
 * calling another as part of a composed operation) fold their recorded closures into the same
 * outer transaction instead of starting a separate one.
 */
public class TransactionContext {

    public static class Transaction {
        public final String actionName;
        public final List<Runnable> undoActions = new ArrayList<>();
        public final List<Runnable> redoActions = new ArrayList<>();

        public Transaction(String actionName) { this.actionName = actionName; }

        public void undo() {
            // LIFO: Undo must execute in reverse chronological order
            for (int i = undoActions.size() - 1; i >= 0; i--) undoActions.get(i).run();
        }

        public void redo() {
            // FIFO: Redo executes in standard chronological order
            for (Runnable action : redoActions) action.run();
        }
    }

    private static final ThreadLocal<Transaction> activeTx = new ThreadLocal<>();
    private static final ThreadLocal<Integer> depth = ThreadLocal.withInitial(() -> 0);

    public static void begin(String actionName) {
        if (depth.get() == 0) activeTx.set(new Transaction(actionName));
        depth.set(depth.get() + 1);
    }

    /**
     * Records a pair of closures (lambdas) representing the exact state transition.
     */
    public static void record(Runnable undoLogic, Runnable redoLogic) {
        Transaction tx = activeTx.get();
        if (tx != null) {
            tx.undoActions.add(undoLogic);
            tx.redoActions.add(redoLogic);
        }
    }

    /**
     * Ends the current {@link #begin} frame. At depth 0 (the outermost frame), finalizes and
     * returns the completed transaction if it recorded anything, or {@code null} if nothing was
     * recorded or this was a nested (non-outermost) call. The caller decides where a non-null
     * result is stored (see {@link OntologyServiceFactory#runAtomically}).
     */
    public static Transaction commit() {
        int d = depth.get() - 1;
        depth.set(d);
        if (d > 0) return null;
        Transaction tx = activeTx.get();
        activeTx.remove();
        return (tx != null && (!tx.undoActions.isEmpty() || !tx.redoActions.isEmpty())) ? tx : null;
    }

    /**
     * Ends the current {@link #begin} frame due to failure. At depth 0 (the outermost frame),
     * actually replays every undo closure recorded so far — at any nesting level — before
     * discarding the transaction, so a mid-transaction exception leaves domain state exactly as it
     * was before the transaction started rather than partially mutated. Nested calls just unwind
     * depth and let the exception keep propagating, so only the outermost frame performs the real
     * rollback.
     */
    public static void rollback() {
        int d = depth.get() - 1;
        depth.set(d);
        if (d > 0) return;
        Transaction tx = activeTx.get();
        activeTx.remove();
        if (tx != null) tx.undo();
    }
}