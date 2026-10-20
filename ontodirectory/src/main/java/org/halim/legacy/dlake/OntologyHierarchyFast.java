package org.halim.legacy.dlake;

import org.halim.legacy.OntoDirectoryException;
import org.halim.legacy.pd.OntologyMemento;
import org.halim.legacy.pd.OntologyReadingService;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/**
 * For speed, it uses only the identity numbers, good for organizing the data lake.
 * But it is forbidden to interact with the files during {@link OntologyHierarchyFast#vacuumDatabase(Path)}.
 */
public class OntologyHierarchyFast {

public ArrayList<OntologyClass> ontologyClasses;
public ArrayList<ArrayList<FileInterface>> filesPerOntology;
public ArrayList<FileInterface> fileInterfaces;

public OntologyHierarchyFast() {
	ontologyClasses = new ArrayList<>();
	filesPerOntology = new ArrayList<>();
	fileInterfaces = new ArrayList<>();
	ontologyClasses.add(OntologyClass.makeROOT_ONTOLOGY_CLASS("File"));
	filesPerOntology.add(new ArrayList<>());
}

private void ifNotNull(OntologyClass instance, Consumer<OntologyClass> action) {
	if(instance != null) action.accept(instance);
}

/** All the actions to take after loading the data lake. For now just creating the fast arrays. */
public void onLoad() {
	Iterator<OntologyClass> iterator = ontologyClasses.iterator(); int ocin = 0;
	while (iterator.hasNext()) {
		final int ci = ocin++; ifNotNull(iterator.next(), oc -> oc.identityNumber = ci);
		filesPerOntology.add(new ArrayList<>());
	}
	fileInterfaces.forEach(fi -> {
		if (fi != null && fi.tagsByIdentity != null) {
			fi.tagsByIdentity.forEach(fii -> filesPerOntology.get(fii).add(fi));
		}
	});
}

/**
 * Defragments the Data Lake by removing tombstoned (null) entries and recalculating
 * all foreign key references (Integer Identities).
 * WARNING: This alters persistent Integer IDs. Do not run implicitly on standard saves.
 */
public void vacuumDatabase(Path dataLakePath) {
	// 1. Clean File Interfaces & Rename
	fileInterfaces.removeIf(Objects::isNull);
	for (int i = 0; i < fileInterfaces.size(); i++) {
		FileInterface fi = fileInterfaces.get(i);
		fi.renameDisk(FileInterface.getDiskNameFor(i), dataLakePath);
		fi.identity = i;
	}
	
	// 2. Build Translation Map & Compact Arrays
	int[] oldToNewMap = new int[ontologyClasses.size()];
	int newIndex = 0;
	int reclaimed = 0;
	
	for (int oldIndex = 0; oldIndex < ontologyClasses.size(); oldIndex++) {
		OntologyClass oc = ontologyClasses.get(oldIndex);
		if (oc != null) {
			oldToNewMap[oldIndex] = newIndex;
			oc.identityNumber = newIndex; // CRITICAL: Fix internal ID
			ontologyClasses.set(newIndex, oc);
			filesPerOntology.set(newIndex, filesPerOntology.get(oldIndex));
			newIndex++;
		} else {
			oldToNewMap[oldIndex] = -1; // Mark as dead
			reclaimed++;
		}
	}
	
	if (reclaimed == 0) return;
	
	// Truncate the dead ends of the arrays
	ontologyClasses.subList(newIndex, ontologyClasses.size()).clear();
	filesPerOntology.subList(newIndex, filesPerOntology.size()).clear();
	
	// 3. Migrate Foreign Keys in FileInterfaces
	for (FileInterface fi : fileInterfaces) {
		if (fi.tagsByIdentity == null) continue;
		for (int i = fi.tagsByIdentity.size() - 1; i >= 0; i--) {
			int oldTagId = fi.tagsByIdentity.get(i);
			int mappedNewId = oldToNewMap[oldTagId];
			
			if (mappedNewId == -1) {
				fi.tagsByIdentity.remove(i); // Parent was tombstoned
			} else {
				fi.tagsByIdentity.set(i, mappedNewId); // Update foreign key
			}
		}
	}
	
	System.out.println("Database Vacuum Complete. Reclaimed " + reclaimed + " class identities.");
}

/**
 * Generates a privacy-masked, parser-friendly string representation of the DAG topology.
 * This is designed exclusively for telemetry and crash dumps.
 * No names or paths are exposed, ensuring complete user privacy.
 */
public String generatePrivacyMaskedDagDump() {
	StringBuilder dump = new StringBuilder();
	dump.append("--- ONTOLOGY DAG TOPOLOGY DUMP ---\n");
	for (OntologyClass oc : ontologyClasses) {
		if (oc == null) {
			dump.append("Class_TOMBSTONE\n");
			continue;
		}
		dump.append("Class_ID_").append(oc.identityNumber).append(" -> Parents: [");
		for (int i = 0; i < oc.parents.size(); i++) {
			dump.append(oc.parents.get(i).identityNumber);
			if (i < oc.parents.size() - 1) dump.append(", ");
		}
		dump.append("], Children: [");
		for (int i = 0; i < oc.children.size(); i++) {
			dump.append(oc.children.get(i).identityNumber);
			if (i < oc.children.size() - 1) dump.append(", ");
		}
		dump.append("]\n");
	}
	dump.append("--- END OF DAG DUMP ---\n");
	return dump.toString();
}

public OntologyClass getClassFromIdentity(int anInt) {
	if (anInt < 0 || anInt >= ontologyClasses.size()) return null;
	return ontologyClasses.get(anInt);
}

public int getIdentityFromClass(@NotNull OntologyClass oc) { return ontologyClasses.indexOf(oc); }

// ////////////////// Ontology Service:

public class OntologyHierarchyReader implements OntologyReadingService {
	public final OntologyHierarchyFast hierarchy;
	public OntologyClass domainEntry;
	
