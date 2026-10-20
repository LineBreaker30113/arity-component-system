/**
 * Uses the {@code org.halim.nStructure.ontology} engine to classify files: a "Data Lake" is a
 * directory whose files are each registered as an ontology element and tagged into the class
 * hierarchy, instead of nested in folders. This package owns everything the ontology engine itself
 * has no concept of -- disk paths, imports/exports, lake lifecycle (create/load/save/vacuum),
 * bug-report dumps -- while delegating all classification questions to the engine underneath.
 * <p>
 * {@link org.halim.nStructure.archive.core} is where this actually lives today: {@code
 * ArchiveManager} (per-lake composition root) and {@code ArchiverServer} (multi-lake orchestrator),
 * plus an in-progress {@code segmentation}-style port split mirroring {@code ontology.core}'s.
 * {@link org.halim.nStructure.archive.ahexagon} is this package's client-facing-port layer,
 * mirroring {@code org.halim.nStructure.mhexagon} at the ontology/GUI seam -- as of this writing it
 * holds no live ports, only a {@code deprecated} subpackage of two unreferenced, superseded types;
 * see that subpackage's own package-info for specifics.
 */
package org.halim.nStructure.archive;
