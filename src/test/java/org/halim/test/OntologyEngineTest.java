package org.halim.test;

import org.halim.ontology.core.OntologyEngine;
import org.halim.ontology.core.segments.data.OntologyCatalogDetailStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OntologyEngineTest {

@Test
public void testDefaultConstructor_NamesRootClassFile() {
    OntologyEngine engine = new OntologyEngine();
    assertThat(engine.getClassDataService().getName(0)).isEqualTo("File");
}

@Test
public void testTwoArgConstructor_NamesRootClassFile() {
    OntologyEngine engine = new OntologyEngine(new OntologyCatalogDetailStore(), new OntologyCatalogDetailStore());
    assertThat(engine.getClassDataService().getName(0)).isEqualTo("File");
}
}
