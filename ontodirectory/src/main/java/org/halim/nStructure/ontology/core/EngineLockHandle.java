package org.halim.nStructure.ontology.core;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A held write-lock, released exactly once via {@link #close()}. Used by storage/restore sessions
 * ({@code view.persistence}) to pause every other mutation on the engine for the duration of a
 * multi-chunk save/load -- unlike {@link OntologyServiceFactory#runAtomically}, which holds the lock
 * only for the duration of a single call, this is held across multiple separate public method calls
 * until explicitly released. Callers should acquire it via try-with-resources so a forgotten or
 * exceptional session still releases the lock.
 */
public final class EngineLockHandle implements AutoCloseable {

private final ReentrantReadWriteLock lock;
private volatile boolean released = false;

EngineLockHandle(ReentrantReadWriteLock lock) {
    this.lock = lock;
    lock.writeLock().lock();
}

@Override
public void close() {
    if (released) return;
    released = true;
    lock.writeLock().unlock();
}

}
