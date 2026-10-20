package org.halim.test;

import org.halim.legacy.dlake.DataLakeManager;
import org.halim.legacy.dlake.FileInterface;
import org.halim.legacy.dlake.OntologyStorageV0;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class DataLakeIntegrationTest extends OntoDirectoryTestBase {

@Test
public void testLakeCreation_GeneratesDirectoryStructure() throws Exception {
	Path lakeRoot = testSandboxRoot.resolve("NewLake");
	DataLakeManager manager = new DataLakeManager(lakeRoot, new OntologyStorageV0());
	
	assertThat(Files.exists(manager.getLakePath())).isTrue();
	assertThat(Files.exists(manager.getLakeImports())).isTrue();
	assertThat(Files.exists(manager.getLakeExports())).isTrue();
	assertThat(Files.exists(manager.getBugReportsDir())).isTrue();
	assertThat(Files.exists(manager.getLakeConfig())).isTrue();
}

@Test
public void testImport_TemporalTagGeneration_MovesAndRenames() throws Exception {
	Path lakeRoot = testSandboxRoot.resolve("IngestionLake");
	DataLakeManager manager = new DataLakeManager(lakeRoot, new OntologyStorageV0());
	
	// Drop dummy files into /imports
	Path importsDir = manager.getLakeImports();
	Files.writeString(importsDir.resolve("doc1.txt"), "data1");
	Files.writeString(importsDir.resolve("doc2.txt"), "data2");
	
	// Act
	manager.importFiles();
	
	// 1. Assert physical files moved out of imports
	assertThat(Files.list(importsDir).count()).isEqualTo(0);
	
	// 2. Assert files were registered in RAM
	long ingestedFileCount = manager.ontologyHierarchy.fileInterfaces.size();
	assertThat(ingestedFileCount).isEqualTo(2);
	
	// 3. Assert temporal tag logic dynamically assigned
	FileInterface importedFile = manager.ontologyHierarchy.fileInterfaces.get(0);
	boolean foundTemporalTag = false;
	
	for (int id : importedFile.tagsByIdentity) {
		String tagName = manager.ontologyHierarchy.getClassFromIdentity(id).name;
		if (tagName.startsWith("imported_at_")) {
			foundTemporalTag = true;
			break;
		}
	}
	
	assertThat(foundTemporalTag)
		  .withFailMessage("No temporal tag starting with 'imported_at_' was assigned to the file.")
		  .isTrue();
}

@Test
public void testAlphaAndOmegaLifecycle_AbsoluteParity() throws Exception {
	Path lakeRoot = testSandboxRoot.resolve("LifecycleLake");
	
	// 1. Boot Lake
	DataLakeManager m1 = new DataLakeManager(lakeRoot, new OntologyStorageV0());
	
	// 2. Wire Graph
	int tagA = m1.getOntologyManagingService().createOntologyClass("CategoryA", null, null);
	int tagB = m1.getOntologyManagingService().createOntologyClass("CategoryB", null, null);
	m1.getOntologyManagingService().addParent(tagB, tagA); // A -> B
	
	// 3. Delete/Tombstone
	m1.getOntologyManagingService().removeOntologyClass(tagA);
	
	// 4. Vacuum & Save
	m1.executeVacuum(); // Defragments tagB down to ID 1
	
	// 5. Reload from Disk into new RAM space
	DataLakeManager m2 = new DataLakeManager(lakeRoot, new OntologyStorageV0());
	
	// Assert State Parity
	assertThat(m2.ontologyHierarchy.ontologyClasses).hasSize(2); // Root + CategoryB
	assertThat(m2.ontologyHierarchy.getClassFromIdentity(1).name).isEqualTo("CategoryB");
}
}