package org.halim.nStructure.sgui.sglib;

import org.halim.nStructure.mhexagon.OntoDirectoryService;
import org.halim.nStructure.mhexagon.OntologyClassHandle;

public interface WorkspaceListener {

	void onSelectedLakeChange(OntoDirectoryService.DataLakeService dataLakeService);

	void onSelectedClassChange(OntologyClassHandle ontologyClass);

	void onDomainChange(OntologyClassHandle newDomain);

}
