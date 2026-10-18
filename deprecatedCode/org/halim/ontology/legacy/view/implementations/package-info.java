/**
 * Concrete implementations of {@code org.halim.ontology.legacy.view}'s client-facing
 * query/command interfaces, each a thin adapter over one or more {@code core.segmentation}
 * services: {@code OntologyDataQueryServiceImpl} and {@code OntologyAnalysisServiceImpl} implement
 * the {@code view.query} services, {@code OntologyHierarchyManagementServiceImpl} and {@code
 * OntologyClassificationManagementServiceImpl} implement the {@code view.command} services, and
 * {@code DefaultOntologyService} is the composition root that wraps one {@code
 * OntologyServicesFactorySegmentation} and hands out all of the above plus undo/redo control as a
 * single {@code OntologyService}.
 * <p>
 * {@link org.halim.ontology.legacy.view.implementations.persistence} is this package's one
 * further concern: the driven port and session types for saving/loading engine state, which {@code
 * DefaultOntologyService} exposes via {@code beginStoring}/{@code beginLoading}.
 */
package org.halim.ontology.legacy.view.implementations;
