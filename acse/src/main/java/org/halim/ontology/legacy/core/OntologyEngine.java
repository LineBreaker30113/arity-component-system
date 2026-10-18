package org.halim.ontology.legacy.core;

import org.halim.ontology.legacy.core.segmentation.command.*;
import org.halim.ontology.legacy.core.segmentation.query.OntologyFilteringServiceSegmentation;
import org.halim.ontology.legacy.core.segmentation.query.OntologyAnalyticsSegmentation;
import org.halim.ontology.legacy.core.segmentation.query.OntologyClassCatalogSegmentation;
import org.halim.ontology.legacy.core.segmentation.query.OntologyElementCatalogSegmentation;
import org.halim.ontology.legacy.core.segmentation.query.AnyCatalogBase;
import org.halim.ontology.legacy.core.segments.data.OntologyAnalyticsCalculator;
import org.halim.ontology.legacy.core.segments.data.OntologyCatalogDetailStore;
import org.halim.ontology.legacy.core.segments.data.OntologyClassCatalog;
import org.halim.ontology.legacy.core.segments.data.OntologyElementCatalog;
import org.halim.ontology.legacy.core.segments.data.OntologyFilterResolverSegmentation;
import org.halim.ontology.legacy.core.segments.manager.DoubleArrayedIndexer;
import org.halim.ontology.legacy.core.segments.manager.OntologyHierarchyLister;
import org.halim.ontology.legacy.core.segments.manager.OntologyInplaceWalker;

import java.util.function.Supplier;

/**
 * Composition root of the ontology engine. Constructs each {@code segments} implementation,
 * wraps it behind its {@code segmentation} port via the single shared {@link OntologyServiceFactory}
 * (so every proxy shares one {@code ReentrantReadWriteLock} and nested calls between segments stay
 * reentrant-safe), and exposes the result through {@link OntologyServicesFactorySegmentation}.
 */
public class OntologyEngine implements OntologyServicesFactorySegmentation {

private final OntologyServiceFactory proxyFactory = new OntologyServiceFactory();

private final OntologyClassificationSegmentation indexerService;
private final OntologyHierarchySegmentation hierarchyService;
private final OntologyClassCatalogSegmentation classDataService;
private final OntologyElementCatalogSegmentation elementDataService;
private final OntologyInplaceCommandingSegmentation processingService;
private final OntologyAnalyticsSegmentation analyticsService;
private final OntologyFilteringServiceSegmentation filteringService;

/** Builds default, engine-owned storage for both catalogs (a fresh {@link OntologyCatalogDetailStore} each). */
public OntologyEngine() {
    this(new OntologyCatalogDetailStore(), new OntologyCatalogDetailStore());
}

/**
 * Builds the engine with client-supplied catalog storage — e.g. a real SoA-backed implementation
 * of {@link AnyCatalogBase}/{@link AnyCatalogMutableBase} — instead of the engine's default. A
 * caller that only wants to customize one side passes a fresh {@link OntologyCatalogDetailStore}
 * for the other.
 */
public <C extends AnyCatalogBase & AnyCatalogMutableBase, E extends AnyCatalogBase & AnyCatalogMutableBase>
      OntologyEngine(C classCatalogDetails, E elementCatalogDetails) {
    // OntologyHierarchyLister permanently reserves id 0 for root at construction, but the indexer
    // starts with no ids minted at all. Burn the indexer's first id (0) for root here so the two id
    // spaces are aligned for root from the moment the engine exists, matching how registerClass
    // keeps every subsequently-minted class aligned (see OntologyHierarchySegmentation#registerClass).
    // Called directly on the raw implementation, bypassing the proxy: this is an internal
    // construction detail, not a user action, so it must not land on the undo/redo history.
    DoubleArrayedIndexer rawIndexer = new DoubleArrayedIndexer();
    rawIndexer.newClass();
    this.indexerService = proxyFactory.createSecureProxy(rawIndexer, OntologyClassificationSegmentation.class);
    this.hierarchyService = proxyFactory.createSecureProxy(new OntologyHierarchyLister(), OntologyHierarchySegmentation.class);

    // Catalogs and analytics are built on top of the already-proxied hierarchy/indexer services, so
    // their internal delegation is covered by the same locking/undo-redo machinery as any other segment.
    this.classDataService = proxyFactory.createSecureProxy(
          new OntologyClassCatalog(hierarchyService, indexerService, classCatalogDetails), OntologyClassCatalogSegmentation.class);
    // Root (id 0, reserved above) must be named "File" for callers that display it (WelcomePage,
    // FilesView, ...). setName is BypassNonCommandConcerns (see OntologyClassCatalogSegmentation),
    // so this still doesn't pollute undo/redo history, matching the rawIndexer.newClass() call above.
    classDataService.setName(0, "File");
    this.elementDataService = proxyFactory.createSecureProxy(
          new OntologyElementCatalog(hierarchyService, indexerService, elementCatalogDetails), OntologyElementCatalogSegmentation.class);
    this.analyticsService = proxyFactory.createSecureProxy(
          new OntologyAnalyticsCalculator(hierarchyService, indexerService), OntologyAnalyticsSegmentation.class);

    // Filtering composes the already-proxied data/analytics services above; processing (below) then
    // composes the already-proxied filtering service, so filtering must be built in between the two.
    this.filteringService = proxyFactory.createSecureProxy(
          new OntologyFilterResolverSegmentation(hierarchyService, indexerService, analyticsService, classDataService, elementDataService),
          OntologyFilteringServiceSegmentation.class);
    this.processingService = proxyFactory.createSecureProxy(
          new OntologyInplaceWalker(hierarchyService, indexerService, filteringService), OntologyInplaceCommandingSegmentation.class);
}

@Override
public OntologyClassificationSegmentation getIndexerService() { return indexerService; }

@Override
public OntologyHierarchySegmentation getHierarchyService() { return hierarchyService; }

@Override
public OntologyClassCatalogSegmentation getClassDataService() { return classDataService; }

@Override
public OntologyElementCatalogSegmentation getElementDataService() { return elementDataService; }

@Override
public OntologyAnalyticsSegmentation getAnalyticsService() { return analyticsService; }

@Override
public OntologyInplaceCommandingSegmentation getProcessingService() { return processingService; }

@Override
public OntologyFilteringServiceSegmentation getFilteringService() { return filteringService; }

@Override
public <T> T runAtomically(String actionName, Supplier<T> body) { return proxyFactory.runAtomically(actionName, body); }

@Override
public boolean canUndo() { return proxyFactory.canUndo(); }

@Override
public boolean canRedo() { return proxyFactory.canRedo(); }

@Override
public String peekUndoActionName() { return proxyFactory.peekUndoActionName(); }

@Override
public String peekRedoActionName() { return proxyFactory.peekRedoActionName(); }

@Override
public void undo() { proxyFactory.undo(); }

@Override
public void redo() { proxyFactory.redo(); }

@Override
public EngineLockHandle beginExclusiveSession() { return proxyFactory.beginExclusiveSession(); }

}
