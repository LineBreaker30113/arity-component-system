/**
 * Intended as the archive layer's client-facing port boundary, mirroring {@code
 * org.halim.nStructure.mhexagon} at the ontology/GUI seam -- but as of this writing it holds no
 * active ports of its own; its only content is the {@link
 * org.halim.nStructure.archive.ahexagon.deprecated} subpackage, two unreferenced types kept for
 * historical reference rather than in active use. Callers needing archive-layer ports today go
 * through {@code org.halim.nStructure.mhexagon.OntoDirectoryService} instead.
 */
package org.halim.nStructure.archive.ahexagon;
