package org.halim.legacy.pd;

import org.halim.legacy.Utilities.Pair;
import org.halim.legacy.dlake.FileInterface;
import org.halim.legacy.dlake.OntologyFilter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

public interface OntoDirectoryService {

void loadDataLake(String fullPath);
void closeLake(DataLakeService dataLakeService);
ArrayList<Pair<String, Object>> getGUI_Settings();
void setGUI_Setting(String key, Object value);

DataLakeService getActiveDataLake();
void dispatchLakeChooseRequest(Path identity);

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
	void exportFiles(OntologyFilter filter);
	void exportFiles(OntologyFilter filter, Path destinationFolder);
	void exportSpecificFiles(java.util.List<FileInterface> files, Path destinationFolder);
	
	void logActivity(String action);
	Path generateDiagnosticDump(Throwable ex);
}
}