package org.halim.test;

import org.halim.nStructure.archive.core.RecentLakesRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class RecentLakesRegistryTest extends OntoDirectoryTestBase {

@Test
public void testRecordOpened_PersistsAndReloadsAcrossInstances() {
    Path lakeA = testSandboxRoot.resolve("LakeA");
    Path lakeB = testSandboxRoot.resolve("LakeB");

    new RecentLakesRegistry(testSandboxRoot).recordOpened(lakeA);
    RecentLakesRegistry second = new RecentLakesRegistry(testSandboxRoot);
    second.recordOpened(lakeB);

    RecentLakesRegistry reloaded = new RecentLakesRegistry(testSandboxRoot);
    assertThat(reloaded.getRecentLakes()).containsExactly(lakeA, lakeB);
}

@Test
public void testRecordOpened_ReopeningExistingLake_DoesNotDuplicateButBumpsOrder() {
    Path lakeA = testSandboxRoot.resolve("LakeA");
    Path lakeB = testSandboxRoot.resolve("LakeB");
    RecentLakesRegistry registry = new RecentLakesRegistry(testSandboxRoot);

    registry.recordOpened(lakeA);
    registry.recordOpened(lakeB);
    registry.recordOpened(lakeA); // re-open A

    assertThat(registry.getRecentLakes()).containsExactly(lakeB, lakeA);
}
}
