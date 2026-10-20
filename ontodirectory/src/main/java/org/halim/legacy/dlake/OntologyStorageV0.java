package org.halim.legacy.dlake;

import org.halim.legacy.OntoDirectoryException;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

public class OntologyStorageV0 implements OntologyStorageService {

private static final byte[] MAGIC_BYTES = { 0x08, 3, 0, 1, 1, 3, 0x7f, 0x16, 0 }; // Version 0
private static final int HEADER_SIZE = 10;
private static final byte LOCK_BYTE = 0x3f;
private static final byte UNLOCK_BYTE = 0x00;
private static final int BUFFER_SIZE = 64 << 10; // 64KB buffer for high-speed I/O

public ByteBuffer readFile(Path file, int maxSize) throws IOException {
	if (!Files.exists(file)) throw new FileNotFoundException(file.toAbsolutePath().toString());
	long size = Files.size(file);
	if (size > maxSize) throw new OntoDirectoryException.StorageFileCorruptedError("File exceeds limit");
	if (size < HEADER_SIZE) throw new OntoDirectoryException.StorageFileCorruptedError("Header truncated");
	
	byte[] allBytes = Files.readAllBytes(file);
	if (allBytes[0] != UNLOCK_BYTE) throw new OntoDirectoryException.StorageFileLockedError("File locked: " + file);
	
	for (int i = 0; i < MAGIC_BYTES.length; i++) {
		if (allBytes[i + 1] != MAGIC_BYTES[i]) throw new OntoDirectoryException.StorageFileCorruptedError("Magic mismatch");
	}
	return ByteBuffer.wrap(allBytes, HEADER_SIZE, allBytes.length - HEADER_SIZE);
}

public DataOutputStream requestFileSave(Path file) throws IOException {
	if (Files.exists(file)) {
		try (InputStream is = Files.newInputStream(file)) {
			if (is.read() != UNLOCK_BYTE) throw new OntoDirectoryException.StorageFileLockedError("Locked: " + file);
		}
	}
	// Use a large buffer to minimize syscalls
	DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file), BUFFER_SIZE));
	dos.write(LOCK_BYTE);
	dos.write(MAGIC_BYTES);
	return dos;
}

public void closeFileSave(Path file) throws IOException {
	try (FileChannel fc = FileChannel.open(file, StandardOpenOption.WRITE)) {
		ByteBuffer unlock = ByteBuffer.allocate(1).put(UNLOCK_BYTE);
		unlock.flip();
		fc.write(unlock, 0);
	}
}

public void loadOntologyHierarchyFromFile(Path source, @NotNull OntologyHierarchyFast target) throws IOException {
	ByteBuffer data = readFile(source, 102 << 16);
	int elementCount = data.getInt();
	target.ontologyClasses.clear();
	
	ArrayList<OntologyClass> classes = target.ontologyClasses;
	OntologyClass.getEmptyArrayList(classes, elementCount);
	
	for (int i = 0; i < elementCount; i++) {
		int payloadSize = data.getInt();
		if(payloadSize == 0) { classes.set(i, null); continue; } // tombstone
		int expectedNextPos = data.position() + payloadSize;
		
		int nameLen = data.getInt();
		byte[] nameBytes = new byte[nameLen];
		data.get(nameBytes);
		
		OntologyClass oc = classes.get(i);
		oc.name = new String(nameBytes, StandardCharsets.UTF_8);
		
		int pCount = data.getInt();
		oc.parents.ensureCapacity(pCount);
		for (int j = 0; j < pCount; j++) oc.parents.add(target.getClassFromIdentity(data.getInt()));
		
		int cCount = data.getInt();
		oc.children.ensureCapacity(cCount);
		for (int j = 0; j < cCount; j++) oc.children.add(target.getClassFromIdentity(data.getInt()));
		
		data.position(expectedNextPos); // Resilience jump
	}
}

