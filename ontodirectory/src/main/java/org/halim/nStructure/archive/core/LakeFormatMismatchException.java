package org.halim.nStructure.archive.core;

/**
 * Thrown by {@link ArchiveManager}'s constructor when the target directory already holds a
 * legacy-format Data Lake (see {@code org.halim.legacy.dlake.DataLakeManager}) rather than an
 * nStructure one. The two share {@code CONFIG_FILE_SUFFIX} ({@code .odConfig.bin}), so that file's
 * mere presence can't distinguish them -- this exists so the mistake surfaces as one clear,
 * actionable message instead of a generic {@code UncheckedIOException} from a failed catalog-file
 * read deep inside {@code loadLake()}.
 */
public class LakeFormatMismatchException extends RuntimeException {
    public LakeFormatMismatchException(String message) { super(message); }
}
