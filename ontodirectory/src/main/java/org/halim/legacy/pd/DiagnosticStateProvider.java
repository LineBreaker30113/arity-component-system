package org.halim.legacy.pd;

public interface DiagnosticStateProvider {
/** Identifies the layer (e.g., "GUI_ADAPTER", "DOMAIN_PORT", "CORE_DAG") */
String getLayerName();

/** Returns a formatted string of the exact variables and state at this moment */
String captureStateDump();
}
