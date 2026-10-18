package org.halim.test;

import org.halim.ontology.legacy.core.TransactionContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression coverage for {@link TransactionContext}'s nesting-awareness (only the outermost
 * begin/commit/rollback in a call chain actually starts, finalizes, or reverts a transaction) and
 * for {@link TransactionContext#rollback()} actually replaying recorded undo closures instead of
 * just discarding them.
 */
public class TransactionContextNestingTest {

@Test
public void testNestedBeginCommit_CollapsesIntoOneTransaction() {
	TransactionContext.begin("outer");
	TransactionContext.record(() -> { }, () -> { });

	TransactionContext.begin("inner");
	TransactionContext.record(() -> { }, () -> { });
	TransactionContext.Transaction innerResult = TransactionContext.commit();
	assertThat(innerResult).isNull(); // nested commit is a no-op passthrough, not a separate transaction

	TransactionContext.Transaction outerResult = TransactionContext.commit();
	assertThat(outerResult).isNotNull();
	assertThat(outerResult.actionName).isEqualTo("outer");
	assertThat(outerResult.undoActions).hasSize(2); // both the outer and inner recordings landed here
}

@Test
public void testNestedBeginThenThrow_OutermostRollbackReplaysAllRecordedUndoActionsLifo() {
	List<String> applied = new ArrayList<>();

	TransactionContext.begin("outer");
	TransactionContext.record(() -> applied.remove("step-1"), () -> applied.add("step-1"));
	applied.add("step-1");

	try {
		TransactionContext.begin("inner");
		try {
			TransactionContext.record(() -> applied.remove("step-2"), () -> applied.add("step-2"));
			applied.add("step-2");
			throw new RuntimeException("boom");
		} catch (RuntimeException e) {
			TransactionContext.rollback(); // depth 2 -> 1: nested, no replay yet
			throw e;
		}
	} catch (RuntimeException e) {
		TransactionContext.rollback(); // depth 1 -> 0: outermost, replays both undo actions LIFO
	}

	assertThat(applied).isEmpty();
}

@Test
public void testCommit_WithNothingRecorded_ReturnsNull() {
	TransactionContext.begin("empty");
	TransactionContext.Transaction result = TransactionContext.commit();
	assertThat(result).isNull();
}

}
