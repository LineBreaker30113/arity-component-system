/**
 * Manages and presents the nStructure application: window/panel orchestration, the active-lake and
 * active-domain "workspace" state, and wiring GUI events to {@code mhexagon.OntoDirectoryService}.
 * This package currently mixes concerns that should eventually separate -- orchestration ({@code
 * ApplicationController}, {@code WorkspaceController}), layout shells ({@code GUI_RootPanel},
 * {@code CenterPanel}, {@code LeftSidebar}, {@code WorkspacePanel}), and (in {@code sglib}/{@code
 * visual} below) reusable widgets and concrete views all currently reach into each other fairly
 * directly. A proper separation-of-concerns pass is deliberately future work, not done as part of
 * the current port -- don't take the current class boundaries as the intended final shape.
 * <p>
 * {@code ApplicationController} is the top-level Swing adapter (deploys the frame, owns the
 * debounced-save background thread); {@code WorkspaceController} tracks the active lake/domain and
 * fans out changes to {@code sglib.WorkspaceListener}s; {@code GUI_RootPanel}, {@code CenterPanel},
 * {@code LeftSidebar}, and {@code WorkspacePanel} are the layout shells those controllers drive.
 * {@link org.halim.nStructure.sgui.sglib} holds reusable widgets and parsing helpers shared across
 * views; {@link org.halim.nStructure.sgui.visual} holds the concrete, single-purpose view panels
 * those shells display.
 */
package org.halim.nStructure.sgui;