	public OntologyHierarchyReader() {
		hierarchy = OntologyHierarchyFast.this;
	}
	
	@Override
	public OntologyClass getRootOntologyClass() { return ontologyClasses.getFirst(); }
	
	@Override
	public OntologyClass getClassFromIdentity(int identity) {
		return OntologyHierarchyFast.this.getClassFromIdentity(identity);
	}
	
	@Override
	public int getDomainIdentity() { return domainEntry != null ? getIdentityFromClass(domainEntry) : -1; }
	
	@Override
	public void setDomain(OntologyClass domain) { this.domainEntry = domain; }
	
	@Override
	public ArrayList<FileInterface> getOntologyElements(int classIdentity) {
		if (classIdentity < 0 || classIdentity >= filesPerOntology.size()) return new ArrayList<>();
		return filesPerOntology.get(classIdentity);
	}
	
	@Override
	public ArrayList<FileInterface> getAllOntologyElements(int classIdentity) {
		OntologyClass startClass = getClassFromIdentity(classIdentity);
		if (startClass == null) return new ArrayList<>();
		
		HashSet<FileInterface> aggregatedInterfaces = new HashSet<>();
		startClass.forEachDescendant(descendant -> {
			aggregatedInterfaces.addAll(filesPerOntology.get(descendant.identityNumber));
		});
		
		return new ArrayList<>(aggregatedInterfaces);
	}
	
	public HashSet<FileInterface> getAllOntologyElementsSet(OntologyClass ontologyClass) {
		HashSet<FileInterface> interfaces = new HashSet<>(filesPerOntology.get(getIdentityFromClass(ontologyClass)));
		for (OntologyClass sub : ontologyClass.children) {
			interfaces.addAll(getAllOntologyElementsSet(sub));
		}
		return interfaces;
	}
	
	@Override
	public boolean isElementForFilter(OntologyClass ontologyClass, FileInterface file) {
		if (ontologyClass == null) return false;
		return filesPerOntology.get(getIdentityFromClass(ontologyClass)).contains(file);
	}
	
	@Override
	public boolean isDescendentForFilter(OntologyClass ontologyClass, @NotNull FileInterface file) {
		if (file.tagsByIdentity == null) return false;
		for(Integer i : file.tagsByIdentity) {
			OntologyClass tag = getClassFromIdentity(i);
			if(tag != null && tag.isAncestry(ontologyClass)) return true;
		}
		return false;
	}
	