public void saveOntologyHierarchy(Path target, @NotNull OntologyHierarchyFast hierarchy) throws IOException {
	Path tempTarget = target.resolveSibling(target.getFileName().toString() + ".tmp");
	
	try (DataOutputStream dos = requestFileSave(tempTarget)) {
		ArrayList<OntologyClass> classes = hierarchy.ontologyClasses;
		dos.writeInt(classes.size());
		
		for (OntologyClass oc : classes) {
			if (oc == null) {
				dos.writeInt(0);
				continue;
			} // tombstone
			
			byte[] nameBytes = oc.name.getBytes(StandardCharsets.UTF_8);
			// Payload size = name_len(4) + name + parent_cnt(4) + parents(N*4) + child_cnt(4) + children(N*4)
			int payloadSize = 12 + nameBytes.length + (oc.parents.size() * 4) + (oc.children.size() * 4);
			
			dos.writeInt(payloadSize);
			dos.writeInt(nameBytes.length);
			dos.write(nameBytes);
			
			dos.writeInt(oc.parents.size());
			for (OntologyClass p : oc.parents) dos.writeInt(hierarchy.getIdentityFromClass(p));
			
			dos.writeInt(oc.children.size());
			for (OntologyClass c : oc.children) dos.writeInt(hierarchy.getIdentityFromClass(c));
		}
		dos.flush();
	} finally {
		closeFileSave(tempTarget); // Guarantee release of the lock byte
	}
	
	// Atomic swap prevents partial writes or corrupted state during a crash
	Files.move(tempTarget, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
}

public ArrayList<FileInterface> loadOntologyElementsFromFile(Path source) throws IOException {
	ByteBuffer data = readFile(source, 180 << 20);
	int fileCount = data.getInt();
	if (fileCount < 0 || fileCount > 1_000_000) throw new IOException("Invalid file count");
	
	ArrayList<FileInterface> files = new ArrayList<>(fileCount);
	byte[] diskBuf = new byte[8];
	byte[] nameBuf = new byte[128];
	
	for (int i = 0; i < fileCount; i++) {
		FileInterface fi = new FileInterface();
		fi.identity = i;
		
		data.get(diskBuf);
		if(diskBuf[0] == 0) { files.add(null); continue; } // tombstone
		fi.diskName = new String(diskBuf, StandardCharsets.US_ASCII).trim();
		
		data.get(nameBuf);
		fi.actualName = new String(nameBuf, StandardCharsets.UTF_8).trim();
		
		int tagCount = data.getInt();
		fi.tagsByIdentity = new ArrayList<>(tagCount);
		if(tagCount == 0) { fi.tagsByIdentity.add(0); } // no parent means element of "File" tag.
		for (int j = 0; j < tagCount; j++) {
			fi.tagsByIdentity.add(data.getInt());
		}
		files.add(fi);
	}
	return files;
}

public void saveOntologyElements(@NotNull Path target, @NotNull ArrayList<FileInterface> files) throws IOException {
	Path tempTarget = target.resolveSibling(target.getFileName().toString() + ".tmp");
	
	try (DataOutputStream dos = requestFileSave(tempTarget)) {
		dos.writeInt(files.size());
		final byte[] padding = new byte[128];
		int index = 0;
		byte nullHeader[] = new byte[8];
		for (FileInterface fi : files) {
			if (fi == null) {
				dos.write(nullHeader);
				index++; // Keep alignment with the tombstone
				continue;
			}
			
			// Disk Name (8 bytes ASCII)
			if (fi.diskName == null) throw new OntoDirectoryException.
				  OntologyHierarchyHasUnintendedStateException("Disk name for file (i:" + index + ") ID " + fi.identity + " was null!");
			byte[] dBytes = fi.diskName.getBytes(StandardCharsets.US_ASCII);
			dos.write(dBytes, 0, Math.min(8, dBytes.length));
			if (dBytes.length < 8) dos.write(padding, 0, 8 - dBytes.length);
			
			// Actual Name (128 bytes UTF-8)
			if (fi.actualName == null) throw new OntoDirectoryException.
				  OntologyHierarchyHasUnintendedStateException("Actual name for file (i:" + index + ") ID " + fi.identity + " was null!");
			byte[] nBytes = fi.actualName.getBytes(StandardCharsets.UTF_8);
			dos.write(nBytes, 0, Math.min(128, nBytes.length));
			if (nBytes.length < 128) dos.write(padding, 0, 128 - nBytes.length);
			
			// Variable Tags
			if (fi.tagsByIdentity == null) throw new OntoDirectoryException.
				  OntologyHierarchyHasUnintendedStateException("Tags array for file (i:" + index + ") ID " + fi.identity + " was null!");
			int tagCount = fi.tagsByIdentity.size();
			dos.writeInt(tagCount);
			for (int j = 0; j < tagCount; j++) {
				dos.writeInt(fi.tagsByIdentity.get(j));
			}
			index++;
		}
		dos.flush();
	} finally {
		closeFileSave(tempTarget);
	}
	
	Files.move(tempTarget, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
}


}