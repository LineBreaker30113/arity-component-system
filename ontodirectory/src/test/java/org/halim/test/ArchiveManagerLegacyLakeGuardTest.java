package org.halim.test;

import org.halim.legacy.dlake.DataLakeManager;
import org.halim.nStructure.archive.core.ArchiveManager;
import org.halim.nStructure.archive.core.LakeFormatMismatchException;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ArchiveManagerLegacyLakeGuardTest extends OntoDirectoryTestBase {

@Test
public void testConstructor_RejectsDirectoryHoldingALegacyFormatLake() throws Exception {
    Path lakeRoot = testSandboxRoot.resolve("MixedUpLake");
    Files.createDirectories(lakeRoot);
    Files.createFile(lakeRoot.resolve(ArchiveManager.CONFIG_FILE_SUFFIX));      // shared filename
    Files.createFile(lakeRoot.resolve(DataLakeManager.hierarchyPathSuffix));    // legacy-only marker

    assertThatThrownBy(() -> new ArchiveManager(lakeRoot))
          .isInstanceOf(LakeFormatMismatchException.class)
          .hasMessageContaining(DataLakeManager.hierarchyPathSuffix);
}
}
