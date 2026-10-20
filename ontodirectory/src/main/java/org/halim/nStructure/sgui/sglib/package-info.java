/**
 * Reusable widgets and parsing helpers shared across the concrete views in {@code
 * org.halim.nStructure.sgui.visual}. {@code WorkSpaceViewPanel} (implementing {@code
 * WorkspaceListener}) and its two abstract subclasses {@code ContentView} (views of one selected
 * tag's contents) and {@code HierarchyView} (views of the overall class structure) are the base
 * types every concrete view extends. {@code ScrollableListPanel} and {@code AccordionPanel} are
 * generic layout widgets; {@code Utilities} is a grab-bag of shared colors/fonts/small static
 * helpers; {@code ManualParser} and {@code DailyTipParser} read {@code manual.md} and the "daily
 * tip" text out of resources at runtime.
 */
package org.halim.nStructure.sgui.sglib;