	@Override
	public List<FileInterface> getOntologyElements(@NotNull OntologyFilter filter) {
		return new ArrayList<>(filter.resolve(this));
	}
	
	@Override
	public void addOntologyServiceListener(OntologyServiceListener ontologyServiceListener) {
		// Hook into Event Bus when implemented
	}
}

// Inherits reader capabilities to satisfy the port contract
public class OntologyHierarchyManager extends OntologyHierarchyReader implements OntologyReadingService.OntologyManagingService {
	public static final int stackSize = 100;
	private final Stack<OntologyMemento> undoStack = new Stack<>();
	private final Stack<OntologyMemento> redoStack = new Stack<>();
	
	public OntologyHierarchyManager() {
		super();
	}
	
	private boolean isReplaying = false;
	
	private void recordAction(OntologyMemento memento) {
		if (isReplaying) return;
		undoStack.push(memento);
		if (undoStack.size() > stackSize) undoStack.removeElementAt(0);
		redoStack.clear();
	}
	
	@Override
	public int filterToClass(OntologyFilter filter, String newClassName) {
		int newClassId = createOntologyClass(newClassName, (List<Integer>) null, null);
		OntologyClass newClass = getClassFromIdentity(newClassId);
		if (newClass == null) return -1;
		
		List<FileInterface> matchedFiles = getOntologyElements(filter);
		for (FileInterface file : matchedFiles) {
			addElement(newClassId, file);
		}
		
		return newClassId;
	}
	
	@Override
	public int createOntologyClass(String name, List<Integer> parentIds, List<Integer> childrenIds) {
		OntologyClass noc = new OntologyClass(name, getRootOntologyClass());
		if (parentIds != null) {
			for (Integer pId : parentIds) {
				OntologyClass parent = getClassFromIdentity(pId);
				if (parent != null) noc.addParent(parent);
				else
					throw new OntoDirectoryException("One element in the parents array of '" + name + "' was null");
			}
		}
		if (childrenIds != null) {
			for (Integer cId : childrenIds) {
				OntologyClass child = getClassFromIdentity(cId);
				if (child != null) child.addParent(noc);
				else
					throw new OntoDirectoryException("One element in the children array of '" + name + "' was null");
			}
		}
		
		insertNewClass(noc);
		
		OntologyMemento memento = new OntologyMemento();
		memento.actionType = OntologyMemento.ActionType.Create;
		memento.primaryTargetId = noc.identityNumber;
		recordAction(memento);
		return noc.identityNumber;
	}
	
	private void insertNewClass(OntologyClass noc) {
		int insertIndex = ontologyClasses.indexOf(null);
		if (insertIndex == -1) {
			noc.identityNumber = ontologyClasses.size();
			ontologyClasses.add(noc);
			filesPerOntology.add(new ArrayList<>());
		} else {
			noc.identityNumber = insertIndex;
			ontologyClasses.set(insertIndex, noc);
			filesPerOntology.set(insertIndex, new ArrayList<>());
		}
	}
	
	public void createOntologyClass(String name) {
		createOntologyClass(name, (List<Integer>) null, null);
	}
	
	public void createNewSubClass(OntologyClass parentC, String name) {
		createOntologyClass(name, new ArrayList<>(List.of(getIdentityFromClass(parentC))), null);
	}
	
	@Override
	public void removeOntologyClass(int classIdentity) {
		OntologyClass oc = getClassFromIdentity(classIdentity);
		if (oc == null || oc == getRootOntologyClass()) return;
		
		int targetIndex = getIdentityFromClass(oc);
		
		OntologyMemento memento = new OntologyMemento();
		memento.actionType = OntologyMemento.ActionType.Remove;
		memento.primaryTargetId = targetIndex;
		memento.classSnapshot = oc;
		memento.fileListSnapshot = new ArrayList<>(filesPerOntology.get(targetIndex));
		memento.parentSnapshot = new ArrayList<>(oc.parents);
		memento.childSnapshot = new ArrayList<>(oc.children);
		
		for (OntologyClass parent : new ArrayList<>(oc.parents)) {
			oc.removeParent(parent);
		}
		for (OntologyClass child : new ArrayList<>(oc.children)) {
			child.removeParent(oc);
		}
		getRootOntologyClass().children.remove(oc); oc.parents.clear(); // "removeParent" calls leave the root as parent.
		
		ontologyClasses.set(targetIndex, null);
		filesPerOntology.get(targetIndex).forEach(fi -> {
			if (fi.tagsByIdentity != null) {
				fi.tagsByIdentity.remove(Integer.valueOf(targetIndex));
				if (fi.tagsByIdentity.isEmpty()) {
					fi.tagsByIdentity.add(0); // Anchor to Root
					filesPerOntology.get(0).add(fi);
				}
			}
		});
		filesPerOntology.set(targetIndex, null);
		filesPerOntology.set(targetIndex, null);
		
		recordAction(memento);
	}
	
