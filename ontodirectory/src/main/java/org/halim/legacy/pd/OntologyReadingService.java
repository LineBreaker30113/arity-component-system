package org.halim.legacy.pd;

import org.halim.legacy.dlake.FileInterface;
import org.halim.legacy.dlake.OntologyClass;
import org.halim.legacy.dlake.OntologyFilter;

import java.util.ArrayList;
import java.util.List;

public interface OntologyReadingService {

OntologyClass getRootOntologyClass();

OntologyClass getClassFromIdentity(int identity);

int getDomainIdentity();

default void setDomain(int domainIdentity) { setDomain(getClassFromIdentity(domainIdentity)); }

void setDomain(OntologyClass domain);

ArrayList<FileInterface> getOntologyElements(int classIdentity);

ArrayList<FileInterface> getAllOntologyElements(int classIdentity);

default boolean isElementOf(int classIdentity, FileInterface file) {
	return isElementForFilter(getClassFromIdentity(classIdentity), file);
}

default boolean isDescendentOf(int classIdentity, FileInterface file) {
	return isDescendentForFilter(getClassFromIdentity(classIdentity), file);
}

/** For The Filter Only! Checks if the element has the tag. */
boolean isElementForFilter(OntologyClass ontologyClass, FileInterface file);

/** For The Filter Only! Checks if the element has a tag descendent of the class. */
boolean isDescendentForFilter(OntologyClass ontologyClass, FileInterface file);

/** Returns all the ontology Elements that satisfy the filter for the domain. */
List<FileInterface> getOntologyElements(OntologyFilter filter);

void addOntologyServiceListener(OntologyServiceListener ontologyServiceListener);

interface OntologyServiceListener {
	void setOntologyDomain(OntologyClass ontologyClass);
	void printRequestProcess(Object message);
}

interface OntologyManagingService extends OntologyReadingService {
	
	int filterToClass(OntologyFilter filter, String newClassName);
	
	void copyContentsTo(int sourceIdentity, int targetIdentity, boolean copyParents, boolean copyChildren, boolean copyFiles);
	
	/** Return the identity of the newly created class. */
	int createOntologyClass(String name, List<Integer> parentIds, List<Integer> childrenIds);
	
	void removeOntologyClass(int classIdentity);
	
	void restoreOntologyClass(int identity, OntologyClass snapshot, ArrayList<FileInterface> fileSnap);
	
	void addParent(int childId, int parentId);
	
	void removeParent(int childId, int parentId);
	
	void addElement(int classIdentity, FileInterface file);
	
	void removeElement(int classIdentity, FileInterface file);
	
	void renameOntologyClass(int classIdentity, String newName);
	
	void renameElementActualName(FileInterface file, String newName);
	
	void undo();
	void redo();
	
	int createOntologyClassRaw(String name, List<Integer> parentIds, List<Integer> childrenIds);
	
	void addElementRaw(int classIdentity, FileInterface file);
	
	void deleteElements(List<FileInterface> selectedFiles);
	
	
}


}