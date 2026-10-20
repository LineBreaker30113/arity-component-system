package org.halim.nStructure.archive.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks the on-disk identity of each element -- the obfuscated disk filename and resolved actual
 * path -- as a side-table keyed by element identity. The ontology engine only tracks names (see
 * {@code OntologyElementHandle}'s javadoc); disk-file bookkeeping is deliberately a data-lake-only
 * concern layered on top here, not part of the engine itself.
 * <p>
 * Ports legacy {@code FileInterface}'s {@code diskName}/{@code actualFile} fields and
 * {@code getDiskNameFor}'s obfuscation algorithm verbatim. Persisted separately from the ontology
 * engine's own four storage blocks, via {@link #saveTo}/{@link #loadFrom} -- disk names are not
 * re-derivable from the element id alone (the obfuscation is randomized per call), so they must be
 * stored, not regenerated, on every load.
 */
public class FileMetadataCatalog {

private static final byte[] NOISE = "ghijklmnoqprstuvyzwGHIJKLMNOQPRSTUVYZW".getBytes(StandardCharsets.US_ASCII);
private static final byte[] REPRESENTERS = "00112233445566778899aAbBcCdDeEfF".getBytes(StandardCharsets.US_ASCII);

private final Map<Integer, String> diskNames = new HashMap<>();
private int lastIndex = -1;

/** Generates a fresh, randomized 8-char obfuscated disk name embedding the hex of {@code identity}. */
public static String generateDiskName(int identity) {
    String hex = Integer.toHexString(identity);
    byte[] hexBytes = hex.getBytes(StandardCharsets.US_ASCII);
    byte[] result = new byte[8];
    int noiseLength = 8 - hexBytes.length;

    for (int i = 0; i < noiseLength; i++) {
        result[i] = NOISE[(int) (Math.random() * NOISE.length)];
    }
    for (int i = 0; i < hexBytes.length; i++) {
        int hexValue = Character.digit(hexBytes[i], 16);
        if (hexValue == 0) {
            result[i + noiseLength] = NOISE[(int) (Math.random() * NOISE.length)];
            continue;
        }
        int representerIndex = (hexValue * 2) + (Math.random() < 0.5 ? 0 : 1);
        result[i + noiseLength] = REPRESENTERS[representerIndex];
    }
    return new String(result, StandardCharsets.US_ASCII);
}

public String getDiskName(int identity) { return diskNames.get(identity); }

public Path getActualFile(int identity, Path lakePath) {
    String diskName = diskNames.get(identity);
    return diskName == null ? null : lakePath.resolve(diskName);
}

public void register(int identity, String diskName) {
    diskNames.put(identity, diskName);
    if (identity > lastIndex) lastIndex = identity;
}

public void forget(int identity) { diskNames.remove(identity); }

/** Same framing style as OntologyStorageSession's catalogs: lastIndex, then one entry per index
 *  (-1 length = no disk file for that id). */
public void saveTo(DataOutputStream out) {
    try {
        out.writeInt(lastIndex);
        for (int i = 0; i <= lastIndex; i++) {
            String diskName = diskNames.get(i);
            if (diskName == null) { out.writeInt(-1); continue; }
            byte[] bytes = diskName.getBytes(StandardCharsets.US_ASCII);
            out.writeInt(bytes.length);
            out.write(bytes);
        }
    } catch (IOException e) {
        throw new UncheckedIOException("Failed to write file metadata catalog", e);
    }
}

public static FileMetadataCatalog loadFrom(DataInputStream in) {
    FileMetadataCatalog catalog = new FileMetadataCatalog();
    try {
        int lastIndex = in.readInt();
        for (int i = 0; i <= lastIndex; i++) {
            int len = in.readInt();
            if (len == -1) continue;
            byte[] bytes = new byte[len];
            in.readFully(bytes);
            catalog.register(i, new String(bytes, StandardCharsets.US_ASCII));
        }
    } catch (IOException e) {
        throw new UncheckedIOException("Failed to read file metadata catalog", e);
    }
    return catalog;
}

}