	@Override
	public void restoreOntologyClass(int identity, OntologyClass snapshot, ArrayList<FileInterface> fileSnap) {
		ontologyClasses.set(identity, snapshot);
		filesPerOntology.set(identity, fileSnap != null ? new ArrayList<>(fileSnap) : new ArrayList<>());
	}
	
	@Override
	public void copyContentsTo(int sourceIdentity, int targetIdentity, boolean copyParents, boolean copyChildren, boolean copyFiles) {
		OntologyClass source = getClassFromIdentity(sourceIdentity);
		OntologyClass target = getClassFromIdentity(targetIdentity);
		
		if (source == null || target == null || source == target) return;
		
		if (copyParents) {
			for (OntologyClass parent : new ArrayList<>(source.parents)) {
				if (parent != getRootOntologyClass()) {
					addParent(targetIdentity, getIdentityFromClass(parent));
				}
			}
		}
		
		if (copyChildren) {
			for (OntologyClass child : new ArrayList<>(source.children)) {
				addParent(getIdentityFromClass(child), targetIdentity);
			}
		}
		
		if (copyFiles) {
			List<FileInterface> sourceFiles = new ArrayList<>(filesPerOntology.get(sourceIdentity));
			for (FileInterface file : sourceFiles) {
				addElement(targetIdentity, file);
			}
		}
	}
	
	@Override
	public void addParent(int childId, int parentId) {
		OntologyClass child = getClassFromIdentity(childId);
		OntologyClass parent = getClassFromIdentity(parentId);
		if (child != null && parent != null) {
			child.addParent(parent);
			
			OntologyMemento memento = new OntologyMemento();
			memento.actionType = OntologyMemento.ActionType.AddParent;
			memento.primaryTargetId = childId;
			memento.secondaryTargetId = parentId;
			recordAction(memento);
		}
	}
	
	@Override
	public void removeParent(int childId, int parentId) {
		OntologyClass child = getClassFromIdentity(childId);
		OntologyClass parent = getClassFromIdentity(parentId);
		if (child != null && parent != null) {
			child.removeParent(parent);
			
			OntologyMemento memento = new OntologyMemento();
			memento.actionType = OntologyMemento.ActionType.RemoveParent;
			memento.primaryTargetId = childId;
			memento.secondaryTargetId = parentId;
			recordAction(memento);
		}
	}
	
	@Override
	public void addElement(int classIdentity, FileInterface file) {
		OntologyClass targetClass = getClassFromIdentity(classIdentity);
		if (targetClass == null) return;
		
		ArrayList<FileInterface> fileList = filesPerOntology.get(getIdentityFromClass(targetClass));
		if (!fileList.contains(file)) {
			fileList.add(file);
			
			if (file.tagsByIdentity == null) {
				file.tagsByIdentity = new ArrayList<>();
			}
			file.tagsByIdentity.add(getIdentityFromClass(targetClass));
			
			OntologyMemento memento = new OntologyMemento();
			memento.actionType = OntologyMemento.ActionType.AddElement;
			memento.primaryTargetId = classIdentity;
			memento.targetFile = file;
			recordAction(memento);
		}
	}
	
