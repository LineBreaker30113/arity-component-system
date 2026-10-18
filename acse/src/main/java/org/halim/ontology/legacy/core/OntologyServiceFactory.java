package org.halim.ontology.legacy.core;

import java.lang.reflect.Proxy;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * The abstract factory for the engine's ports: given a bare {@code segments} implementation and the
 * {@code segmentation} port interface it implements, returns a dynamic proxy of that interface backed
 * by {@link OntologyServiceProxy}. Cross-cutting concerns (locking, undo/redo transaction boundaries,
 * logging) live exclusively behind this proxy — {@code segments} implementations must stay pure
 * implementations of their port, with no locking or undo/redo bookkeeping of their own beyond
 * recording the domain-specific undo/redo closures via {@link TransactionContext#record}.
 * <p>
 * Also owns the engine's single undo/redo history ({@link UndoRedoStack}) and exposes
 * {@link #runAtomically} — the entry point composed multi-service operations (e.g. {@code view}'s
 * command services) use to run several already-proxied calls as one atomically-locked, one-undo-entry
 * transaction, relying on {@link TransactionContext}'s nesting-awareness and the write lock's
 * reentrancy for the owning thread.
 */
public class OntologyServiceFactory {

private final ReentrantReadWriteLock engineLock;
private final UndoRedoStack undoRedoStack;
private final OntologyServiceLogger logger;

public OntologyServiceFactory() {
	this(OntologyServiceLogger.NO_OP);
}

public OntologyServiceFactory(OntologyServiceLogger logger) {
	// A single lock for the entire engine ensures thread safety across all segmented services.
	this.engineLock = new ReentrantReadWriteLock();
	this.undoRedoStack = new UndoRedoStack();
	this.logger = logger;
}

/**
 * Creates a dynamically proxied service wrapping the core implementation.
 * * @param implementation The concrete class (e.g., DoubleArrayedIndexer)
 * @param interfaceType  The defining Port interface (e.g., OntologyClassificationSegmentation.class)
 * @param <T>            The type of the interface
 * @return A thread-safe, transactional proxy of type T
 */
@SuppressWarnings("unchecked")
public <T> T createSecureProxy(Object implementation, Class<T> interfaceType) {

	// Ensure the implementation actually implements the requested interface
	if (!interfaceType.isAssignableFrom(implementation.getClass())) {
		throw new IllegalArgumentException(
			"Implementation " + implementation.getClass().getSimpleName() +
				" does not implement " + interfaceType.getSimpleName()
		);
	}

	return (T) Proxy.newProxyInstance(
		interfaceType.getClassLoader(),
		new Class<?>[]{interfaceType}, // The proxy will implement this exact interface
		new OntologyServiceProxy(implementation, engineLock, undoRedoStack, logger, interfaceType.getSimpleName())
	);
}

/**
 * Runs {@code body} as a single atomic, undoable operation: holds the engine's write lock for the
 * whole duration (reentrant for the owning thread, so nested calls into other proxied services —
 * including other {@code runAtomically} calls, e.g. a composed command calling another composed
 * command — reacquire instantly) and wraps it in one outer {@link TransactionContext} transaction,
 * so every closure any nested proxied call records lands in a single undo-stack entry named
 * {@code actionName} instead of one entry per nested call.
 */
public <T> T runAtomically(String actionName, Supplier<T> body) {
	engineLock.writeLock().lock();
	try {
		TransactionContext.begin(actionName);
		try {
			T result = body.get();
			TransactionContext.Transaction committed = TransactionContext.commit();
			if (committed != null) undoRedoStack.pushCommitted(committed);
			return result;
		} catch (RuntimeException | Error e) {
			TransactionContext.rollback();
			throw e;
		}
	} finally {
		engineLock.writeLock().unlock();
	}
}

public void runAtomically(String actionName, Runnable body) {
	runAtomically(actionName, () -> { body.run(); return null; });
}

/**
 * Acquires the engine's write lock and holds it until the returned handle is closed, pausing every
 * other mutation for as long as it's held. Used by storage/restore sessions to guarantee the
 * engine's state can't change out from under a multi-chunk save/load. Unlike {@link #runAtomically},
 * this does NOT open a {@link TransactionContext} transaction -- sessions read/write existing state
 * chunk by chunk and don't need undo/redo recording for that.
 */
public EngineLockHandle beginExclusiveSession() {
	return new EngineLockHandle(engineLock);
}

public boolean canUndo() { return undoRedoStack.canUndo(); }
public boolean canRedo() { return undoRedoStack.canRedo(); }
public String peekUndoActionName() { return undoRedoStack.peekUndoActionName(); }
public String peekRedoActionName() { return undoRedoStack.peekRedoActionName(); }

public void undo() {
	engineLock.writeLock().lock();
	try { undoRedoStack.undo(); } finally { engineLock.writeLock().unlock(); }
}

public void redo() {
	engineLock.writeLock().lock();
	try { undoRedoStack.redo(); } finally { engineLock.writeLock().unlock(); }
}
}