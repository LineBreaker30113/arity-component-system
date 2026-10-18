/**
 * The client-facing driven port for persisting the engine's state to an arbitrary data stream (see
 * {@code view/package-info.java}). Storage is chunked rather than built as one large in-memory blob:
 * a {@link org.halim.ontology.legacy.view.implementations.persistence.OntologyStorageSession}/
 * {@link org.halim.ontology.legacy.view.implementations.persistence.OntologyRestoreSession} pair is opened via
 * {@code OntologyService#beginStoring}/{@code #beginLoading}, then written to or read from in
 * successive, self-describing chunks (one call per chunk), so a very large ontology can be saved or
 * loaded incrementally (e.g. one chunk per GUI event-loop tick without blocking the EDT) and a
 * truncated stream is detectable rather than silently misread.
 */
package org.halim.ontology.legacy.view.implementations.persistence;
