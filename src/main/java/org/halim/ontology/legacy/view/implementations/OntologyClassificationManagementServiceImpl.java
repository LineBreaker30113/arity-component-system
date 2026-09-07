package org.halim.ontology.legacy.view.implementations;

import org.halim.ontology.legacy.core.segmentation.command.OntologyClassificationSegmentation;
import org.halim.ontology.legacy.core.segmentation.query.OntologyElementCatalogSegmentation;
import org.halim.ontology.legacy.core.segmentation.command.OntologyServicesFactorySegmentation;
import org.halim.ontology.legacy.view.command.OntologyClassificationManagementService;

public class OntologyClassificationManagementServiceImpl implements OntologyClassificationManagementService {

private final OntologyServicesFactorySegmentation engine;
private final OntologyClassificationSegmentation indexerService;
private final OntologyElementCatalogSegmentation elementDataService;

public OntologyClassificationManagementServiceImpl(OntologyServicesFactorySegmentation engine) {
    this.engine = engine;
    this.indexerService = engine.getIndexerService();
    this.elementDataService = engine.getElementDataService();
}

@Override
public int createElement(String name, int... classIdentities) {
    return engine.runAtomically("CREATE_ELEMENT", () -> {
        int id = indexerService.newElement();
        elementDataService.setName(id, name);
        if (classIdentities.length == 0) indexerService.ensureConnection(0, id);
        else indexerService.ensureConnection(classIdentities, id);
        return id;
    });
}

@Override
public void deleteElement(int elementIdentity) { indexerService.deleteElement(elementIdentity); }

@Override
public void deleteElements(int[] elementIdentities) { indexerService.deleteElement(elementIdentities); }

@Override
public void tagElement(int classIdentity, int elementIdentity) { indexerService.ensureConnection(classIdentity, elementIdentity); }

@Override
public void tagElements(int classIdentity, int[] elementIdentities) { indexerService.ensureConnection(classIdentity, elementIdentities); }

@Override
public void tagElementToClasses(int[] classIdentities, int elementIdentity) { indexerService.ensureConnection(classIdentities, elementIdentity); }

@Override
public void untagElement(int classIdentity, int elementIdentity) {
    engine.runAtomically("UNTAG_ELEMENT", () -> {
        indexerService.breakConnection(classIdentity, elementIdentity);
        if (indexerService.getClassesCount(elementIdentity) == 0) indexerService.ensureConnection(0, elementIdentity);
        return null;
    });
}

@Override
public void untagElements(int classIdentity, int[] elementIdentities) {
    engine.runAtomically("UNTAG_ELEMENTS", () -> {
        indexerService.breakConnection(classIdentity, elementIdentities);
        for (int elementId : elementIdentities) {
            if (indexerService.getClassesCount(elementId) == 0) indexerService.ensureConnection(0, elementId);
        }
        return null;
    });
}

@Override
public void untagElementFromClasses(int[] classIdentities, int elementIdentity) {
    engine.runAtomically("UNTAG_ELEMENT_FROM_CLASSES", () -> {
        indexerService.breakConnection(classIdentities, elementIdentity);
        if (indexerService.getClassesCount(elementIdentity) == 0) indexerService.ensureConnection(0, elementIdentity);
        return null;
    });
}

@Override
public void renameElement(int elementIdentity, String newName) { elementDataService.setName(elementIdentity, newName); }

@Override
public void restoreElement(int elementIdentity) { indexerService.registerElement(elementIdentity); }

}
