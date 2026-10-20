package org.halim.nStructure.archive.core.segmentation;

import org.halim.nStructure.ontology.core.segmentation.query.OntologyElementCatalogSegmentation;

public interface FileCatalogSegmentation extends OntologyElementCatalogSegmentation {

    String getDiskName(int elementIdentity);
    long getFileSize(int elementIdentity);
    String[] getFileProperties(int elementIndex);

}
