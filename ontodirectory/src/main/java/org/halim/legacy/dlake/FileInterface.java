package org.halim.legacy.dlake;

import org.halim.legacy.OntoDirectoryException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;

public class FileInterface {

public static final int RECORD_SIZE = 140;

public Path actualFile;
public int identity;
public String diskName;
public String actualName;
public ArrayList<Integer> tagsByIdentity;

public static final byte[] noise = "ghijklmnoqprstuvyzwGHIJKLMNOQPRSTUVYZW".getBytes(StandardCharsets.US_ASCII);
public static final byte[] representers = "00112233445566778899aAbBcCdDeEfF".getBytes(StandardCharsets.US_ASCII);

@Override
public int hashCode() {
	return java.util.Objects.hash(identity);
}

@Contract(pure = true)
public static FileInterface @NotNull [] getEmptyArray(int elementCount) {
	FileInterface[] result = new FileInterface[elementCount];
	for(int i = 0; i < elementCount; i++) { result[i] = new FileInterface(); }
	return result;
}

@Contract("_ -> new")
public static @NotNull String getDiskNameFor(int integer) {
	String hex = Integer.toHexString(integer);
	byte[] hexBytes = hex.getBytes(StandardCharsets.US_ASCII);
	byte[] result = new byte[8];
	int noiseLength = 8 - hexBytes.length;
	
	for(int i = 0; i < noiseLength; i++) {
		result[i] = noise[(int) (Math.random() * noise.length)];
	}
	
	for(int i = 0; i < hexBytes.length; i++) {
		int hexValue = Character.digit(hexBytes[i], 16);
		if(hexValue == 0) {
			result[i + noiseLength] = noise[(int) (Math.random() * noise.length)];
			continue;
		}
		int representerIndex = (hexValue * 2) + (Math.random() < 0.5 ? 0 : 1);
		result[i + noiseLength] = representers[representerIndex];
	}
	return new String(result, StandardCharsets.US_ASCII);
}

public void renameDisk(String newDiskName, Path lakePath) {
	Path oldPath = this.actualFile;
	Path newPath = lakePath.resolve(newDiskName);
	
	if (oldPath == null || oldPath.equals(newPath)) return;
	
	try {
		java.nio.file.Files.move(oldPath, newPath,
			  java.nio.file.StandardCopyOption.ATOMIC_MOVE,
			  java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		finalizePathUpdate(newDiskName, newPath);
	} catch (java.io.IOException e) {
		try {
			java.nio.file.Files.copy(oldPath, newPath,
				  java.nio.file.StandardCopyOption.REPLACE_EXISTING,
				  java.nio.file.StandardCopyOption.COPY_ATTRIBUTES);
			java.nio.file.Files.delete(oldPath);
			finalizePathUpdate(newDiskName, newPath);
		} catch (java.io.IOException ex) {
			throw new OntoDirectoryException(
				  "Critical IO Failure: Manager could not migrate file from "
					    + oldPath.getFileName() + " to " + newDiskName + ". Action aborted to prevent data loss."
			);
		}
	}
}

private void finalizePathUpdate(String newDiskName, Path newPath) {
	this.diskName = newDiskName;
	this.actualFile = newPath;
}

}