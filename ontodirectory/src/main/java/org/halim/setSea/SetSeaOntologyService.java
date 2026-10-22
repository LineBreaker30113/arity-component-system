package org.halim.setSea;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.halim.nStructure.ontology.core.EngineLockHandle;
import org.halim.nStructure.ontology.core.segmentation.query.OntologyFilteringServiceSegmentation;
import org.halim.nStructure.ontology.view.OntologyService;
import org.halim.nStructure.ontology.view.command.OntologyClassificationManagementService;
import org.halim.nStructure.ontology.view.command.OntologyHierarchyManagementService;
import org.halim.nStructure.ontology.view.implementations.persistence.OntologyRestoreSession;
import org.halim.nStructure.ontology.view.implementations.persistence.OntologyStorageSession;
import org.halim.nStructure.ontology.view.query.OntologyAnalysisService;
import org.halim.nStructure.ontology.view.query.OntologyDataQueryService;


/**
 * The read side the GUI's handles ask, over a {@link SetSeaDatabase}: the data and the analytics a
 * class handle and an element handle use. Edits go through {@link SetSeaManager}, so the management,
 * filtering, locking and storage services of the ontology package are refused here, as are the
 * uniqueness analyses no screen calls.
 * <p>
 * Two answers follow the ontology package rather than the database: a file holding no tag lists the root
 * as its one class, since every file inherits it, and a class is its own ancestor and its own heritage.
 * */
public class SetSeaOntologyService implements OntologyService {

final SetSeaDatabase database;
final OntologyDataQueryService data = new Data();
final OntologyAnalysisService analytics = new Analytics();

public SetSeaOntologyService(SetSeaDatabase database) { this.database = database; }

@Override public OntologyDataQueryService getOntologyDataService() { return data; }
@Override public OntologyAnalysisService getAnalyticsService() { return analytics; }

@Override public OntologyFilteringServiceSegmentation getFilteringService() { throw refused("filtering"); }
@Override public OntologyClassificationManagementService getClassificationManagementService() { throw refused("classification management, which SetSeaManager does"); }
@Override public OntologyHierarchyManagementService getHierarchyManagementService() { throw refused("hierarchy management, which SetSeaManager does"); }
@Override public EngineLockHandle beginExclusiveSession() { throw refused("an exclusive session"); }
@Override public OntologyStorageSession beginStoring() { throw refused("the ontology package's storage, since ACSE stores it"); }
@Override public OntologyRestoreSession beginLoading() { throw refused("the ontology package's storage, since ACSE loads it"); }

@Override public boolean canUndo() { return database.canUndo(); }
@Override public boolean canRedo() { return database.canRedo(); }
@Override public void undo() { database.undo(); }
@Override public void redo() { database.redo(); }

static UnsupportedOperationException refused(String what) { return new UnsupportedOperationException("a Set Sea has no " + what); }


class Data implements OntologyDataQueryService {

	@Override public String getClassName(int classIdentity) { return database.tagName(classIdentity); }
	@Override public IntArrayList getParents(int classIdentity) { return IntArrayList.wrap(database.parentsOf(classIdentity)); }
	@Override public IntArrayList getChildren(int classIdentity) { return IntArrayList.wrap(database.childrenOf(classIdentity)); }
	@Override public int getParentCount(int classIdentity) { return database.parentsOf(classIdentity).length; }
	@Override public int getChildrenCount(int classIdentity) { return database.childrenOf(classIdentity).length; }
	@Override public IntArrayList getElementsOf(int classIdentity) { return IntArrayList.wrap(database.directFilesOf(classIdentity)); }
	@Override public int getElementsCount(int classIdentity) { return database.directFilesOf(classIdentity).length; }
	@Override public String getElementName(int elementIdentity) { return database.fileName(elementIdentity); }

	@Override
	public IntArrayList getClassesOf(int elementIdentity) {
		int[] held = database.tagsOf(elementIdentity);
		return held.length == 0 ? IntArrayList.of(SetSeaDatabase.ROOT) : IntArrayList.wrap(held);
	}

	@Override public int getClassesCount(int elementIdentity) { return getClassesOf(elementIdentity).size(); }

	/** As the ontology package has it: among the file's tags the one with the most ancestors, then up through each first parent to the root, given root first. */
	@Override
	public int[] getMostDescriptivePath(int elementIdentity) {
		int deepest = SetSeaDatabase.ROOT, depth = -1;
		for(int tag : database.tagsOf(elementIdentity)) {
			int ancestors = database.ancestorsOf(tag).length;
			if(ancestors > depth) { deepest = tag; depth = ancestors; }
		}
		IntArrayList path = new IntArrayList();
		for(int at = deepest; ; ) {
			path.add(0, at);
			int[] parents = database.parentsOf(at);
			if(parents.length == 0) { break; }
			at = parents[0];
		}
		return path.toIntArray();
	}

	@Override public boolean isConnected(int classIdentity, int elementIdentity) { return database.holds(classIdentity, elementIdentity); }
	@Override public int getLastElementIndex() { return database.lastFile(); }
	@Override public boolean isElementTombstoned(int elementIdentity) { return !database.isFile(elementIdentity); }
	@Override public int getLastClassIndex() { return database.lastTag(); }
	@Override public boolean isClassTombstoned(int classIdentity) { return !database.isTag(classIdentity); }

}


class Analytics implements OntologyAnalysisService {

	@Override public IntArrayList getHeritage(int classIdentity) { return IntArrayList.wrap(database.descendantsOf(classIdentity)); }
	@Override public IntArrayList getAncestors(int classIdentity) { return IntArrayList.wrap(database.ancestorsOf(classIdentity)); }
	/** Whether {@code doubtedIdentity} is {@code classIdentity} or above it. */
	@Override public boolean isAncestor(int classIdentity, int doubtedIdentity) { return database.reaches(doubtedIdentity, classIdentity); }
	/** Whether {@code doubtedIdentity} is {@code classIdentity} or below it. */
	@Override public boolean isHeritage(int classIdentity, int doubtedIdentity) { return database.reaches(classIdentity, doubtedIdentity); }
	@Override public IntArrayList getDomainElements(int classIdentity) { return IntArrayList.wrap(database.domainFilesOf(classIdentity)); }
	@Override public int getDomainElementCount(int classIdentity) { return database.domainFilesOf(classIdentity).length; }

	@Override public int getUniquestChild(int classIdentity) { throw refused("uniqueness analysis"); }
	@Override public int[] getChildrenByUniqueness(int classIdentity) { throw refused("uniqueness analysis"); }
	@Override public int getUniquestParent(int classIdentity) { throw refused("uniqueness analysis"); }
	@Override public int[] getParentsByUniqueness(int classIdentity) { throw refused("uniqueness analysis"); }
	@Override public boolean isUniquenessCalculationDirectionUpward() { throw refused("uniqueness analysis"); }
	@Override public void setUniquenessCalculationDirectionUpward(boolean upward) { throw refused("uniqueness analysis"); }
	@Override public int[] getUniquestPath(int classIdentity) { throw refused("uniqueness analysis"); }
	@Override public int[] getDeepestPath(int classIdentity) { throw refused("depth analysis"); }

}

}
