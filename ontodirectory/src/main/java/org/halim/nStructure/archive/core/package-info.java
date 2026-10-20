/**
 * Where the archive layer's lake lifecycle actually lives: {@code ArchiveManager} is the per-lake
 * composition root (wraps one {@code OntologyEngine}, persists it to five custom binary catalog
 * files, and adapts it to both {@code mhexagon.OntologyReadingService} and {@code
 * mhexagon.OntoDirectoryService.DataLakeService}); {@code ArchiverServer} is the multi-lake
 * orchestrator, owning one {@code ArchiveManager} per opened path and tracking which is focused;
 * {@code FileMetadataCatalog} is the disk-name/actual-path side-table {@code ArchiveManager} keeps
 * since the ontology engine itself has no file-path concept; {@code RecentLakesRegistry} persists
 * which lake paths have been opened, across restarts, independently of legacy's {@code
 * SettingLogic.dataLakes} (deliberately namespaced -- see its own javadoc); and {@code
 * LakeFormatMismatchException} is what {@code ArchiveManager}'s constructor throws if pointed at a
 * directory already holding a legacy-format lake rather than an nStructure one.
 * <p>
 * {@link org.halim.nStructure.archive.core.segmentation} is an in-progress port split for this
 * package's own responsibilities, mirroring {@code ontology.core.segmentation}'s pattern -- most of
 * its interfaces are still stubs, not yet consumed by the classes above; see its own package-info.
 */
package org.halim.nStructure.archive.core;
