/**
 * An in-progress port split for the archive layer's own responsibilities, applying {@code
 * ontology.core.segmentation}'s pattern (one narrow interface per responsibility) to {@code
 * archive.core}. Only {@code FileCatalogSegmentation} has real content today -- it extends {@code
 * ontology.core.segmentation.query.OntologyElementCatalogSegmentation} to add disk-specific detail
 * (disk name, file size, file properties) alongside the engine's own per-element catalog data.
 * {@code ArchiveFileSegmentation}, {@code DirectoryArchiverService} (the intended "final
 * orchestrator," per its own comment), and {@code MultipleArchivesSegmentation} (one method,
 * {@code getLoadedArchiveCount}) are placeholder interfaces, not yet implemented or consumed by
 * {@code ArchiveManager}/{@code ArchiverServer} -- confirm their intended scope before building
 * against them.
 */
package org.halim.nStructure.archive.core.segmentation;