	@Override
	public void removeElement(int classIdentity, FileInterface file) {
		OntologyClass targetClass = getClassFromIdentity(classIdentity);
		if (targetClass == null) return;
		
		int classId = getIdentityFromClass(targetClass);
		ArrayList<FileInterface> fileList = filesPerOntology.get(classId);
		if (fileList.remove(file)) {
			if (file.tagsByIdentity != null) {
				file.tagsByIdentity.remove(Integer.valueOf(classId));
			}
			
			OntologyMemento memento = new OntologyMemento();
			memento.actionType = OntologyMemento.ActionType.RemoveElement;
			memento.primaryTargetId = classIdentity;
			memento.targetFile = file;
			recordAction(memento);
		}
	}
	
	@Override
	public void renameOntologyClass(int classIdentity, String newName) {
		OntologyClass targetClass = getClassFromIdentity(classIdentity);
		if (targetClass != null && targetClass != getRootOntologyClass()) {
			String oldName = targetClass.name;
			targetClass.name = newName;
			
			OntologyMemento memento = new OntologyMemento();
			memento.actionType = OntologyMemento.ActionType.Rename;
			memento.primaryTargetId = classIdentity;
			memento.stringPayload = oldName;
			recordAction(memento);
		}
	}
	
	@Override
	public void renameElementActualName(FileInterface file, String newName) {
		if (file != null && newName != null && !newName.trim().isEmpty()) {
			String oldName = file.actualName;
			file.actualName = newName.trim();
			
			OntologyMemento memento = new OntologyMemento();
			memento.actionType = OntologyMemento.ActionType.RenameElementActualName;
			memento.targetFile = file;
			memento.stringPayload = oldName;
			recordAction(memento);
		}
	}
	
	@Override
	public void undo() {
		if (undoStack.isEmpty()) return;
		isReplaying = true;
		try {
			OntologyMemento memento = undoStack.pop();
			memento.undo(this, this);
			redoStack.push(memento);
			if (redoStack.size() > stackSize / 3) redoStack.removeElementAt(0);
		} finally {
			isReplaying = false;
		}
	}
	
	@Override
	public void redo() {
		if (redoStack.isEmpty()) return;
		isReplaying = true;
		try {
			OntologyMemento memento = redoStack.pop();
			memento.undo(this, this);
			undoStack.push(memento);
			if (undoStack.size() > stackSize) undoStack.removeElementAt(0);
		} finally {
			isReplaying = false;
		}
	}
	
	@Override
	public int createOntologyClassRaw(String name, List<Integer> parentIds, List<Integer> childrenIds) {
		OntologyClass noc = new OntologyClass(name, getRootOntologyClass());
		if (parentIds != null) {
			for (Integer pId : parentIds) {
				OntologyClass parent = getClassFromIdentity(pId);
				if (parent != null) noc.addParent(parent);
				else
					throw new OntoDirectoryException("One element in the parents array of '" + name + "' was null");
			}
		}
		if (childrenIds != null) {
			for (Integer cId : childrenIds) {
				OntologyClass child = getClassFromIdentity(cId);
				if (child != null) child.addParent(noc);
				else
					throw new OntoDirectoryException("One element in the children array of '" + name + "' was null");
			}
		}
		
		insertNewClass(noc);
		
		return noc.identityNumber;
	}
	
	@Override
	public void addElementRaw(int classIdentity, FileInterface file) {
		OntologyClass targetClass = getClassFromIdentity(classIdentity);
		if (targetClass == null) return;
		
		ArrayList<FileInterface> fileList = filesPerOntology.get(getIdentityFromClass(targetClass));
		if (!fileList.contains(file)) {
			fileList.add(file);
			
			if (file.tagsByIdentity == null) {
				file.tagsByIdentity = new ArrayList<>();
			}
			file.tagsByIdentity.add(getIdentityFromClass(targetClass));
			
		}
	}
	
	@Override
	public void deleteElements(List<FileInterface> selectedFiles) {
		selectedFiles.forEach(sf -> {
			sf.tagsByIdentity.forEach(tg -> {
				filesPerOntology.get(tg).remove(sf);
			});
		});
		for(int i  = 0; i < fileInterfaces.size(); i++) {
			for(int ii = 0; ii < selectedFiles.size(); ii++) {
				if(fileInterfaces.get(i) != selectedFiles.get(ii)) continue;
				fileInterfaces.set(i, null);
				selectedFiles.remove(ii);
			}
		}
	}
	
}

}