package org.halim.ontology.view.implementations;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.halim.ontology.core.segmentation.command.OntologyClassificationSegmentation;
import org.halim.ontology.core.segmentation.query.OntologyClassCatalogSegmentation;
import org.halim.ontology.core.segmentation.query.OntologyElementCatalogSegmentation;
import org.halim.ontology.view.query.OntologyDataQueryService;

public class OntologyDataQueryServiceImpl implements OntologyDataQueryService {

private final OntologyClassCatalogSegmentation classDataService;
private final OntologyElementCatalogSegmentation elementDataService;
private final OntologyClassificationSegmentation indexerService;

public OntologyDataQueryServiceImpl(OntologyClassCatalogSegmentation classDataService,
                                    OntologyElementCatalogSegmentation elementDataService, OntologyClassificationSegmentation indexerService) {
    this.classDataService = classDataService;
    this.elementDataService = elementDataService;
    this.indexerService = indexerService;
}

@Override public String getClassName(int classIdentity) { return classDataService.getName(classIdentity); }
@Override public IntArrayList getParents(int classIdentity) { return classDataService.getParents(classIdentity); }
@Override public IntArrayList getChildren(int classIdentity) { return classDataService.getChildren(classIdentity); }
@Override public int getParentCount(int classIdentity) { return classDataService.getParentCount(classIdentity); }
@Override public int getChildrenCount(int classIdentity) { return classDataService.getChildrenCount(classIdentity); }
@Override public IntArrayList getElementsOf(int classIdentity) { return classDataService.getElementsOf(classIdentity); }
@Override public int getElementsCount(int classIdentity) { return classDataService.getElementsCount(classIdentity); }

@Override public String getElementName(int elementIdentity) { return elementDataService.getName(elementIdentity); }
@Override public IntArrayList getClassesOf(int elementIdentity) { return elementDataService.getClassesOf(elementIdentity); }
@Override public int getClassesCount(int elementIdentity) { return elementDataService.getClassesCount(elementIdentity); }
@Override public int[] getMostDescriptivePath(int elementIdentity) { return elementDataService.getMostDescriptivePath(elementIdentity); }

@Override public boolean isConnected(int classIdentity, int elementIdentity) { return indexerService.isConnected(classIdentity, elementIdentity); }

@Override public int getLastElementIndex() { return indexerService.getLastElementIndex(); }
@Override public boolean isElementTombstoned(int elementIdentity) { return indexerService.isElementTombstoned(elementIdentity); }
@Override public int getLastClassIndex() { return indexerService.getLastClassIndex(); }
@Override public boolean isClassTombstoned(int classIdentity) { return indexerService.isClassTombstoned(classIdentity); }

}
