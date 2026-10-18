package org.halim.test;

import org.halim.ontology.legacy.core.OntologyEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regression coverage for {@code OntologyServiceFactory#runAtomically} through the real
 * {@link OntologyEngine} proxy layer: a composed body calling several different already-proxied
 * services must land as exactly one undo-stack entry, and a body that throws partway through must
 * leave state fully reverted and push nothing.
 */
public class OntologyServiceFactoryRunAtomicallyTest {

private OntologyEngine engine;

@BeforeEach
public void setup() {
	engine = new OntologyEngine();
}

@Test
public void testRunAtomically_MultipleServiceCalls_ProduceExactlyOneUndoEntry() {
	assertThat(engine.canUndo()).isFalse();

	int element = engine.runAtomically("COMBINED", () -> {
		int newElement = engine.getIndexerService().newElement();
		engine.getIndexerService().ensureConnection(0, newElement);
		return newElement;
	});

	assertThat(engine.getIndexerService().isConnected(0, element)).isTrue();
	assertThat(engine.canUndo()).isTrue();

	engine.undo(); // one call must revert both the connection AND the element creation

	assertThat(engine.canUndo()).isFalse();
	assertThat(engine.getIndexerService().getClassesCount(element)).isEqualTo(0);
}

@Test
public void testRunAtomically_ThrowsPartway_LeavesStateFullyRevertedAndPushesNothing() {
	assertThatThrownBy(() -> engine.runAtomically("FAILING", (Runnable) () -> {
		int newElement = engine.getIndexerService().newElement();
		engine.getIndexerService().ensureConnection(0, newElement);
		throw new IllegalStateException("simulated failure");
	})).isInstanceOf(IllegalStateException.class);

	assertThat(engine.canUndo()).isFalse();
}

@Test
public void testRunAtomically_NestedCall_StillProducesExactlyOneUndoEntry() {
	engine.runAtomically("OUTER", () -> {
		engine.getIndexerService().newElement();
		engine.runAtomically("INNER", () -> {
			engine.getIndexerService().newElement();
			return null;
		});
		return null;
	});

	assertThat(engine.canUndo()).isTrue();
	engine.undo();
	assertThat(engine.canUndo()).isFalse(); // a single undo() reverted both nested elements
}

}
