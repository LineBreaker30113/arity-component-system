package org.halim.nStructure.ontology.view.implementations;

import org.halim.nStructure.ontology.core.EngineLockHandle;
import org.halim.nStructure.ontology.core.segmentation.query.OntologyFilteringServiceSegmentation;
import org.halim.nStructure.ontology.core.segmentation.command.OntologyServicesFactorySegmentation;
import org.halim.nStructure.ontology.view.OntologyService;
import org.halim.nStructure.ontology.view.command.OntologyClassificationManagementService;
import org.halim.nStructure.ontology.view.command.OntologyHierarchyManagementService;
import org.halim.nStructure.ontology.view.implementations.persistence.OntologyRestoreSession;
import org.halim.nStructure.ontology.view.implementations.persistence.OntologyStorageSession;
import org.halim.nStructure.ontology.view.query.OntologyAnalysisService;
import org.halim.nStructure.ontology.view.query.OntologyDataQueryService;

/**
 * Composition root for the client-facing {@link OntologyService} port: wraps one
 * {@link OntologyServicesFactorySegmentation} (typically a {@code core.OntologyEngine}) and wires each
 * query/command service to it, mirroring how {@code OntologyEngine} composes {@code core.segments}
 * one layer down. {@code new DefaultOntologyService(new OntologyEngine())} is the whole composition.
 */
public class DefaultOntologyService implements OntologyService {

private final OntologyServicesFactorySegmentation engine;
private final OntologyDataQueryService dataQueryService;
private final OntologyAnalysisService analysisService;
private final OntologyClassificationManagementService classificationManagementService;
private final OntologyHierarchyManagementService hierarchyManagementService;

public DefaultOntologyService(OntologyServicesFactorySegmentation engine) {
    this.engine = engine;
    this.dataQueryService = new OntologyDataQueryServiceImpl(
          engine.getClassDataService(), engine.getElementDataService(), engine.getIndexerService());
    this.analysisService = new OntologyAnalysisServiceImpl(engine.getAnalyticsService());
    this.classificationManagementService = new OntologyClassificationManagementServiceImpl(engine);
    this.hierarchyManagementService = new OntologyHierarchyManagementServiceImpl(engine);
}

@Override
public EngineLockHandle beginExclusiveSession() { return engine.beginExclusiveSession(); }

@Override
public OntologyStorageSession beginStoring() { return new OntologyStorageSession(this); }

@Override
public OntologyRestoreSession beginLoading() { return new OntologyRestoreSession(this); }

@Override
public OntologyDataQueryService getOntologyDataService() { return dataQueryService; }

@Override
public OntologyAnalysisService getAnalyticsService() { return analysisService; }

@Override
public OntologyFilteringServiceSegmentation getFilteringService() { return engine.getFilteringService(); }

@Override
public OntologyClassificationManagementService getClassificationManagementService() { return classificationManagementService; }

@Override
public OntologyHierarchyManagementService getHierarchyManagementService() { return hierarchyManagementService; }

@Override
public boolean canUndo() { return engine.canUndo(); }

@Override
public boolean canRedo() { return engine.canRedo(); }

@Override
public void undo() { engine.undo(); }

@Override
public void redo() { engine.redo(); }

}
