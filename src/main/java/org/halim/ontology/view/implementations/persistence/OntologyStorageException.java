package org.halim.ontology.view.implementations.persistence;

/**
 * Unchecked wrapper for I/O and format failures during a storage/restore session, matching this
 * architecture's fail-loud style (no checked exceptions on the view/core ports).
 */
public class OntologyStorageException extends RuntimeException {
    public OntologyStorageException(String message) { super(message); }
    public OntologyStorageException(String message, Throwable cause) { super(message, cause); }
}
