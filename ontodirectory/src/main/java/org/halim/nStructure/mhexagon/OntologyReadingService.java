package org.halim.nStructure.mhexagon;

import org.halim.nStructure.ontology.core.segmentation.query.OntologyElementFilter;

import java.util.ArrayList;
import java.util.List;

public interface OntologyReadingService {

OntologyClassHandle getRootOntologyClass();

OntologyClassHandle getClassFromIdentity(int identity);

int getDomainIdentity();

default void setDomain(int domainIdentity) { setDomain(getClassFromIdentity(domainIdentity)); }

void setDomain(OntologyClassHandle domain);

ArrayList<OntologyElementHandle> getOntologyElements(int classIdentity);

ArrayList<OntologyElementHandle> getAllOntologyElements(int classIdentity);

default boolean isElementOf(int classIdentity, OntologyElementHandle element) {
	return isElementForFilter(getClassFromIdentity(classIdentity), element);
}

default boolean isDescendentOf(int classIdentity, OntologyElementHandle element) {
	return isDescendentForFilter(getClassFromIdentity(classIdentity), element);
}

/** For The Filter Only! Checks if the element has the tag. */
boolean isElementForFilter(OntologyClassHandle ontologyClass, OntologyElementHandle element);

/** For The Filter Only! Checks if the element has a tag descendent of the class. */
boolean isDescendentForFilter(OntologyClassHandle ontologyClass, OntologyElementHandle element);

/** Returns all the ontology Elements that satisfy the filter for the domain. */
List<OntologyElementHandle> getOntologyElements(OntologyElementFilter filter);

void addOntologyServiceListener(OntologyServiceListener ontologyServiceListener);

interface OntologyServiceListener {
	void setOntologyDomain(OntologyClassHandle ontologyClass);
	void printRequestProcess(Object message);
}

interface OntologyManagingService extends OntologyReadingService {

	int filterToClass(OntologyElementFilter filter, String newClassName);

	/** Ports legacy's bulk "copy contents to another class" -- composed from the same
	 *  addParent/tagElement primitives legacy used in a loop, since the new engine has no single
	 *  primitive for this (nothing else needed it). */
	void copyContentsTo(int sourceIdentity, int targetIdentity, boolean copyParents, boolean copyChildren, boolean copyFiles);

	/** Return the identity of the newly created class. */
	int createOntologyClass(String name, List<Integer> parentIds, List<Integer> childrenIds);

	void removeOntologyClass(int classIdentity);

	void addParent(int childId, int parentId);

	void removeParent(int childId, int parentId);

	void addElement(int classIdentity, int elementIdentity);

	void removeElement(int classIdentity, int elementIdentity);

	void renameOntologyClass(int classIdentity, String newName);

	void renameElementActualName(int elementIdentity, String newName);

	boolean canUndo();
	boolean canRedo();
	void undo();
	void redo();

	void deleteElements(List<OntologyElementHandle> selectedElements);

}


}
