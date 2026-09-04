package org.halim.ontology.incubation;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Holds both {@link OE_IntSet} representations to the same contract. Nearly every test here is
 * parameterized over the two, so the positive and the negative form are checked against one
 * reference {@link TreeSet} and, through it, against each other - only the tests that are about a
 * representation's own trade-off are specific to one of them.
 * <p>
 * Lives in the source package rather than {@code org.halim.test} because it reaches for
 * {@code getSet()} and the backing lists to check the storage invariants after every mutation.
 * */
public class OEIntSetListerTest {

/** Seeded, so a failure is reproducible and reports the sequence that got there. */
private static final long SEED = 2026;
private static final int ROUNDS = 120;
private static final int OPERATIONS_PER_ROUND = 100;


/**
 * A lister together with the store behind it, so a test can look past the interface at how the set
 * is actually held.
 * */
static final class Form {
	
	final Object backing;
	final OE_IntSet set;
	
	Form(Object backing, OE_IntSet set) {
		this.backing = backing;
		this.set = set;
	}
	
	int first() {
		return switch(backing) {
			case IntSetListerNegative negative -> negative.getFirst();
			case IntSetListerPositive positive -> positive.getFirst();
			default -> throw new IllegalStateException("unknown form " + backing);
		};
	}
	
	int end() {
		return switch(backing) {
			case IntSetListerNegative negative -> negative.getEnd();
			case IntSetListerPositive positive -> positive.getEnd();
			default -> throw new IllegalStateException("unknown form " + backing);
		};
	}
	
	/** The invariants each representation promises, restated as assertions. */
	void assertInvariants() {
		int previous = Integer.MIN_VALUE;
		switch(backing) {
			case IntSetListerNegative negative -> {
				for(int h = 0; h < negative.exclusionList.size(); h++) {
					int hole = negative.exclusionList.getInt(h);
					assertThat(hole).as("holes ascending, no duplicates, in %s", negative).isGreaterThan(previous);
					assertThat(hole).as("hole strictly inside the bounds of %s", negative)
							.isStrictlyBetween(negative.firstIndex, negative.lastIndex);
					previous = hole;
				}
			}
			case IntSetListerPositive positive -> {
				for(int e = 0; e < positive.inclusionList.size(); e++) {
					int element = positive.inclusionList.getInt(e);
					assertThat(element).as("elements ascending, no duplicates, in %s", positive).isGreaterThan(previous);
					previous = element;
				}
			}
			default -> throw new IllegalStateException("unknown form " + backing);
		}
	}
	
