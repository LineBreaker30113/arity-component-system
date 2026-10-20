package org.halim.legacy.dlake;

import org.jetbrains.annotations.NotNull;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Service interface for handling the persistent storage of Ontology hierarchies
 * and file metadata.
 */
public interface OntologyStorageService {

/**
 * Reads a file and validates its magic numbers and lock status.
 */
ByteBuffer readFile(Path file, int maxSize) throws IOException;

/**
 * Initializes a write stream to a file, setting the lock byte and magic numbers.
 */
DataOutputStream requestFileSave(Path file) throws IOException;

/**
 * Unlocks the file by resetting the first byte to zero.
 */
void closeFileSave(Path file) throws IOException;

/**
 * Loads the hierarchy structure from the binary source.
 */
void loadOntologyHierarchyFromFile(Path source, OntologyHierarchyFast target) throws IOException;

/**
 * Loads the list of file interfaces (metadata) from the binary source.
 */
ArrayList<FileInterface> loadOntologyElementsFromFile(Path source) throws IOException;

/**
 * Persists the given ontology hierarchy to the target path.
 */
void saveOntologyHierarchy(Path target, @NotNull OntologyHierarchyFast hierarchy) throws IOException;

/**
 * Persists the list of file elements to the target path.
 */
void saveOntologyElements(Path target, @NotNull ArrayList<FileInterface> files) throws IOException;


///**
// * Loads the whole ontology structure from the binary source.
// */
//void loadOntology(Path rootPath, OntologyHierarchyFast targetHierarchy) throws IOException;
///**
// * Persists the given ontology hierarchy to the target path.
// */
//void saveOntology(Path targetRoot, @NotNull OntologyHierarchyFast hierarchy) throws IOException;

/**
 * Persists the list of file elements to the target path.
 */


///** * Serializes this class's state into a binary format.
// * <p>
// * <b>Precondition:</b> The caller must ensure that this class exists within the provided
// * {@link OntologyHierarchy}. The self identity is assumed to be its index resolved by the {@code owner}.
// * * @param owner The map resolving object instances to integer identities.
// * @return A tightly packed {@link ByteBuffer} containing the serialized class data.
// */
//public @NotNull ByteBuffer serializeOC(@NotNull OntologyHierarchyNew hierarchy, @NotNull OntologyClass oc);
//
///**
// * Hydrates this object's state from a binary payload.
// * <p>
// * <b>Usage Protocol:</b>
// * <ol>
// * <li>Read the total number of tags from the file header.</li>
// * <li>Allocate the required instances using {@link org.halim.legacy.dlake.OntologyClass#getEmptyArray(int)}.</li>
// * <li>Invoke this method on each instance, passing its specific data slice.</li>
// * </ol>
// *
// * @param owner The map resolving integer identities back to {@code OntologyClass} instances.
// * @param data  The binary payload containing this class's serialized state.
// */
//public void unPackOC(@NotNull OntologyHierarchyNew owner, @NotNull ByteBuffer data, @NotNull OntologyClass oc);

}