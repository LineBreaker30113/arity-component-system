package org.halim.test;

import org.halim.legacy.dlake.FileInterface;
import org.halim.legacy.dlake.OntologyStorageV0;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FileInterfaceBoundaryTest extends OntoDirectoryTestBase {

@ParameterizedTest
@ValueSource(strings = {
	  "", // Empty String Boundary
	  "türkçe_karakterler_çğöşü.docx", // Complex UTF-8 Length Mismatch Boundary
	  "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ012345678912345678", // Exactly 128 Bytes
	  "This_String_Is_Intentionally_Much_Longer_Than_One_Hundred_And_Twenty_Eight_Bytes_To_Force_The_Binary_Truncation_Logic_To_Slice_It_Without_Crashing_The_Stream.pdf" // Overflow Boundary
})
public void testStorage_ByteBoundaries_MaintainsBinaryAlignment(String testName) throws Exception {
	OntologyStorageV0 storage = new OntologyStorageV0();
	Path elementsFile = testSandboxRoot.resolve("boundary_identities.bin");
	
	FileInterface fi = new FileInterface();
	fi.identity = 0;
	fi.diskName = "0xBOUND";
	fi.actualName = testName;
	fi.tagsByIdentity = new ArrayList<>(List.of(0));
	
	ArrayList<FileInterface> list = new ArrayList<>();
	list.add(fi);
	
	// Save payload
	storage.saveOntologyElements(elementsFile, list);
	
	// Reload payload
	ArrayList<FileInterface> hydrated = storage.loadOntologyElementsFromFile(elementsFile);
	
	FileInterface result = hydrated.get(0);
	
	// Assert 128-byte limit truncation
	if (testName.getBytes().length > 128) {
		assertThat(result.actualName.getBytes().length).isLessThanOrEqualTo(128);
	} else {
		// Because we pad with zero bytes but String trim() removes whitespaces, we just check contains.
		// Using UTF-8 ensures multibyte characters aren't scrambled.
		assertThat(result.actualName).isEqualTo(testName);
	}
	assertThat(result.diskName).isEqualTo("0xBOUND");
}
}