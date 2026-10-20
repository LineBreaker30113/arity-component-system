package org.halim.nStructure.ontology.view.query;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.halim.nStructure.ontology.core.segmentation.query.OntologyAnalyticsSegmentation;

/**
 * Client-facing analytics: full-DAG aggregates (heritage/ancestors, domain membership) and
 * uniqueness ranking, on top of {@link OntologyDataQueryService}'s direct parent/child/element
 * facts. Thin pass-through to {@code core}'s {@link OntologyAnalyticsSegmentation}.
 */
public interface OntologyAnalysisService {

    IntArrayList getHeritage(int classIdentity);
    IntArrayList getAncestors(int classIdentity);
    boolean isAncestor(int classIdentity, int doubtedIdentity);
    boolean isHeritage(int classIdentity, int doubtedIdentity);

    IntArrayList getDomainElements(int classIdentity);
    int getDomainElementCount(int classIdentity);

    int getUniquestChild(int classIdentity);
    int[] getChildrenByUniqueness(int classIdentity);
    int getUniquestParent(int classIdentity);
    int[] getParentsByUniqueness(int classIdentity);

    boolean isUniquenessCalculationDirectionUpward();
    void setUniquenessCalculationDirectionUpward(boolean upward);
    int[] getUniquestPath(int classIdentity);
    int[] getDeepestPath(int classIdentity);

}
