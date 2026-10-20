package org.halim.nStructure.ontology.view;

import org.halim.nStructure.ontology.core.EngineLockHandle;
import org.halim.nStructure.ontology.core.segmentation.query.OntologyFilteringServiceSegmentation;
import org.halim.nStructure.ontology.view.command.OntologyClassificationManagementService;
import org.halim.nStructure.ontology.view.command.OntologyHierarchyManagementService;
import org.halim.nStructure.ontology.view.implementations.persistence.OntologyRestoreSession;
import org.halim.nStructure.ontology.view.implementations.persistence.OntologyStorageSession;
import org.halim.nStructure.ontology.view.query.OntologyAnalysisService;
import org.halim.nStructure.ontology.view.query.OntologyDataQueryService;

public interface OntologyService {

    OntologyDataQueryService getOntologyDataService();
    OntologyAnalysisService getAnalyticsService();
    OntologyFilteringServiceSegmentation getFilteringService();

    OntologyClassificationManagementService getClassificationManagementService();
    OntologyHierarchyManagementService getHierarchyManagementService();

    /**
     * Acquires the engine's write lock and holds it until the returned handle is closed, pausing
     * every other mutation for as long as it's held -- see {@code core.EngineLockHandle}. Used by
     * {@link OntologyStorageSession} to guarantee a storage session can't be corrupted by a concurrent
     * mutation.
     */
    EngineLockHandle beginExclusiveSession();

    /**
     * Opens a storage session -- see {@link OntologyStorageSession}. Every other mutation on this
     * engine is paused until the session is finalized.
     */
    OntologyStorageSession beginStoring();

    /**
     * Opens a restore session -- see {@link OntologyRestoreSession}. Every other mutation on this
     * engine is paused until the session is finalized.
     */
    OntologyRestoreSession beginLoading();

    boolean canUndo();
    boolean canRedo();
    void undo();
    void redo();

}
