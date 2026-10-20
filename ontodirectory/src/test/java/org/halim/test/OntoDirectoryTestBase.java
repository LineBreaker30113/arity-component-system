package org.halim.test;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class OntoDirectoryTestBase {

protected FileSystem virtualFS;
protected Path testSandboxRoot;

@BeforeEach
public void initializeTestSandbox() throws Exception {
	virtualFS = Jimfs.newFileSystem(Configuration.unix());
	testSandboxRoot = virtualFS.getPath("/test-sandbox");
	Files.createDirectories(testSandboxRoot);
	
	System.setProperty("onto.dev.dir", testSandboxRoot.toString());
}

@AfterEach
public void teardownTestSandbox() throws Exception {
	System.clearProperty("onto.dev.dir");
	virtualFS.close();
}
}