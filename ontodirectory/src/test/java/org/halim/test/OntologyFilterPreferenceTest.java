package org.halim.test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.halim.nStructure.ontology.core.segmentation.query.*;
import org.halim.nStructure.ontology.view.filter.AbstractEnumeratingFilterBase;
import org.halim.nStructure.ontology.view.filter.AbstractTestingFilterBase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins every preference-propagation rule for both filter kinds. Preference is a property of the filter
 * tree alone -- no engine is involved -- so these are pure structural assertions.
 * <p>
 * The rules: testing is the default and a node must prove the opposite. {@code Or} needs both children
 * to prefer enumeration because a union can grow the set; {@code And} and {@code Subtract} need only
 * one, because they remove instances from an already-valid set, so one branch drives the enumeration
 * while the other is used purely as a test.
 */
public class OntologyFilterPreferenceTest {

// --- Leaves ---

private static final OntologyClassFilter ENUMERATING_CLASS_LEAF = new OntologyClassFilter.DescendantOf(1);
private static final OntologyClassFilter TESTING_CLASS_LEAF = new OntologyClassFilter.NameContaining("x");
private static final OntologyElementFilter ENUMERATING_ELEMENT_LEAF = new OntologyElementFilter.UnderDomain(1);
private static final OntologyElementFilter TESTING_ELEMENT_LEAF = new OntologyElementFilter.NameContaining("x");

@Test
public void testHierarchyClassLeaves_AlwaysPreferEnumeration() {
	assertThat(new OntologyClassFilter.ChildOf(1).isEnumeratePreferred()).isTrue();
	assertThat(new OntologyClassFilter.DescendantOf(1).isEnumeratePreferred()).isTrue();
	assertThat(new OntologyClassFilter.DomainOf(1).isEnumeratePreferred()).isTrue();
	assertThat(new OntologyClassFilter.ParentOf(1).isEnumeratePreferred()).isTrue();
	assertThat(new OntologyClassFilter.AncestorOf(1).isEnumeratePreferred()).isTrue();
}

@Test
public void testClassificationElementLeaves_AlwaysPreferEnumeration() {
	assertThat(new OntologyElementFilter.DirectElementOf(1).isEnumeratePreferred()).isTrue();
	assertThat(new OntologyElementFilter.UnderDomain(1).isEnumeratePreferred()).isTrue();
}

@Test
public void testNameContaining_NeverPrefersEnumeration() {
	assertThat(new OntologyClassFilter.NameContaining("x").isEnumeratePreferred()).isFalse();
	assertThat(new OntologyElementFilter.NameContaining("x").isEnumeratePreferred()).isFalse();
}

// --- Combinators: Or needs both, And needs either, Subtract asks only its kept side ---

@Test
public void testOr_PrefersEnumeration_OnlyWhenBothChildrenDo() {
	assertThat(new OntologyClassFilter.Or(ENUMERATING_CLASS_LEAF, ENUMERATING_CLASS_LEAF).isEnumeratePreferred()).isTrue();
	assertThat(new OntologyClassFilter.Or(ENUMERATING_CLASS_LEAF, TESTING_CLASS_LEAF).isEnumeratePreferred()).isFalse();
	assertThat(new OntologyClassFilter.Or(TESTING_CLASS_LEAF, ENUMERATING_CLASS_LEAF).isEnumeratePreferred()).isFalse();
	assertThat(new OntologyClassFilter.Or(TESTING_CLASS_LEAF, TESTING_CLASS_LEAF).isEnumeratePreferred()).isFalse();

	assertThat(new OntologyElementFilter.Or(ENUMERATING_ELEMENT_LEAF, ENUMERATING_ELEMENT_LEAF).isEnumeratePreferred()).isTrue();
	assertThat(new OntologyElementFilter.Or(ENUMERATING_ELEMENT_LEAF, TESTING_ELEMENT_LEAF).isEnumeratePreferred()).isFalse();
	assertThat(new OntologyElementFilter.Or(TESTING_ELEMENT_LEAF, ENUMERATING_ELEMENT_LEAF).isEnumeratePreferred()).isFalse();
	assertThat(new OntologyElementFilter.Or(TESTING_ELEMENT_LEAF, TESTING_ELEMENT_LEAF).isEnumeratePreferred()).isFalse();
}

@Test
public void testAnd_PrefersEnumeration_WhenEitherChildDoes() {
	assertThat(new OntologyClassFilter.And(ENUMERATING_CLASS_LEAF, ENUMERATING_CLASS_LEAF).isEnumeratePreferred()).isTrue();
	assertThat(new OntologyClassFilter.And(ENUMERATING_CLASS_LEAF, TESTING_CLASS_LEAF).isEnumeratePreferred()).isTrue();
	assertThat(new OntologyClassFilter.And(TESTING_CLASS_LEAF, ENUMERATING_CLASS_LEAF).isEnumeratePreferred()).isTrue();
	assertThat(new OntologyClassFilter.And(TESTING_CLASS_LEAF, TESTING_CLASS_LEAF).isEnumeratePreferred()).isFalse();

	assertThat(new OntologyElementFilter.And(ENUMERATING_ELEMENT_LEAF, ENUMERATING_ELEMENT_LEAF).isEnumeratePreferred()).isTrue();
	assertThat(new OntologyElementFilter.And(ENUMERATING_ELEMENT_LEAF, TESTING_ELEMENT_LEAF).isEnumeratePreferred()).isTrue();
	assertThat(new OntologyElementFilter.And(TESTING_ELEMENT_LEAF, ENUMERATING_ELEMENT_LEAF).isEnumeratePreferred()).isTrue();
	assertThat(new OntologyElementFilter.And(TESTING_ELEMENT_LEAF, TESTING_ELEMENT_LEAF).isEnumeratePreferred()).isFalse();
}

@Test
public void testSubtract_FollowsKeptSideOnly_ExcludedIsIrrelevant() {
	assertThat(new OntologyClassFilter.Subtract(ENUMERATING_CLASS_LEAF, ENUMERATING_CLASS_LEAF).isEnumeratePreferred()).isTrue();
	assertThat(new OntologyClassFilter.Subtract(ENUMERATING_CLASS_LEAF, TESTING_CLASS_LEAF).isEnumeratePreferred()).isTrue();
	assertThat(new OntologyClassFilter.Subtract(TESTING_CLASS_LEAF, ENUMERATING_CLASS_LEAF).isEnumeratePreferred()).isFalse();
	assertThat(new OntologyClassFilter.Subtract(TESTING_CLASS_LEAF, TESTING_CLASS_LEAF).isEnumeratePreferred()).isFalse();

	assertThat(new OntologyElementFilter.Subtract(ENUMERATING_ELEMENT_LEAF, ENUMERATING_ELEMENT_LEAF).isEnumeratePreferred()).isTrue();
	assertThat(new OntologyElementFilter.Subtract(ENUMERATING_ELEMENT_LEAF, TESTING_ELEMENT_LEAF).isEnumeratePreferred()).isTrue();
	assertThat(new OntologyElementFilter.Subtract(TESTING_ELEMENT_LEAF, ENUMERATING_ELEMENT_LEAF).isEnumeratePreferred()).isFalse();
	assertThat(new OntologyElementFilter.Subtract(TESTING_ELEMENT_LEAF, TESTING_ELEMENT_LEAF).isEnumeratePreferred()).isFalse();
}

@Test
public void testPreference_PropagatesThroughNesting() {
	// One testing leaf buried under an Or anywhere in the tree defeats enumeration all the way up,
	// because Or is the one combinator that needs both sides.
	OntologyClassFilter deepOrWithTester = new OntologyClassFilter.And(
		  new OntologyClassFilter.Subtract(
				new OntologyClassFilter.Or(ENUMERATING_CLASS_LEAF, TESTING_CLASS_LEAF), ENUMERATING_CLASS_LEAF),
		  TESTING_CLASS_LEAF);
	assertThat(deepOrWithTester.isEnumeratePreferred()).isFalse();

	// Swap that one leaf for an enumerating one and the whole tree can anchor.
	OntologyClassFilter deepOrAllEnumerating = new OntologyClassFilter.And(
		  new OntologyClassFilter.Subtract(
				new OntologyClassFilter.Or(ENUMERATING_CLASS_LEAF, ENUMERATING_CLASS_LEAF), ENUMERATING_CLASS_LEAF),
		  TESTING_CLASS_LEAF);
	assertThat(deepOrAllEnumerating.isEnumeratePreferred()).isTrue();
}

// --- Custom nodes: the wrapper forwards whatever the client implementation declares ---

@Test
public void testCustom_ForwardsTheImplementationsDeclaration() {
	CustomOntologyFilterBase enumeratingClass = new AbstractEnumeratingFilterBase() {
		@Override public IntArrayList enumerate() { return new IntArrayList(); }
	};
	CustomOntologyFilterBase testingClass = new AbstractTestingFilterBase() {
		@Override public boolean test(int candidateIdentity) { return false; }
	};
	assertThat(new OntologyClassFilter.Custom(enumeratingClass).isEnumeratePreferred()).isTrue();
	assertThat(new OntologyClassFilter.Custom(testingClass).isEnumeratePreferred()).isFalse();

	CustomOntologyFilterBase enumeratingElement = new AbstractEnumeratingFilterBase() {
		@Override public IntArrayList enumerate() { return new IntArrayList(); }
	};
	CustomOntologyFilterBase testingElement = new AbstractTestingFilterBase() {
		@Override public boolean test(int candidateIdentity) { return false; }
	};
	assertThat(new OntologyElementFilter.Custom(enumeratingElement).isEnumeratePreferred()).isTrue();
	assertThat(new OntologyElementFilter.Custom(testingElement).isEnumeratePreferred()).isFalse();
}

@Test
public void testLambdaImplementation_IsTestPreferring() {
	assertThat(new OntologyClassFilter.Custom(id -> id == 3).isEnumeratePreferred()).isFalse();
	assertThat(new OntologyElementFilter.Custom(id -> id == 3).isEnumeratePreferred()).isFalse();
}

/** An implementation that declares neither direction is treated as test-preferring, silently -- test
 *  is always correct, just possibly slower than an enumeration would have been. */
@Test
public void testImplementationDeclaringNeitherDirection_IsSilentlyTestPreferring() {
	CustomOntologyFilterBase declaresNothing = new CustomOntologyFilterBase() {
		@Override public boolean test(int candidateIdentity) { return candidateIdentity == 1; }
	};
	assertThat(declaresNothing.isEnumeratePreferred()).isFalse();
	assertThat(new OntologyClassFilter.Custom(declaresNothing).isEnumeratePreferred()).isFalse();
	assertThatThrownBy(declaresNothing::enumerate).isInstanceOf(UnsupportedOperationException.class);
}

}
