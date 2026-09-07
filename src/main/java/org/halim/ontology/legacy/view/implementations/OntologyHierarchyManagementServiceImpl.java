package org.halim.ontology.legacy.view.implementations;

import org.halim.ontology.legacy.core.segmentation.query.OntologyClassCatalogSegmentation;
import org.halim.ontology.legacy.core.segmentation.query.OntologyElementFilter;
import org.halim.ontology.legacy.core.segmentation.query.OntologyFilteringServiceSegmentation;
import org.halim.ontology.legacy.core.segmentation.command.OntologyClassificationSegmentation;
import org.halim.ontology.legacy.core.segmentation.command.OntologyHierarchySegmentation;
import org.halim.ontology.legacy.core.segmentation.command.OntologyServicesFactorySegmentation;
import org.halim.ontology.legacy.view.command.OntologyHierarchyManagementService;

public class OntologyHierarchyManagementServiceImpl implements OntologyHierarchyManagementService {

private final OntologyServicesFactorySegmentation engine;
private final OntologyHierarchySegmentation hierarchyService;
private final OntologyClassificationSegmentation indexerService;
private final OntologyClassCatalogSegmentation classDataService;
private final OntologyFilteringServiceSegmentation filteringService;

public OntologyHierarchyManagementServiceImpl(OntologyServicesFactorySegmentation engine) {
    this.engine = engine;
    this.hierarchyService = engine.getHierarchyService();
    this.indexerService = engine.getIndexerService();
    this.classDataService = engine.getClassDataService();
    this.filteringService = engine.getFilteringService();
}

@Override
public int createClass(String name, int... parentClassIdentities) {
    return engine.runAtomically("CREATE_CLASS", () -> {
        int id = indexerService.newClass();
        hierarchyService.registerClass(id);
        classDataService.setName(id, name);
        for (int parentId : parentClassIdentities) hierarchyService.addParent(id, parentId);
        return id;
    });
}

@Override
public void deleteClass(int classIdentity) {
    requireNotRoot(classIdentity);
    engine.runAtomically("DELETE_CLASS", () -> { deleteClassInternal(classIdentity); return null; });
}

@Override
public void deleteClasses(int[] classIdentities) {
    for (int id : classIdentities) requireNotRoot(id);
    engine.runAtomically("DELETE_CLASSES", () -> {
        for (int id : classIdentities) deleteClassInternal(id);
        return null;
    });
}

private void requireNotRoot(int classIdentity) {
    if (classIdentity == 0) throw new IllegalArgumentException("Cannot delete the root class");
}

/** Ports legacy's {@code OntologyHierarchySegmentation.removeOntologyClass} cleanup order (reparent
 *  children, clear own parent edges, retire the classification id, reanchor newly-orphaned
 *  elements to root) composed atomically instead of legacy's unguarded direct-mutation-before-
 *  recording pattern. */
private void deleteClassInternal(int classIdentity) {
    for (int child : hierarchyService.getChildren(classIdentity).toIntArray()) {
        hierarchyService.removeParent(child, classIdentity);
    }
    hierarchyService.clearParents(classIdentity);
    int[] taggedElements = indexerService.getElementsOf(classIdentity).toIntArray();
    indexerService.deleteClass(classIdentity);
    for (int elementId : taggedElements) {
        if (indexerService.getClassesCount(elementId) == 0) indexerService.ensureConnection(0, elementId);
    }
}

@Override
public void addParent(int classIdentity, int parentIdentity) { hierarchyService.addParent(classIdentity, parentIdentity); }

@Override
public void removeParent(int classIdentity, int parentIdentity) { hierarchyService.removeParent(classIdentity, parentIdentity); }

@Override
public void renameClass(int classIdentity, String newName) { classDataService.setName(classIdentity, newName); }

@Override
public int materializeFilterAsClass(OntologyElementFilter filter, String className, int... parentClassIdentities) {
    return engine.runAtomically("MATERIALIZE_FILTER_AS_CLASS", () -> {
        int newClassId = createClass(className, parentClassIdentities);
        int[] matches = filteringService.getAllElements(filter);
        if (matches.length > 0) indexerService.ensureConnection(newClassId, matches);
        return newClassId;
    });
}

@Override
public void restoreClass(int classIdentity, String name) {
    indexerService.registerClassificationSlot(classIdentity);
    if (classIdentity != 0) hierarchyService.registerClass(classIdentity); // root's hierarchy slot always exists
    classDataService.setName(classIdentity, name);
}

}
