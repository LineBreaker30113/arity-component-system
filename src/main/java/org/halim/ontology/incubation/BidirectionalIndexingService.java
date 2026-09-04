package org.halim.ontology.incubation;

import org.halim.ontology.core.segmentation.command.OntologyClassificationSegmentation;
import org.halim.ontology.core.segmentation.helper.OntologyQueryable;
import org.halim.ontology.core.segmentation.helper.OntologyTransaction;

public interface BidirectionalIndexingService {

interface OntologyClassificationChangeListener {
	void onClassCreated(int classIdentity);
	void onClassDeleted(int classIdentity);
	void onElementCreated(int elementIdentity);
	void onElementDeleted(int elementIdentity);
	void onConnectionMade(int classIdentity, int elementIdentity);
	void onConnectionBreak(int classIdentity, int elementIdentity);
}

@OntologyTransaction(action = "ADD_CLASSIFICATION_LISTENER")
void addOntologyClassificationChangeListener(OntologyClassificationSegmentation.OntologyClassificationChangeListener occl);


// --- MUTATION TRANSACTIONS ---
@OntologyTransaction(action = "ENSURE_LINK")
void ensureConnection(int classIdentity, int elementIdentity);

@OntologyTransaction(action = "ENSURE_LINK_BULK_ZIP")
default void ensureConnection(int[] classIdentities, int[] elementIdentities) {
	if(classIdentities.length != elementIdentities.length) throw new IllegalArgumentException("Size Mismatch!");
	for(int i = 0; i < elementIdentities.length; i++) ensureConnection(classIdentities[i], elementIdentities[i]);
}

@OntologyTransaction(action = "ENSURE_LINK_BULK_1_TO_N")
default void ensureConnection(int classIdentity, int[] elementIdentities) {
	for(int elementId : elementIdentities) ensureConnection(classIdentity, elementId);
}

@OntologyTransaction(action = "ENSURE_LINK_BULK_N_TO_1")
default void ensureConnection(int[] classIdentities, int elementIdentity) {
	for(int classIdentity : classIdentities) ensureConnection(classIdentity, elementIdentity);
}


@OntologyTransaction(action = "BREAK_LINK")
void breakConnection(int classIdentity, int elementIdentity);

@OntologyTransaction(action = "BREAK_LINK_BULK_ZIP")
default void breakConnection(int[] classIdentities, int[] elementIdentities) {
	if(classIdentities.length != elementIdentities.length) throw new IllegalArgumentException("Mismatch");
	for(int i = 0; i < elementIdentities.length; i++) breakConnection(classIdentities[i], elementIdentities[i]);
}

@OntologyTransaction(action = "BREAK_LINK_BULK_1_TO_N")
default void breakConnection(int classIdentity, int[] elementIdentities) {
	for(int elementId : elementIdentities) breakConnection(classIdentity, elementId);
}

@OntologyTransaction(action = "BREAK_LINK_BULK_N_TO_1")
default void breakConnection(int[] classIdentities, int elementIdentity) {
	for(int classIdentity : classIdentities) breakConnection(classIdentity, elementIdentity);
}


@OntologyTransaction(action = "DELETE_CLASS")
void deleteClass(int classIdentity);

@OntologyTransaction(action = "DELETE_CLASS_BULK")
default void deleteClass(int[] classIdentities) {
	for(int ci : classIdentities) deleteClass(ci);
}


@OntologyTransaction(action = "DELETE_ELEMENT")
void deleteElement(int elementIdentity);

@OntologyTransaction(action = "DELETE_ELEMENT_BULK")
default void deleteElement(int[] elementIdentities) {
	for(int ei : elementIdentities) deleteElement(ei);
}


@OntologyTransaction(action = "NEW_CLASS")
int newClass();

@OntologyTransaction(action = "NEW_CLASS_BULK")
default int[] newClasses(int count) {
	int[] result = new int[count];
	for(int i = 0; i < count; i++) result[i] = newClass();
	return result;
}

@OntologyTransaction(action = "NEW_ELEMENT")
int newElement();

@OntologyTransaction(action = "NEW_ELEMENT_BULK")
default int[] newElements(int count) {
	int[] result = new int[count];
	for(int i = 0; i < count; i++) result[i] = newElement();
	return result;
}

/**
 * Ensures {@code elementIdentity} has a valid slot, growing internal storage up to and including
 * it if needed. Unlike {@link #newElement()} (which mints the next free/tombstoned id), this
 * targets a specific id -- used when restoring element data from storage, where the id is already
 * fixed by what was saved. No-op if the slot already exists.
 */
@OntologyTransaction(action = "REGISTER_ELEMENT")
void registerElement(int elementIdentity);

/**
 * Ensures {@code classIdentity} has a valid classification slot (an empty element list), growing
 * internal storage up to and including it if needed. Distinct from
 * {@code OntologyHierarchySegmentation#registerClass}, which only registers the hierarchy side (a
 * class needs both to be fully usable) -- used together when restoring class data from storage,
 * where the id is already fixed by what was saved. No-op if the slot already exists.
 */
@OntologyTransaction(action = "REGISTER_CLASSIFICATION_SLOT")
void registerClassificationSlot(int classIdentity);

/** The highest element id ever allocated (including tombstoned ones), or -1 if none have been. */
@OntologyQueryable.BypassNonQueryConcerns
int getLastElementIndex();

/** Whether {@code elementIdentity} is currently tombstoned (deleted, awaiting id reuse) or was
 *  never allocated at all -- as opposed to a live element that simply has zero class connections,
 *  which {@link org.halim.ontology.core.segmentation.helper.OntologyQueryable.BaseElementData#getClassesCount}
 *  can't distinguish from a tombstone on its own. */
@OntologyQueryable.BypassNonQueryConcerns
boolean isElementTombstoned(int elementIdentity);

/** The highest class id ever allocated (including tombstoned ones), or -1 if none have been. */
@OntologyQueryable.BypassNonQueryConcerns
int getLastClassIndex();

/** Whether {@code classIdentity} is currently tombstoned (deleted, awaiting id reuse) or was never
 *  allocated at all -- as opposed to a live class that simply has zero elements. */
@OntologyQueryable.BypassNonQueryConcerns
boolean isClassTombstoned(int classIdentity);


}
