/**
 * Builds and wires the ontology engine itself. {@code OntologyEngine} is the composition root: it
 * constructs each {@code segments} implementation, wraps it behind its {@code segmentation} port
 * via {@code OntologyServiceFactory} (a dynamic-proxy factory backed by {@code
 * OntologyServiceProxy}), and exposes the result. The proxy is where every cross-cutting concern
 * lives -- {@code TransactionContext} records undo/redo closures and coordinates nested transaction
 * boundaries, {@code UndoRedoStack} holds the resulting committed-transaction history, {@code
 * EngineLockHandle} is a held write-lock for multi-call sessions (e.g. save/load), and {@code
 * OntologyServiceLogger} is an optional pluggable sink for invocation logging -- so {@code segments}
 * implementations themselves stay free of locking, undo/redo, or logging code.
 * <p>
 * Two subpackages hold what this composition root wires together: {@link
 * org.halim.ontology.legacy.core.segmentation} declares the ports (interfaces) each engine
 * responsibility exposes, and {@link org.halim.ontology.legacy.core.segments} holds the
 * concrete classes that implement those ports. Nothing outside this package or {@code
 * org.halim.ontology.legacy.view} should reference {@code segments} directly -- always go
 * through a {@code segmentation} interface, so the proxy's cross-cutting concerns can't be
 * bypassed.
 */
package org.halim.ontology.legacy.core;