	@Override
	public String toString() { return String.valueOf(backing); }
}

/** Builds one form prefilled with [firstIndex, lastIndex], both ends inclusive. */
interface RangeFactory {
	Form of(int firstIndex, int lastIndex);
}

static Form negativeRange(int firstIndex, int lastIndex) {
	IntSetListerNegative backing = new IntSetListerNegative(firstIndex, lastIndex);
	return new Form(backing, backing.getSet());
}
static Form positiveRange(int firstIndex, int lastIndex) {
	IntSetListerPositive backing = new IntSetListerPositive(firstIndex, lastIndex);
	return new Form(backing, backing.getSet());
}

static Stream<Arguments> forms() {
	return Stream.of(
			arguments(named("negative", (RangeFactory) OEIntSetListerTest::negativeRange)),
			arguments(named("positive", (RangeFactory) OEIntSetListerTest::positiveRange)));
}


/**
 * Reads the set through every observable the interface offers and asserts they tell the same story,
 * then hands back that one story for the caller to compare against the reference.
 * */
private static List<Integer> snapshot(OE_IntSet set) {
	List<Integer> byGet = new ArrayList<>();
	for(int i = 0; i < set.size(); i++) { byGet.add(set.get(i)); }
	
	List<Integer> forward = new ArrayList<>();
	OE_IntSet.IntListerIterator iterator = set.iterator();
	while(iterator.hasNext()) { forward.add(iterator.next()); }
	
	// the same iterator, now walked back to the start
	List<Integer> backward = new ArrayList<>();
	while(iterator.hasPrevious()) { backward.add(iterator.previous()); }
	Collections.reverse(backward);
	
	assertThat(forward).as("iteration order matches get()").isEqualTo(byGet);
	assertThat(backward).as("backward iteration matches get()").isEqualTo(byGet);
	assertThat(set.getIntList()).as("getIntList() matches get()").isEqualTo(byGet);
	assertThat(boxed(set.toList())).as("toList() matches get()").isEqualTo(byGet);
	assertThat(set.size()).as("size() matches get()").isEqualTo(byGet.size());
	return byGet;
}

/**
 * Widens a fastutil list to the plain one it already is. {@code IntArrayList} is both a
 * {@code List<Integer>} and a {@code Comparable}, which leaves AssertJ's assertThat ambiguous over
 * it, so every assertion on int storage goes through here.
 * */
private static List<Integer> boxed(IntArrayList ints) {
	return ints;
}

private static TreeSet<Integer> reference(int firstIndex, int lastIndex) {
	TreeSet<Integer> elements = new TreeSet<>();
	for(int value = firstIndex; value <= lastIndex; value++) { elements.add(value); }
	return elements;
}


@ParameterizedTest(name = "{0}")
@MethodSource("forms")
public void testConstructor_RangeIsInclusiveAtBothEnds(RangeFactory factory) {
	assertThat(factory.of(2, 5).set.getIntList()).containsExactly(2, 3, 4, 5);
	assertThat(factory.of(-3, 0).set.getIntList()).containsExactly(-3, -2, -1, 0);
	assertThat(factory.of(7, 7).set.getIntList()).containsExactly(7);
}

@ParameterizedTest(name = "{0}")
@MethodSource("forms")
public void testConstructor_DegenerateRangeIsEmptyAndStaysUsable(RangeFactory factory) {
	// A backwards range is one the caller never meant, but count() reads the bounds arithmetically,
	// so it has to normalise to the empty set rather than to a negative size.
	for(int[] range : new int[][] {{5, 4}, {5, 0}, {0, -1}, {0, -9}, {-3, -20}}) {
		Form form = factory.of(range[0], range[1]);
		assertThat(form.set.size()).as("size of range %s", java.util.Arrays.toString(range)).isZero();
		assertThat(snapshot(form.set)).isEmpty();
		assertThat(form.first()).as("empty bounds meet").isEqualTo(form.end());
		
		// and the empty set has to accept elements again
		assertThat(form.set.add(3)).isTrue();
		assertThat(form.set.add(3)).isFalse();
		assertThat(snapshot(form.set)).containsExactly(3);
		form.assertInvariants();
	}
}

@ParameterizedTest(name = "{0}")
@MethodSource("forms")
public void testAddAndRemove_MatchTreeSetThroughEveryObservable(RangeFactory factory) {
	Random random = new Random(SEED);
	for(int round = 0; round < ROUNDS; round++) {
		int firstIndex = random.nextInt(40) - 20;
		int lastIndex = firstIndex + random.nextInt(40) - 5;   // sometimes backwards, so empty
		Form form = factory.of(firstIndex, lastIndex);
		TreeSet<Integer> reference = reference(firstIndex, lastIndex);
		assertThat(snapshot(form.set)).as("after construction").isEqualTo(new ArrayList<>(reference));
		
		for(int operation = 0; operation < OPERATIONS_PER_ROUND; operation++) {
			int value = random.nextInt(60) - 30;   // straddles the range on both sides
			boolean adding = random.nextBoolean();
			boolean changed = adding ? reference.add(value) : reference.remove(value);
			
			assertThat(adding ? form.set.add(value) : form.set.remove(value))
					.as("%s %d on %s", adding ? "add" : "remove", value, form)
					.isEqualTo(changed);
			assertThat(snapshot(form.set)).as("contents of %s", form).isEqualTo(new ArrayList<>(reference));
			form.assertInvariants();
			
			if(reference.isEmpty()) { assertThat(form.first()).as("empty bounds meet").isEqualTo(form.end()); }
			else {
				assertThat(form.first()).as("smallest element of %s", form).isEqualTo(reference.first());
				assertThat(form.end()).as("one past the largest element of %s", form).isEqualTo(reference.last() + 1);
			}
		}
	}
}

@ParameterizedTest(name = "{0}")
@MethodSource("forms")
public void testIteratorRemove_DropsEveryOtherElementGoingForward(RangeFactory factory) {
	Form form = factory.of(10, 25);
	List<Integer> expected = new ArrayList<>();
	
	OE_IntSet.IntListerIterator iterator = form.set.iterator();
	for(int i = 0; iterator.hasNext(); i++) {
		int value = iterator.next();
		if(i % 2 == 0) { iterator.remove(); }
		else { expected.add(value); }
	}
	
	assertThat(snapshot(form.set)).isEqualTo(expected);
	form.assertInvariants();
}

@ParameterizedTest(name = "{0}")
@MethodSource("forms")
public void testIteratorRemove_EmptiesTheSetGoingBackward(RangeFactory factory) {
	Form form = factory.of(-4, 6);
	form.set.remove(0);   // a hole to walk over on the way back
	
	OE_IntSet.IntListerIterator iterator = form.set.iterator();
	while(iterator.hasNext()) { iterator.next(); }
	while(iterator.hasPrevious()) {
		iterator.previous();
		iterator.remove();
	}
	
	assertThat(snapshot(form.set)).isEmpty();
	form.assertInvariants();
}

@ParameterizedTest(name = "{0}")
@MethodSource("forms")
public void testIteratorRemove_WithoutAPrecedingMoveThrows(RangeFactory factory) {
	OE_IntSet.IntListerIterator iterator = factory.of(0, 3).set.iterator();
	assertThatThrownBy(iterator::remove).isInstanceOf(IllegalStateException.class);
	
	iterator.next();
	iterator.remove();
	assertThatThrownBy(iterator::remove).as("a second remove without a move").isInstanceOf(IllegalStateException.class);
}

@ParameterizedTest(name = "{0}")
@MethodSource("forms")
public void testIterator_WalkingPastEitherEndThrows(RangeFactory factory) {
	OE_IntSet.IntListerIterator iterator = factory.of(0, 1).set.iterator();
	assertThatThrownBy(iterator::previous).isInstanceOf(NoSuchElementException.class);
	
	iterator.next();
	iterator.next();
	assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
}

@ParameterizedTest(name = "{0}")
@MethodSource("forms")
public void testGet_OutsideTheSetThrows(RangeFactory factory) {
	OE_IntSet set = factory.of(0, 1).set;
	assertThatThrownBy(() -> set.get(-1)).isInstanceOf(IndexOutOfBoundsException.class);
	assertThatThrownBy(() -> set.get(2)).isInstanceOf(IndexOutOfBoundsException.class);
	assertThatThrownBy(() -> factory.of(0, -1).set.get(0)).as("on an empty set").isInstanceOf(IndexOutOfBoundsException.class);
}

@ParameterizedTest(name = "{0}")
@MethodSource("forms")
public void testToList_IsASnapshotDetachedFromTheStorage(RangeFactory factory) {
	OE_IntSet set = factory.of(0, 4).set;
	IntArrayList ints = set.toList();
	List<Integer> integers = set.getIntList();
	
	set.remove(2);
	
	assertThat(boxed(ints)).containsExactly(0, 1, 2, 3, 4);
	assertThat(integers).containsExactly(0, 1, 2, 3, 4);
	assertThatThrownBy(() -> integers.add(9)).as("getIntList() is unmodifiable").isInstanceOf(UnsupportedOperationException.class);
}

@Test
public void testNegative_StoresADenseRangeInNothingButItsBounds() {
	IntSetListerNegative backing = new IntSetListerNegative(0, 100_000);
	assertThat(backing.getSet().size()).isEqualTo(100_001);
	assertThat(boxed(backing.exclusionList)).as("a full range costs one pair of ints").isEmpty();
	
	backing.getSet().remove(500);
	assertThat(boxed(backing.exclusionList)).as("a removal is what costs storage here").containsExactly(500);
}

@Test
public void testPositive_HandlesASparseWorkloadTheNegativeFormCouldNot() {
	// The negative form would have to materialise every integer between these as a hole, which is
	// the whole reason the positive form exists.
	IntSetListerPositive backing = new IntSetListerPositive();
	OE_IntSet set = backing.getSet();
	TreeSet<Integer> reference = new TreeSet<>();
	Random random = new Random(SEED);
	
	assertThat(set.size()).isZero();
	assertThat(backing.getFirst()).isEqualTo(backing.getEnd());
	
	for(int operation = 0; operation < 4000; operation++) {
		int value = random.nextInt(2_000_000) - 1_000_000;
		boolean adding = random.nextInt(3) > 0;
		assertThat(adding ? set.add(value) : set.remove(value)).isEqualTo(adding ? reference.add(value) : reference.remove(value));
	}
	
	assertThat(set.getIntList()).isEqualTo(new ArrayList<>(reference));
	assertThat(backing.inclusionList.size()).as("storage is the set itself").isEqualTo(reference.size());
}

// ---------------------------------------------------------------- set algebra

/** Builds one form holding exactly {@code values}, so a test can pick the representation. */
static OE_IntSet of(String form, Collection<Integer> values) {
	if(form.equals("negative")) {
		IntSetListerNegative backing = new IntSetListerNegative(0, -1);
		OE_IntSet set = backing.getSet();
		for(int v : values) { set.add(v); }
		return set;
	}
	IntSetListerPositive backing = new IntSetListerPositive();
	OE_IntSet set = backing.getSet();
	for(int v : values) { set.add(v); }
	return set;
}

/** Both representations paired every way round, since the algebra crosses them. */
static Stream<Arguments> formPairs() {
	return Stream.of(
			arguments("negative", "negative"), arguments("negative", "positive"),
			arguments("positive", "negative"), arguments("positive", "positive"));
}

@ParameterizedTest(name = "{0} vs {1}")
@MethodSource("formPairs")
public void testContains_AgreesWithTheReference(String leftForm, String rightForm) {
	Random random = new Random(SEED);
	TreeSet<Integer> reference = new TreeSet<>();
	for(int i = 0; i < 400; i++) { reference.add(random.nextInt(900)); }
	OE_IntSet set = of(leftForm, reference);
	
	for(int value = -20; value < 920; value++) {
		assertThat(set.contains(value)).as("contains(%d) in %s form", value, leftForm).isEqualTo(reference.contains(value));
	}
}

@ParameterizedTest(name = "{0} vs {1}")
@MethodSource("formPairs")
public void testAlgebra_MatchesTreeSetAcrossEveryPairing(String leftForm, String rightForm) {
	Random random = new Random(SEED);
	for(int round = 0; round < 40; round++) {
		TreeSet<Integer> leftRef = new TreeSet<>(), rightRef = new TreeSet<>();
		// deliberately overlapping ranges, so the sets share some elements and not others
		for(int i = 0; i < 1 + random.nextInt(120); i++) { leftRef.add(random.nextInt(300)); }
		for(int i = 0; i < 1 + random.nextInt(120); i++) { rightRef.add(random.nextInt(300) + 100); }
		OE_IntSet left = of(leftForm, leftRef), right = of(rightForm, rightRef);
		
		TreeSet<Integer> expectedShared = new TreeSet<>(leftRef);
		expectedShared.retainAll(rightRef);
		TreeSet<Integer> expectedOnly = new TreeSet<>(leftRef);
		expectedOnly.removeAll(rightRef);
		
		assertThat(boxed(left.intersect(right))).as("intersect").isEqualTo(new ArrayList<>(expectedShared));
		assertThat(left.intersectionCount(right)).as("intersectionCount").isEqualTo(expectedShared.size());
		assertThat(boxed(left.difference(right))).as("difference").isEqualTo(new ArrayList<>(expectedOnly));
		assertThat(left.containsAll(right)).as("containsAll").isEqualTo(leftRef.containsAll(rightRef));
		
		// intersection is symmetric even though the implementation drives from the smaller side
		assertThat(boxed(right.intersect(left))).as("intersect is symmetric").isEqualTo(new ArrayList<>(expectedShared));
		assertThat(right.intersectionCount(left)).as("count is symmetric").isEqualTo(expectedShared.size());
	}
}

@ParameterizedTest(name = "{0} vs {1}")
@MethodSource("formPairs")
public void testMutatingAlgebra_LeavesTheSameSetAsTheReference(String leftForm, String rightForm) {
	Random random = new Random(SEED);
	for(int round = 0; round < 30; round++) {
		TreeSet<Integer> leftRef = new TreeSet<>(), rightRef = new TreeSet<>();
		for(int i = 0; i < 1 + random.nextInt(80); i++) { leftRef.add(random.nextInt(200)); }
		for(int i = 0; i < 1 + random.nextInt(80); i++) { rightRef.add(random.nextInt(200) + 50); }
		OE_IntSet right = of(rightForm, rightRef);
		
		OE_IntSet retained = of(leftForm, leftRef);
		TreeSet<Integer> retainedRef = new TreeSet<>(leftRef);
		assertThat(retained.retainAll(right)).as("retainAll reports change").isEqualTo(retainedRef.retainAll(rightRef));
		assertThat(retained.getIntList()).as("retainAll result").isEqualTo(new ArrayList<>(retainedRef));
		
		OE_IntSet removed = of(leftForm, leftRef);
		TreeSet<Integer> removedRef = new TreeSet<>(leftRef);
		assertThat(removed.removeAll(right)).as("removeAll reports change").isEqualTo(removedRef.removeAll(rightRef));
		assertThat(removed.getIntList()).as("removeAll result").isEqualTo(new ArrayList<>(removedRef));
		
		OE_IntSet added = of(leftForm, leftRef);
		TreeSet<Integer> addedRef = new TreeSet<>(leftRef);
		assertThat(added.addAll(right)).as("addAll reports change").isEqualTo(addedRef.addAll(rightRef));
		assertThat(added.getIntList()).as("addAll result").isEqualTo(new ArrayList<>(addedRef));
	}
}

@ParameterizedTest(name = "{0} vs {1}")
@MethodSource("formPairs")
public void testAlgebra_HandlesEmptyAndDisjointOperands(String leftForm, String rightForm) {
	OE_IntSet empty = of(leftForm, List.of());
	OE_IntSet full = of(rightForm, List.of(1, 2, 3));
	
	assertThat(empty.isEmpty()).isTrue();
	assertThat(boxed(empty.intersect(full))).isEmpty();
	assertThat(boxed(full.intersect(empty))).isEmpty();
	assertThat(empty.intersectionCount(full)).isZero();
	assertThat(boxed(empty.difference(full))).isEmpty();
	assertThat(boxed(full.difference(empty))).containsExactly(1, 2, 3);
	assertThat(empty.containsAll(full)).isFalse();
	assertThat(full.containsAll(empty)).as("everything contains the empty set").isTrue();
	assertThat(empty.retainAll(full)).as("no change retaining against anything").isFalse();
	
	OE_IntSet disjoint = of(rightForm, List.of(90, 91));
	OE_IntSet left = of(leftForm, List.of(1, 2, 3));
	assertThat(boxed(left.intersect(disjoint))).isEmpty();
	assertThat(left.removeAll(disjoint)).as("removing a disjoint set changes nothing").isFalse();
	assertThat(left.retainAll(disjoint)).as("retaining a disjoint set empties this one").isTrue();
	assertThat(left.isEmpty()).isTrue();
}


}
