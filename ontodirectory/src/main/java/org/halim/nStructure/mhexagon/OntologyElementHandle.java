package org.halim.nStructure.mhexagon;

import org.halim.nStructure.ontology.view.OntologyService;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI-facing read-only view of an ontology element, replacing legacy's {@code FileInterface} object
 * with an int-identity handle backed by the new engine. Unlike {@code FileInterface}, this has no
 * disk-name/actual-file-path -- resolving those is data-lake storage bookkeeping, a separate concern
 * this refactor deliberately leaves untouched. Callers that need the real on-disk file (open/export/
 * drag) still need a data-lake-backed lookup; {@link #getActualFile()} makes that gap explicit
 * rather than silently returning something wrong.
 */
public class OntologyElementHandle {

private final int identity;
private final OntologyService service;

public OntologyElementHandle(int identity, OntologyService service) {
    this.identity = identity;
    this.service = service;
}

public int getIdentity() { return identity; }

public String getActualName() { return service.getOntologyDataService().getElementName(identity); }

/** The classes (tags) this element is directly connected to -- replaces legacy's
 *  {@code FileInterface.tagsByIdentity}. */
public List<OntologyClassHandle> getClasses() {
    List<OntologyClassHandle> result = new ArrayList<>();
    for (int c : service.getOntologyDataService().getClassesOf(identity)) result.add(new OntologyClassHandle(c, service));
    return result;
}

/** Not available from the ontology engine -- disk-path resolution is data-lake storage
 *  bookkeeping, out of scope for this refactor. */
public java.nio.file.Path getActualFile() {
    throw new UnsupportedOperationException(
          "File path resolution is a data-lake concern, not available through OntologyService");
}

@Override
public boolean equals(Object o) {
    return o instanceof OntologyElementHandle other && other.identity == identity;
}

@Override
public int hashCode() { return identity; }

@Override
public String toString() { return getActualName(); }

}
