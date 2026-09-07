package org.halim.test;

import org.halim.ontology.legacy.core.segments.data.OntologyCatalogDetailStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class OntologyCatalogDetailStoreTest {

private OntologyCatalogDetailStore store;

@BeforeEach
public void setup() {
	store = new OntologyCatalogDetailStore();
}

@Test
public void testUnsetObjectDetail_ReturnsNull() {
	assertThat(store.getDetailObject(5, 0)).isNull();
}

@Test
public void testSetObjectDetail_ThenGet_RoundTrips() {
	store.setDetailObject(3, 0, "Documents");
	assertThat(store.getDetailObject(3, 0)).isEqualTo("Documents");
}

// The default store is names-only (Object-typed); it deliberately doesn't implement the
// unboxed-primitive detail types that a custom AnyCatalogBase would use for some other detail.

@Test
public void testGetDetailLong_ThrowsUnsupported() {
	assertThatThrownBy(() -> store.getDetailLong(5, 0)).isInstanceOf(UnsupportedOperationException.class);
}

@Test
public void testGetDetailDouble_ThrowsUnsupported() {
	assertThatThrownBy(() -> store.getDetailDouble(5, 0)).isInstanceOf(UnsupportedOperationException.class);
}

@Test
public void testGetDetailBoolean_ThrowsUnsupported() {
	assertThatThrownBy(() -> store.getDetailBoolean(5, 0)).isInstanceOf(UnsupportedOperationException.class);
}

@Test
public void testSetDetailLong_ThrowsUnsupported() {
	assertThatThrownBy(() -> store.setDetailLong(3, 1, 42L)).isInstanceOf(UnsupportedOperationException.class);
}

@Test
public void testSetDetailDouble_ThrowsUnsupported() {
	assertThatThrownBy(() -> store.setDetailDouble(3, 2, 3.5)).isInstanceOf(UnsupportedOperationException.class);
}

@Test
public void testSetDetailBoolean_ThrowsUnsupported() {
	assertThatThrownBy(() -> store.setDetailBoolean(3, 3, true)).isInstanceOf(UnsupportedOperationException.class);
}

@Test
public void testGetDetailObject_NonZeroDetailTypeIdentity_ThrowsUnsupported() {
	// The store holds exactly one row (names, detailTypeIdentity 0) -- any other row is rejected
	// rather than silently read.
	assertThatThrownBy(() -> store.getDetailObject(1, 1)).isInstanceOf(UnsupportedOperationException.class);
}

@Test
public void testSetDetailObject_NonZeroDetailTypeIdentity_ThrowsUnsupported() {
	assertThatThrownBy(() -> store.setDetailObject(1, 1, "value")).isInstanceOf(UnsupportedOperationException.class);
}

@Test
public void testEntriesAreIndependentColumnsWithinTheRow_SettingOneDoesNotDisturbAnother() {
	store.setDetailObject(0, 0, "root");
	store.setDetailObject(5, 0, "far-away-entity");

	assertThat(store.getDetailObject(0, 0)).isEqualTo("root");
	assertThat(store.getDetailObject(5, 0)).isEqualTo("far-away-entity");
	assertThat(store.getDetailObject(2, 0)).isNull();
}

}
