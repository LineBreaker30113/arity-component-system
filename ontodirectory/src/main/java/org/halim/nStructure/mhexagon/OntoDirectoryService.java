package org.halim.nStructure.mhexagon;

import org.halim.legacy.Utilities.Pair;
import org.halim.nStructure.ontology.core.segmentation.query.OntologyElementFilter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public interface OntoDirectoryService {

void loadDataLake(String fullPath);
void closeLake(DataLakeService dataLakeService);
ArrayList<Pair<String, Object>> getGUI_Settings();
void setGUI_Setting(String key, Object value);

DataLakeService getActiveDataLake();
void dispatchLakeChooseRequest(Path identity);

List<Path> getRecentDataLakes();

void addOntoDirectoryServiceListener(OntoDirectoryServiceListener listener);

void reportFatalError(Throwable ex);

interface OntoDirectoryServiceListener {
	void onGUI_Change();
	void onDataLakeLoad(DataLakeService dataLakeService);
	void showBugReport(Path reportFile);
}

interface DataLakeService {
	void saveChanges();
	OntologyReadingService getOntologyReadingService();
	OntologyReadingService.OntologyManagingService getOntologyManagingService();
	Path getRootPath();

	void executeVacuum();

	void addDataLakeServiceListener(DataLakeServiceListener listener);

	interface DataLakeServiceListener {
		void onChange();

	}

	void importFiles();
	void importFiles(Path sourceDirectory) throws IOException;
	void exportFiles(OntologyElementFilter filter);
	void exportFiles(OntologyElementFilter filter, Path destinationFolder);
	void exportSpecificFiles(List<OntologyElementHandle> elements, Path destinationFolder);

	void logActivity(String action);
	Path generateDiagnosticDump(Throwable ex);
}
}
