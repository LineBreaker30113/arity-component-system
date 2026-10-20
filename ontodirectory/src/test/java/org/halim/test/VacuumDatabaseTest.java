package org.halim.test;

import org.halim.legacy.dlake.FileInterface;
import org.halim.legacy.dlake.OntologyClass;
import org.halim.legacy.dlake.OntologyHierarchyFast;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class VacuumDatabaseTest {

private OntologyHierarchyFast hierarchy;
private Path fakeLakePath;

@BeforeEach
public void setup() {
	hierarchy = new OntologyHierarchyFast();
	hierarchy.ontologyClasses.clear();
	hierarchy.filesPerOntology.clear();
	hierarchy.fileInterfaces.clear();
	fakeLakePath = Paths.get("/test-sandbox/.DATA_LAKE");
}

@Test
public void testVacuum_ShiftsIdentities_AndUpdatesForeignKeys() {
	// Setup Classes [0: Root, 1: Tag1(Dead), 2: Tag2(Dead), 3: Tag3(Alive)]
	OntologyClass root = OntologyClass.makeROOT_ONTOLOGY_CLASS("File");
	OntologyClass tag1 = new OntologyClass("Tag1", root);
	OntologyClass tag2 = new OntologyClass("Tag2", root);
	OntologyClass tag3 = new OntologyClass("Tag3", root);
	
	hierarchy.ontologyClasses.add(root);
	hierarchy.ontologyClasses.add(tag1);
	hierarchy.ontologyClasses.add(tag2);
	hierarchy.ontologyClasses.add(tag3);
	
	for (int i = 0; i < 4; i++) {
		hierarchy.filesPerOntology.add(new ArrayList<>());
		if (hierarchy.ontologyClasses.get(i) != null) {
			hierarchy.ontologyClasses.get(i).identityNumber = i;
		}
	}
	
	// Setup File assigned to Tag 3
	FileInterface fi = new FileInterface();
	fi.identity = 0;
	fi.diskName = "0x000000";
	fi.actualName = "test.txt";
	fi.tagsByIdentity = new ArrayList<>(List.of(3));
	hierarchy.fileInterfaces.add(fi);
	
	// Execute "Deletions" (Tombstoning)
	hierarchy.ontologyClasses.set(1, null);
	hierarchy.ontologyClasses.set(2, null);
	
	// ACT: The Vacuum
	hierarchy.vacuumDatabase(fakeLakePath);
	
	// ASSERT: Array compaction
	assertThat(hierarchy.ontologyClasses).hasSize(2);
	assertThat(hierarchy.ontologyClasses.get(0).name).isEqualTo("File");
	assertThat(hierarchy.ontologyClasses.get(1).name).isEqualTo("Tag3");
	
	// ASSERT: Identity Re-indexing
	assertThat(hierarchy.ontologyClasses.get(1).identityNumber).isEqualTo(1); // Shifted from 3 to 1
	
	// ASSERT: Foreign Key Migration
	assertThat(hierarchy.fileInterfaces).hasSize(1);
	assertThat(hierarchy.fileInterfaces.get(0).tagsByIdentity).containsExactly(1);
}
}