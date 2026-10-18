package org.halim.test;

import org.halim.ontology.legacy.core.OntologyEngine;
import org.halim.ontology.legacy.core.segmentation.query.OntologyElementFilter;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Regression coverage for a self-deadlock that only manifests through the real AOP proxy layer:
 * several read-locked methods on the analytics/element-data services internally called hierarchy
 * methods that carried no lock-bypass annotation, so the proxy defaulted them to requiring the
 * write lock — and a thread already holding the read lock can never acquire the write lock on the
 * same {@link java.util.concurrent.locks.ReentrantReadWriteLock}. No other test in this suite catches
 * this, because every other test constructs the raw {@code segments} classes directly, bypassing
 * {@link OntologyEngine}'s proxy layer entirely. Each call here is wrapped in
 * {@code assertTimeoutPreemptively}, which runs it on a separate thread and aborts instead of
 * hanging the whole suite if the deadlock regresses.
 * <p>
 * Every scenario below operates on class id 0 (root) only: {@link OntologyEngine}'s hierarchy
 * service reserves id 0 for root at construction, but nothing currently wires
 * {@code getIndexerService().newClass()} through to a corresponding slot in the hierarchy service,
 * so minting and placing an additional class isn't possible through the engine's public API today.
 * That's an unrelated, pre-existing gap — root alone is sufficient to exercise the deadlock path.
 */
public class OntologyEngineProxyLockingTest {

@Test
public void testGetDomainElementCount_ThroughRealProxy_DoesNotDeadlock() {
	OntologyEngine engine = new OntologyEngine();
	engine.getIndexerService().newClass(); // burns the indexer's id 0, aligning it with the hierarchy's reserved root
	int element = engine.getIndexerService().newElement();
	engine.getIndexerService().ensureConnection(0, element);

	assertTimeoutPreemptively(Duration.ofSeconds(3), () ->
		  engine.getAnalyticsService().getDomainElementCount(0));
}

@Test
public void testGetMostDescriptivePath_ThroughRealProxy_DoesNotDeadlock() {
	OntologyEngine engine = new OntologyEngine();
	engine.getIndexerService().newClass(); // burns the indexer's id 0, aligning it with the hierarchy's reserved root
	int element = engine.getIndexerService().newElement();
	engine.getIndexerService().ensureConnection(0, element);

	assertTimeoutPreemptively(Duration.ofSeconds(3), () ->
		  engine.getElementDataService().getMostDescriptivePath(element));
}

@Test
public void testGetAncestorAsSet_ThroughRealProxy_DoesNotDeadlock() {
	OntologyEngine engine = new OntologyEngine();

	assertTimeoutPreemptively(Duration.ofSeconds(3), () ->
		  engine.getAnalyticsService().getAncestorAsSet(0));
}

/**
 * {@code NameContaining} resolution calls {@code CatalogModuleBase.getDetailObject} on the
 * already-proxied element-data service from inside another already-locked proxied call
 * ({@code filteringService.getAllElements}) — the same nested-proxy-call shape that caused the
 * original deadlock this test class guards against. Catches a missing bypass annotation on
 * {@code CatalogModuleBase}/{@code AnyCatalogMutableBase}'s methods.
 */
@Test
public void testFilterElementsByName_ThroughRealProxy_DoesNotDeadlock() {
	OntologyEngine engine = new OntologyEngine();
	engine.getIndexerService().newClass(); // burns the indexer's id 0, aligning it with the hierarchy's reserved root
	int element = engine.getIndexerService().newElement();
	engine.getIndexerService().ensureConnection(0, element);
	engine.getElementDataService().setName(element, "root-file");

	assertTimeoutPreemptively(Duration.ofSeconds(3), () ->
		  engine.getFilteringService().getAllElements(
				new OntologyElementFilter.And(
					  new OntologyElementFilter.DirectElementOf(0),
					  new OntologyElementFilter.NameContaining("root"))));
}

}
