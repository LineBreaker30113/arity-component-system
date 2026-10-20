package org.halim.nStructure.archive.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Persists which Data Lake paths have been opened in nStructure, across restarts — deliberately
 * its own file, never {@code org.halim.legacy.SettingLogic.dataLakes}. The two engines can produce
 * lakes that look identical from the config-file's name alone ({@code .odConfig.bin} is shared), so
 * keeping the "recent" histories separate avoids ever surfacing one engine's lake path in the
 * other's list — on top of {@code ArchiveManager}'s own format guard.
 * <p>
 * Storage is one absolute path per line, plain UTF-8 text — proportionate to "a list of paths," not
 * a database. A missing or corrupt file is treated as an empty history rather than a boot failure;
 * a *write* failure, however, throws {@link UncheckedIOException}, matching every other persistence
 * path in this codebase ({@code ArchiveManager#saveChanges}, legacy's {@code DataLakeManager}).
 */
public class RecentLakesRegistry {

public static final String REGISTRY_FILE_NAME = "nstructure-recent-lakes.txt";

private final Path registryFile;
private final LinkedHashSet<Path> recentLakes = new LinkedHashSet<>();

public RecentLakesRegistry(Path configDir) {
    this.registryFile = configDir.resolve(REGISTRY_FILE_NAME);
    load();
}

/** Mirrors legacy's {@code SettingLogic.ACTIVE_CONFIG_DIR} resolution (package-private there,
 *  unreachable from here) -- same {@code -Donto.dev.dir} override, recomputed independently. */
public static Path resolveDefaultConfigDir() {
    String override = System.getProperty("onto.dev.dir");
    if (override != null) return Path.of(override);
    return Path.of(System.getProperty("user.home"), ".config", "onto-directory");
}

public List<Path> getRecentLakes() { return List.copyOf(recentLakes); }

/** Records a lake as opened, bumping it to most-recent if already present, and persists immediately. */
public void recordOpened(Path lakePath) {
    recentLakes.remove(lakePath);
    recentLakes.add(lakePath);
    persist();
}

private void load() {
    if (!Files.exists(registryFile)) return;
    try {
        for (String line : Files.readAllLines(registryFile, StandardCharsets.UTF_8)) {
            if (!line.isBlank()) recentLakes.add(registryFile.getFileSystem().getPath(line.trim()));
        }
    } catch (IOException e) {
        System.err.println("Failed to read recent Data Lake list, starting empty: " + e.getMessage());
        recentLakes.clear();
    }
}

private void persist() {
    try {
        Files.createDirectories(registryFile.getParent());
        Files.write(registryFile, recentLakes.stream().map(Path::toString).toList(), StandardCharsets.UTF_8);
    } catch (IOException e) {
        throw new UncheckedIOException("Failed to persist recent Data Lake list: " + e.getMessage(), e);
    }
}
}
