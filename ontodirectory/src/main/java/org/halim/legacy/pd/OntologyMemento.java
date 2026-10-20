package org.halim.legacy.pd;

import org.halim.legacy.dlake.FileInterface;
import org.halim.legacy.dlake.OntologyClass;

import java.util.ArrayList;

public class OntologyMemento {

public enum ActionType {
	Create, Remove, AddParent, RemoveParent, AddElement, RemoveElement, Rename, RenameElementActualName
}

public ActionType actionType;
public int primaryTargetId;
public int secondaryTargetId;
public String stringPayload;
public FileInterface targetFile;
public OntologyClass classSnapshot;
public ArrayList<FileInterface> fileListSnapshot;

// CRITICAL FIX: Edge Preservation for topological resurrection
public ArrayList<OntologyClass> parentSnapshot;
public ArrayList<OntologyClass> childSnapshot;

public void undo(OntologyReadingService.OntologyManagingService oms, OntologyReadingService ors) {
	switch (actionType) {
		case Create:
			classSnapshot = ors.getClassFromIdentity(primaryTargetId);
			fileListSnapshot = new ArrayList<>(ors.getOntologyElements(primaryTargetId));
			
			// Capture topology before tearing it down for the Redo phase
			parentSnapshot = new ArrayList<>(classSnapshot.parents);
			childSnapshot = new ArrayList<>(classSnapshot.children);
			
			oms.removeOntologyClass(primaryTargetId);
			actionType = ActionType.Remove;
			break;
		
		case Remove:
			// 1. Restore exact memory footprint to the array
			oms.restoreOntologyClass(primaryTargetId, classSnapshot, fileListSnapshot);
			
			// 2. Mathematically re-stitch the DAG edges
			if (parentSnapshot != null) {
				for (OntologyClass p : parentSnapshot) {
					classSnapshot.addParent(p);
				}
			}
			if (childSnapshot != null) {
				for (OntologyClass c : childSnapshot) {
					c.addParent(classSnapshot);
					// If the child was orphaned during deletion, it fell back to Root.
					// Now that its true parent is back, sever the temporary Root anchor.
					OntologyClass root = ors.getRootOntologyClass();
					if (c.parents.contains(root) && c.parents.size() > 1) {
						c.removeParent(root);
					}
				}
			}
			actionType = ActionType.Create;
			break;
		
		case AddParent:
			oms.removeParent(primaryTargetId, secondaryTargetId);
			actionType = ActionType.RemoveParent;
			break;
		
		case RemoveParent:
			oms.addParent(primaryTargetId, secondaryTargetId);
			actionType = ActionType.AddParent;
			break;
		
		case AddElement:
			oms.removeElement(primaryTargetId, targetFile);
			actionType = ActionType.RemoveElement;
			break;
		
		case RemoveElement:
			oms.addElement(primaryTargetId, targetFile);
			actionType = ActionType.AddElement;
			break;
		
		case Rename:
			String currentName = ors.getClassFromIdentity(primaryTargetId).name;
			oms.renameOntologyClass(primaryTargetId, stringPayload);
			stringPayload = currentName;
			break;
		
		case RenameElementActualName:
			String currActual = targetFile.actualName;
			targetFile.actualName = stringPayload;
			stringPayload = currActual;
			break;
	}
}
}