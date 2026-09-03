package org.halim.ontology.core.segmentation.command;

import org.halim.ontology.core.EngineLockHandle;
import org.halim.ontology.core.segmentation.query.OntologyFilteringServiceSegmentation;
import org.halim.ontology.core.segmentation.query.OntologyAnalyticsSegmentation;
import org.halim.ontology.core.segmentation.query.OntologyClassCatalogSegmentation;
import org.halim.ontology.core.segmentation.query.OntologyElementCatalogSegmentation;

import java.util.function.Supplier;

public interface OntologyServicesFactorySegmentation {

    OntologyClassificationSegmentation getIndexerService();
    OntologyHierarchySegmentation getHierarchyService();
    OntologyAnalyticsSegmentation getAnalyticsService();
    OntologyInplaceCommandingSegmentation getProcessingService();
    OntologyClassCatalogSegmentation getClassDataService();
    OntologyElementCatalogSegmentation getElementDataService();
    OntologyFilteringServiceSegmentation getFilteringService();

    /**
     * Runs {@code body} as a single atomic, undoable operation spanning any number of calls to the
     * services above — see {@code core.OntologyServiceFactory#runAtomically} for the locking/
     * transaction-nesting guarantees this relies on. This is the entry point composed multi-step
     * operations (e.g. {@code view}'s command services) use instead of calling several proxied
     * service methods directly in sequence.
     */
    <T> T runAtomically(String actionName, Supplier<T> body);
    default void runAtomically(String actionName, Runnable body) {
        runAtomically(actionName, () -> { body.run(); return null; });
    }

    boolean canUndo();
    boolean canRedo();
    String peekUndoActionName();
    String peekRedoActionName();
    void undo();
    void redo();

    /**
     * Acquires the engine's write lock and holds it until the returned handle is closed -- see
     * {@code core.OntologyServiceFactory#beginExclusiveSession}. Used by storage/restore sessions
     * ({@code view.persistence}) so a multi-chunk save/load can't be corrupted by a concurrent
     * mutation.
     */
    EngineLockHandle beginExclusiveSession();

}
