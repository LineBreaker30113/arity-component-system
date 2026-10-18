package org.halim.ontology.legacy.view.implementations;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.halim.ontology.legacy.core.segmentation.query.OntologyAnalyticsSegmentation;
import org.halim.ontology.legacy.view.query.OntologyAnalysisService;

public class OntologyAnalysisServiceImpl implements OntologyAnalysisService {

private final OntologyAnalyticsSegmentation analyticsService;

public OntologyAnalysisServiceImpl(OntologyAnalyticsSegmentation analyticsService) {
    this.analyticsService = analyticsService;
}

@Override public IntArrayList getHeritage(int classIdentity) { return analyticsService.getHeritage(classIdentity); }
@Override public IntArrayList getAncestors(int classIdentity) { return analyticsService.getAncestors(classIdentity); }
@Override public boolean isAncestor(int classIdentity, int doubtedIdentity) { return analyticsService.isAncestor(classIdentity, doubtedIdentity); }
@Override public boolean isHeritage(int classIdentity, int doubtedIdentity) { return analyticsService.isHeritage(classIdentity, doubtedIdentity); }

@Override public IntArrayList getDomainElements(int classIdentity) { return analyticsService.getDomainElements(classIdentity); }
@Override public int getDomainElementCount(int classIdentity) { return analyticsService.getDomainElementCount(classIdentity); }

@Override public int getUniquestChild(int classIdentity) { return analyticsService.getUniquestChild(classIdentity); }
@Override public int[] getChildrenByUniqueness(int classIdentity) { return analyticsService.getChildrenByUniqueness(classIdentity); }
@Override public int getUniquestParent(int classIdentity) { return analyticsService.getUniquestParent(classIdentity); }
@Override public int[] getParentsByUniqueness(int classIdentity) { return analyticsService.getParentsByUniqueness(classIdentity); }

@Override public boolean isUniquenessCalculationDirectionUpward() { return analyticsService.isUniquenessCalculationDirectionUpward(); }
@Override public void setUniquenessCalculationDirectionUpward(boolean upward) { analyticsService.setUniquenessCalculationDirectionUpward(upward); }
@Override public int[] getUniquestPath(int classIdentity) { return analyticsService.getUniquestPath(classIdentity); }
@Override public int[] getDeepestPath(int classIdentity) { return analyticsService.getDeepestPath(classIdentity); }

}
