package org.halim.nStructure.sgui.sglib;

import org.halim.nStructure.mhexagon.OntoDirectoryService;
import org.halim.nStructure.mhexagon.OntologyReadingService;


/** Views that display or edit the overall structure (Tree, Venn, Graph) */
public abstract class HierarchyView extends WorkSpaceViewPanel {


public OntologyReadingService hierarchy;


protected abstract void clearHierarchy();


@Override
public void onSelectedLakeChange(OntoDirectoryService.DataLakeService dataLakeService) {
	clearHierarchy();
	this.hierarchy = dataLakeService.getOntologyReadingService();
	Utilities.repaintJFrame(this);
}

}