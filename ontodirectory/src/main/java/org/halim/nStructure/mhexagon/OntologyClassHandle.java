package org.halim.nStructure.mhexagon;

import org.halim.nStructure.ontology.view.OntologyService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * GUI-facing read-only view of an ontology class, replacing legacy's {@code OntologyClass}
 * object-graph node with an int-identity handle backed by the new engine. Mirrors
 * {@code OntologyClass}'s read surface (getName/getSafeParents/getSafeChildren/isAncestry/
 * isHeritage/getAncestrySet/forEachDescendant) so calling code needs minimal changes -- mutations
 * go through {@code OntologyReadingService.OntologyManagingService}, not this class.
 */
public class OntologyClassHandle {

private final int identity;
private final OntologyService service;

public OntologyClassHandle(int identity, OntologyService service) {
    this.identity = identity;
    this.service = service;
}

public int getIdentity() { return identity; }

public String getName() { return service.getOntologyDataService().getClassName(identity); }

public List<OntologyClassHandle> getSafeParents() {
    List<OntologyClassHandle> result = new ArrayList<>();
    for (int p : service.getOntologyDataService().getParents(identity)) result.add(new OntologyClassHandle(p, service));
    return result;
}

public List<OntologyClassHandle> getSafeChildren() {
    List<OntologyClassHandle> result = new ArrayList<>();
    for (int c : service.getOntologyDataService().getChildren(identity)) result.add(new OntologyClassHandle(c, service));
    return result;
}

/** Is this class equal to or a descendant of {@code candidate}? (self-inclusive, matching legacy) */
public boolean isAncestry(OntologyClassHandle candidate) {
    return identity == candidate.identity || service.getAnalyticsService().isAncestor(identity, candidate.identity);
}

/** Is {@code candidate} equal to or a descendant of this class? (self-inclusive, matching legacy) */
public boolean isHeritage(OntologyClassHandle candidate) {
    return identity == candidate.identity || service.getAnalyticsService().isHeritage(identity, candidate.identity);
}

/** This class plus every ancestor above it. */
public Set<OntologyClassHandle> getAncestrySet() {
    Set<OntologyClassHandle> result = new HashSet<>();
    result.add(this);
    for (int a : service.getAnalyticsService().getAncestors(identity)) result.add(new OntologyClassHandle(a, service));
    return result;
}

public void forEachDescendant(Consumer<OntologyClassHandle> action) {
    for (int d : service.getAnalyticsService().getHeritage(identity)) action.accept(new OntologyClassHandle(d, service));
}

@Override
public boolean equals(Object o) {
    return o instanceof OntologyClassHandle other && other.identity == identity;
}

@Override
public int hashCode() { return identity; }

@Override
public String toString() { return getName(); }

}
