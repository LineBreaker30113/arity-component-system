package org.halim.test;

import org.halim.nStructure.ontology.core.OntologyEngine;
import org.halim.nStructure.ontology.view.implementations.DefaultOntologyService;
import org.halim.nStructure.ontology.view.OntologyService;
import org.halim.nStructure.ontology.view.command.OntologyClassificationManagementService;
import org.halim.nStructure.ontology.view.command.OntologyHierarchyManagementService;
import org.halim.nStructure.ontology.view.implementations.persistence.OntologyRestoreSession;
import org.halim.nStructure.ontology.view.implementations.persistence.OntologyStorageException;
import org.halim.nStructure.ontology.view.implementations.persistence.OntologyStorageSession;
import org.halim.nStructure.ontology.view.query.OntologyDataQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Coverage for {@link OntologyStorageSession}/{@link OntologyRestoreSession}: the write lock is held
 * for the whole session (pausing other mutations), finalize enforces that all four blocks were
 * stored/loaded first, the header is validated on read, and a full store-then-load round trip
 * reconstructs an equivalent engine from scratch.
 */
public class OntologyPersistenceSessionTest {

// 9 magic bytes + 4-byte version + 4-byte content type, written by every OntologyStorageSession block.
private static final int HEADER_SIZE = 9 + 4 + 4;

private OntologyEngine engine;
private OntologyService service;

@BeforeEach
public void setup() {
	engine = new OntologyEngine();
	service = new DefaultOntologyService(engine);
}

private static DataOutputStream nullOut() { return new DataOutputStream(OutputStream.nullOutputStream()); }

private void storeAllFourBlocks(OntologyStorageSession session) {
	session.storeClassCatalog(nullOut());
	session.storeElementCatalog(nullOut());
	session.storeBidirectionalIndexer(nullOut());
	session.storeHierarchy(nullOut());
}

/** Reads a catalog stream back, skipping its header: {@code lastIndex}, then one entry per index
 *  (null = tombstone). */
private static List<String> readCatalog(byte[] data) throws IOException {
	DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
	in.skipBytes(HEADER_SIZE);
	int lastIndex = in.readInt();
	List<String> result = new ArrayList<>();
	for (int i = 0; i <= lastIndex; i++) {
		int len = in.readInt();
		if (len == -1) { result.add(null); continue; }
		byte[] buf = new byte[len];
		in.readFully(buf);
		result.add(new String(buf, StandardCharsets.UTF_8));
	}
	return result;
}

@Test
public void testBeginStoring_HoldsWriteLockUntilFinalized() {
	OntologyStorageSession session = service.beginStoring();

	// Every other mutation must be blocked while the session is open -- verified via timeout rather
	// than actually deadlocking the test.
	assertThatThrownBy(() -> assertTimeoutPreemptively(Duration.ofMillis(300),
		  () -> service.getClassificationManagementService().createElement("blocked.txt")))
		  .isInstanceOf(AssertionFailedError.class);

	storeAllFourBlocks(session);
	session.finalizeStorage();

	// Released now -- this must complete promptly.
	assertTimeoutPreemptively(Duration.ofSeconds(3),
		  () -> service.getClassificationManagementService().createElement("unblocked.txt"));
}

@Test
public void testFinalizeStorage_MissingAComponent_Throws() {
	OntologyStorageSession session = service.beginStoring();
	session.storeClassCatalog(nullOut());
	session.storeElementCatalog(nullOut());
	session.storeBidirectionalIndexer(nullOut());
	// storeHierarchy deliberately omitted

	assertThatThrownBy(session::finalizeStorage).isInstanceOf(IllegalStateException.class);

	session.finalizeStorageForced(); // release the lock so it doesn't leak into other tests
}

@Test
public void testFinalizeStorage_AllFourComponentsStored_Succeeds() {
	OntologyStorageSession session = service.beginStoring();
	storeAllFourBlocks(session);

	session.finalizeStorage(); // must not throw

	assertThat(engine.canUndo()).isFalse(); // storing isn't a user-undoable action
}

@Test
public void testFinalizeStorageForced_ReleasesLockEvenWithPendingComponents() {
	OntologyStorageSession session = service.beginStoring();
	session.storeClassCatalog(nullOut()); // only one of four

	session.finalizeStorageForced(); // bypasses the pending check

	assertTimeoutPreemptively(Duration.ofSeconds(3),
		  () -> service.getClassificationManagementService().createElement("unblocked.txt"));
}

@Test
public void testStoreElementCatalog_TombstoneAndLiveEntriesRoundTrip() throws IOException {
	int e0 = service.getClassificationManagementService().createElement("a.txt");
	int e1 = service.getClassificationManagementService().createElement("b.txt");
	service.getClassificationManagementService().deleteElement(e0); // e0 becomes a tombstone

	ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	OntologyStorageSession session = service.beginStoring();
	session.storeElementCatalog(new DataOutputStream(buffer));
	session.finalizeStorageForced();

	List<String> entries = readCatalog(buffer.toByteArray());
	assertThat(entries).hasSize(e1 + 1);
	assertThat(entries.get(e0)).isNull(); // tombstone
	assertThat(entries.get(e1)).isEqualTo("b.txt");
}

@Test
public void testStoreClassCatalog_RootPlusNamedClassRoundTrip() throws IOException {
	int classA = service.getHierarchyManagementService().createClass("A");

	ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	OntologyStorageSession session = service.beginStoring();
	session.storeClassCatalog(new DataOutputStream(buffer));
	session.finalizeStorageForced();

	List<String> entries = readCatalog(buffer.toByteArray());
	assertThat(entries).hasSize(classA + 1);
	assertThat(entries.get(0)).isEqualTo("File"); // root is named "File" by OntologyEngine's constructor
	assertThat(entries.get(classA)).isEqualTo("A");
}

@Test
public void testStoreBidirectionalIndexer_SetsExpectedBit() throws IOException {
	int classA = service.getHierarchyManagementService().createClass("A"); // id 1 (root is 0)
	int element = service.getClassificationManagementService().createElement("file.txt", classA);

	ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	OntologyStorageSession session = service.beginStoring();
	session.storeBidirectionalIndexer(new DataOutputStream(buffer));
	session.finalizeStorageForced();

	DataInputStream in = new DataInputStream(new ByteArrayInputStream(buffer.toByteArray()));
	in.skipBytes(HEADER_SIZE);
	int lastElementIndex = in.readInt();
	int lastClassIndex = in.readInt();
	assertThat(lastElementIndex).isEqualTo(element);
	assertThat(lastClassIndex).isEqualTo(classA);

	int rowBytes = (lastClassIndex + 8) / 8;
	byte[] row = new byte[rowBytes];
	for (int i = 0; i < element; i++) in.readFully(new byte[rowBytes]); // skip earlier rows
	in.readFully(row);

	boolean bitSet = (row[classA / 8] & (1 << (classA % 8))) != 0;
	assertThat(bitSet).isTrue();
	boolean rootBitSet = (row[0 / 8] & (1 << (0 % 8))) != 0;
	assertThat(rootBitSet).isFalse(); // element is tagged to A directly, not root
}

@Test
public void testBeginLoading_HoldsWriteLockUntilFinalized() {
	OntologyRestoreSession session = service.beginLoading();

	assertThatThrownBy(() -> assertTimeoutPreemptively(Duration.ofMillis(300),
		  () -> service.getClassificationManagementService().createElement("blocked.txt")))
		  .isInstanceOf(AssertionFailedError.class);

	session.finalizeLoadingForced();

	assertTimeoutPreemptively(Duration.ofSeconds(3),
		  () -> service.getClassificationManagementService().createElement("unblocked.txt"));
}

@Test
public void testFinalizeLoading_MissingAComponent_Throws() {
	OntologyRestoreSession session = service.beginLoading();
	assertThatThrownBy(session::finalizeLoading).isInstanceOf(IllegalStateException.class);
	session.finalizeLoadingForced();
}

@Test
public void testLoadElementCatalog_WrongMagicBytes_Throws() {
	ByteArrayOutputStream garbage = new ByteArrayOutputStream();
	OntologyRestoreSession session = service.beginLoading();

	assertThatThrownBy(() -> session.loadElementCatalog(
		  new DataInputStream(new ByteArrayInputStream(garbage.toByteArray()))))
		  .isInstanceOf(OntologyStorageException.class);

	session.finalizeLoadingForced();
}

@Test
public void testLoadElementCatalog_WrongBlockKind_Throws() throws IOException {
	// A validly-headered class-catalog stream fed to loadElementCatalog, which expects element catalog.
	ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	OntologyStorageSession storeSession = service.beginStoring();
	storeSession.storeClassCatalog(new DataOutputStream(buffer));
	storeSession.finalizeStorageForced();

	OntologyRestoreSession loadSession = service.beginLoading();
	assertThatThrownBy(() -> loadSession.loadElementCatalog(new DataInputStream(new ByteArrayInputStream(buffer.toByteArray()))))
		  .isInstanceOf(OntologyStorageException.class);
	loadSession.finalizeLoadingForced();
}

@Test
public void testFullRoundTrip_StoreThenLoad_ReconstructsEquivalentEngine() {
	OntologyHierarchyManagementService hierarchyCmd = service.getHierarchyManagementService();
	OntologyClassificationManagementService classificationCmd = service.getClassificationManagementService();

	// A -> B, A -> C, B -> D, C -> D (diamond: D has multiple parents).
	int a = hierarchyCmd.createClass("A");
	int b = hierarchyCmd.createClass("B", a);
	int c = hierarchyCmd.createClass("C", a);
	int d = hierarchyCmd.createClass("D", b, c);

	int kept = classificationCmd.createElement("keep.txt", b, d);
	int dead = classificationCmd.createElement("gone.txt", a);
	classificationCmd.deleteElement(dead); // becomes a tombstone

	ByteArrayOutputStream elementBuf = new ByteArrayOutputStream();
	ByteArrayOutputStream classBuf = new ByteArrayOutputStream();
	ByteArrayOutputStream bidirectionalBuf = new ByteArrayOutputStream();
	ByteArrayOutputStream hierarchyBuf = new ByteArrayOutputStream();

	OntologyStorageSession storeSession = service.beginStoring();
	storeSession.storeElementCatalog(new DataOutputStream(elementBuf));
	storeSession.storeClassCatalog(new DataOutputStream(classBuf));
	storeSession.storeBidirectionalIndexer(new DataOutputStream(bidirectionalBuf));
	storeSession.storeHierarchy(new DataOutputStream(hierarchyBuf));
	storeSession.finalizeStorage();

	OntologyEngine restoredEngine = new OntologyEngine();
	OntologyService restoredService = new DefaultOntologyService(restoredEngine);

	OntologyRestoreSession loadSession = restoredService.beginLoading();
	loadSession.loadElementCatalog(new DataInputStream(new ByteArrayInputStream(elementBuf.toByteArray())));
	loadSession.loadClassCatalog(new DataInputStream(new ByteArrayInputStream(classBuf.toByteArray())));
	loadSession.loadBidirectionalIndexer(new DataInputStream(new ByteArrayInputStream(bidirectionalBuf.toByteArray())));
	loadSession.loadHierarchy(new DataInputStream(new ByteArrayInputStream(hierarchyBuf.toByteArray())));
	loadSession.finalizeLoading();

	OntologyDataQueryService restoredData = restoredService.getOntologyDataService();
	assertThat(restoredData.getClassName(a)).isEqualTo("A");
	assertThat(restoredData.getClassName(d)).isEqualTo("D");
	assertThat(restoredData.getParents(a).toIntArray()).containsExactly(0);
	assertThat(restoredData.getParents(d).toIntArray()).containsExactlyInAnyOrder(b, c);

	assertThat(restoredData.getElementName(kept)).isEqualTo("keep.txt");
	assertThat(restoredData.getClassesOf(kept).toIntArray()).containsExactlyInAnyOrder(b, d);

	// The tombstoned element must not have been resurrected.
	assertThat(restoredEngine.getIndexerService().isElementTombstoned(dead)).isTrue();
}

}
