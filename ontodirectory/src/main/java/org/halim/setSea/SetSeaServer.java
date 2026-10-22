package org.halim.setSea;

import org.halim.legacy.Utilities.Pair;
import org.halim.nStructure.archive.core.RecentLakesRegistry;
import org.halim.nStructure.mhexagon.CrashReporter;
import org.halim.nStructure.mhexagon.OntoDirectoryService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * The open Set Seas, one {@link SetSeaManager} each, and which one is focused: {@code ArchiverServer}'s
 * shape over Set Seas. The recent seas are kept in a registry of their own, in {@value #CONFIG_FOLDER}
 * under the app's config folder, so they do not mix with the lakes the ontology package opened.
 * */
public class SetSeaServer implements OntoDirectoryService {

public static final String CONFIG_FOLDER = "set-sea";

final Map<Path, SetSeaManager> activeSeas = new HashMap<>();
final Map<String, Object> guiSettings = new HashMap<>();
final List<OntoDirectoryServiceListener> listeners = new ArrayList<>();
final RecentLakesRegistry recentSeas = new RecentLakesRegistry(RecentLakesRegistry.resolveDefaultConfigDir().resolve(CONFIG_FOLDER));
Path currentlyActiveSeaPath = null;

@Override
public void loadDataLake(String fullPath) {
	Path path = Paths.get(fullPath);
	SetSeaManager sea = activeSeas.computeIfAbsent(path, SetSeaManager::new);
	currentlyActiveSeaPath = path;
	recentSeas.recordOpened(path);
	notifyGuiChange();
	for(OntoDirectoryServiceListener l : listeners) l.onDataLakeLoad(sea);
}

@Override
public List<Path> getRecentDataLakes() { return recentSeas.getRecentLakes(); }

@Override
public void closeLake(DataLakeService dataLakeService) {
	activeSeas.entrySet().removeIf(entry -> entry.getValue() == dataLakeService);
	if(currentlyActiveSeaPath != null && !activeSeas.containsKey(currentlyActiveSeaPath)) currentlyActiveSeaPath = null;
	notifyGuiChange();
}

@Override
public ArrayList<Pair<String, Object>> getGUI_Settings() {
	ArrayList<Pair<String, Object>> result = new ArrayList<>();
	for(Map.Entry<String, Object> entry : guiSettings.entrySet()) result.add(new Pair<>(entry.getKey(), entry.getValue()));
	return result;
}

@Override
public void setGUI_Setting(String key, Object value) {
	guiSettings.put(key, value);
	notifyGuiChange();
}

@Override
public DataLakeService getActiveDataLake() { return currentlyActiveSeaPath == null ? null : activeSeas.get(currentlyActiveSeaPath); }

@Override
public void dispatchLakeChooseRequest(Path identity) {
	if(!activeSeas.containsKey(identity)) return;
	currentlyActiveSeaPath = identity;
	notifyGuiChange();
}

@Override
public void addOntoDirectoryServiceListener(OntoDirectoryServiceListener listener) {
	if(!listeners.contains(listener)) listeners.add(listener);
}

@Override
public void reportFatalError(Throwable ex) {
	SetSeaManager active = (SetSeaManager) getActiveDataLake();
	Path dump = active != null ? active.generateDiagnosticDump(ex) : CrashReporter.generateGlobalDump(ex, Paths.get(System.getProperty("user.dir")));
	for(OntoDirectoryServiceListener l : listeners) l.showBugReport(dump);
}

void notifyGuiChange() {
	for(OntoDirectoryServiceListener l : listeners) l.onGUI_Change();
}

}
