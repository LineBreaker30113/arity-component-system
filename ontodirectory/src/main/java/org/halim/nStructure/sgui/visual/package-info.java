/**
 * Concrete, single-purpose view panels. {@code WelcomePage} and {@code InstructionsPage} are the
 * two top-level "no lake focused yet" screens; {@code FilesView} (a {@code sglib.ContentView}) and
 * {@code HierarchyTreeView} (a {@code sglib.HierarchyView}) are the two working views over a loaded
 * lake so far. {@code AnchorSection}, {@code WorkspaceSection}, and {@code LoadedLakesSection} are
 * the sidebar's three stacked sections; {@code WorkPanelWrapper} is the titled-frame chrome {@code
 * WorkspacePanel} wraps each view in.
 */
package org.halim.nStructure.sgui.visual;
