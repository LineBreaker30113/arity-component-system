package org.halim.nStructure.archive.core;

import org.halim.legacy.Utilities.Pair;
import org.halim.nStructure.mhexagon.OntoDirectoryService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Multi-lake orchestrator -- the nStructure-engine port of legacy's {@code OntoDirectoryServer}.
 * Owns one {@link ArchiveManager} per opened lake and tracks which one is currently focused, exactly
 * mirroring legacy's {@code Map<Path, DataLakeManager>} pattern.
 * <p>
 * Opened-lake paths persist across restarts via {@link RecentLakesRegistry}, deliberately kept
 * separate from legacy's {@code SettingLogic.dataLakes} (see that class's javadoc). Ad hoc GUI
 * settings ({@link #guiSettings}) remain in-memory only -- that's a smaller, genuinely separate
 * concern from "remember which lakes were opened."
 */
public class ArchiverServer implements OntoDirectoryService {

private final Map<Path, ArchiveManager> activeLakes = new HashMap<>();
private final Map<String, Object> guiSettings = new HashMap<>();
private Path currentlyActiveLakePath = null;
private final List<OntoDirectoryServiceListener> listeners = new ArrayList<>();
private final RecentLakesRegistry recentLakes = new RecentLakesRegistry(RecentLakesRegistry.resolveDefaultConfigDir());

@Override
public void loadDataLake(String fullPath) {
    Path path = Paths.get(fullPath);
    ArchiveManager lake = activeLakes.computeIfAbsent(path, ArchiveManager::new);
    currentlyActiveLakePath = path;
    recentLakes.recordOpened(path);
    notifyGuiChange();
    for (OntoDirectoryServiceListener l : listeners) l.onDataLakeLoad(lake);
}

@Override
public List<Path> getRecentDataLakes() { return recentLakes.getRecentLakes(); }

@Override
public void closeLake(DataLakeService dataLakeService) {
    activeLakes.entrySet().removeIf(entry -> entry.getValue() == dataLakeService);
    if (currentlyActiveLakePath != null && !activeLakes.containsKey(currentlyActiveLakePath)) {
        currentlyActiveLakePath = null;
    }
    notifyGuiChange();
}

@Override
public ArrayList<Pair<String, Object>> getGUI_Settings() {
    ArrayList<Pair<String, Object>> result = new ArrayList<>();
    for (Map.Entry<String, Object> entry : guiSettings.entrySet()) {
        result.add(new Pair<>(entry.getKey(), entry.getValue()));
    }
    return result;
}

@Override
public void setGUI_Setting(String key, Object value) {
    guiSettings.put(key, value);
    notifyGuiChange();
}

@Override
public DataLakeService getActiveDataLake() {
    return currentlyActiveLakePath == null ? null : activeLakes.get(currentlyActiveLakePath);
}

@Override
public void dispatchLakeChooseRequest(Path identity) {
    if (!activeLakes.containsKey(identity)) return;
    currentlyActiveLakePath = identity;
    notifyGuiChange();
}

@Override
public void addOntoDirectoryServiceListener(OntoDirectoryServiceListener listener) {
    if (!listeners.contains(listener)) listeners.add(listener);
}

@Override
public void reportFatalError(Throwable ex) {
    ArchiveManager active = (ArchiveManager) getActiveDataLake();
    Path dump = active != null
          ? active.generateDiagnosticDump(ex)
          : org.halim.nStructure.mhexagon.CrashReporter.generateGlobalDump(ex, Paths.get(System.getProperty("user.dir")));
    for (OntoDirectoryServiceListener l : listeners) l.showBugReport(dump);
}

private void notifyGuiChange() {
    for (OntoDirectoryServiceListener l : listeners) l.onGUI_Change();
}

}
