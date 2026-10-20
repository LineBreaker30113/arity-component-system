package org.halim.legacy;

public class OntoDirectoryException extends RuntimeException {
public OntoDirectoryException(String message) { super(message); }

/** Ontology Hierarchy has unintended state. */
public static class OntologyHierarchyHasUnintendedStateException extends OntoDirectoryException {
	public OntologyHierarchyHasUnintendedStateException(String message) { super(message); }
}
public static class NullGivenAsOntologyClassException extends OntoDirectoryException {
	public NullGivenAsOntologyClassException(String message) { super(message); }
}

public static class OntologyRootClassAddParentCall extends OntoDirectoryException {
	public OntologyRootClassAddParentCall(String message) { super(message); }
}
/** When trying to add a child as a parent. */
public static class OntologyAddParentCausesCycle extends OntoDirectoryException {
	public OntologyAddParentCausesCycle(String message) { super(message); }
}
/** When trying to add a child as a parent. */
public static class OntologyAddParentSelf extends OntoDirectoryException {
	public OntologyAddParentSelf(String message) { super(message); }
}

public static class StorageFileHandlingError extends OntoDirectoryException {
public StorageFileHandlingError(String message) { super(message); }
}
/** When the lock byte is not equal to one. */
public static class StorageFileLockedError extends StorageFileHandlingError {
	public StorageFileLockedError(String message) { super(message); }
}
/** When the magic numbers are not the appropriate. */
public static class StorageFileCorruptedError extends StorageFileHandlingError {
	public StorageFileCorruptedError(String message) { super(message); }
}
/** When the version mismatches. */
public static class StorageFileVersionError extends StorageFileHandlingError {
	public StorageFileVersionError(String message) { super(message); }
}

}
