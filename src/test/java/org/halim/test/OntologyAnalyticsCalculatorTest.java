package org.halim.test;

import org.halim.ontology.legacy.core.segments.data.OntologyAnalyticsCalculator;
import org.halim.ontology.legacy.core.segments.manager.DoubleArrayedIndexer;
import org.halim.ontology.legacy.core.segments.manager.OntologyHierarchyLister;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class OntologyAnalyticsCalculatorTest {

@Test
public void testIsUniquenessCalculationDirectionUpward_DefaultsToFalse() {
	OntologyAnalyticsCalculator analytics =
		  new OntologyAnalyticsCalculator(new OntologyHierarchyLister(), new DoubleArrayedIndexer());

	// Matches the documented contract on OntologyAnalyticsService#isUniquenessCalculationDirectionUpward:
	// "Defaults to false (downward)".
	assertThat(analytics.isUniquenessCalculationDirectionUpward()).isFalse();
}

}
