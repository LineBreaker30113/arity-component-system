package org.halim.nStructure.sgui;

import org.halim.nStructure.sgui.sglib.WorkSpaceViewPanel;
import org.halim.nStructure.sgui.visual.FilesView;
import org.halim.nStructure.sgui.visual.HierarchyTreeView;
import org.halim.nStructure.sgui.visual.WorkPanelWrapper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;

public class WorkspacePanel extends JPanel {

// Relocated from Controller: View Identifiers for routing toggles
public static final String filesVN = "filesV", notesVN = "notesV",
	  treeVN = "treeV", graphVN = "graphV", vennVN = "vennV";
public static final String[] vnames = new String[]{ filesVN, notesVN, treeVN, graphVN, vennVN };

// Tracks the logical order of currently visible panels
private final ArrayList<WorkPanelWrapper> allPanels = new ArrayList<>();
private final ArrayList<WorkPanelWrapper> activePanels = new ArrayList<>();
public final GUI_RootPanel owner;

/** RSE Telemetry: Exposes the current visual state for the Diagnostic Matrix */
public java.util.List<String> getActiveViewNames() {
	return activePanels.stream().map(w -> w.title).toList();
}

public WorkspacePanel(GUI_RootPanel owner) {
	this.owner = owner;
	setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
	
	// Safely initialize views on the Event Dispatch Thread
	SwingUtilities.invokeLater(this::initViews);
}

private void initViews() {
	// 1. Instantiate the Views
	HierarchyTreeView treeView = new HierarchyTreeView(owner.appController);
	FilesView filesView = new FilesView(owner.appController);
	WorkSpaceViewPanel.ComingSoonPanel notesView = new WorkSpaceViewPanel.ComingSoonPanel();
	WorkSpaceViewPanel.ComingSoonPanel graphView = new WorkSpaceViewPanel.ComingSoonPanel();
	WorkSpaceViewPanel.ComingSoonPanel vennView = new WorkSpaceViewPanel.ComingSoonPanel();
	
	// 2. Register them to the Controller's Event Bus
	WorkspaceController wsc = owner.appController.wsModeller;
	wsc.registerListener(treeView);
	wsc.registerListener(filesView);
	wsc.registerListener(notesView);
	wsc.registerListener(graphView);
	wsc.registerListener(vennView);
	
	// 3. Wrap and store them in the master layout list
	allPanels.add(new WorkPanelWrapper(treeVN, treeView, this));
	allPanels.add(new WorkPanelWrapper(filesVN, filesView, this));
	allPanels.add(new WorkPanelWrapper(notesVN, notesView, this));
	allPanels.add(new WorkPanelWrapper(graphVN, graphView, this));
	allPanels.add(new WorkPanelWrapper(vennVN, vennView, this));
	
	// 4. Set default active views (Pinning Tree and Files)
	panelToggled(treeVN);
	panelToggled(filesVN);
}

/** Removes a panel from the active layout */
public void closePanel(WorkPanelWrapper wrapper) {
	activePanels.remove(wrapper);
	renderPanels();
}

/** Re-adds a panel to the active layout */
public void reopenPanel(WorkPanelWrapper wrapper) {
	activePanels.add(wrapper);
	renderPanels();
}

public void panelToggled(String pname) {
	org.halim.nStructure.mhexagon.CrashReporter.log("[GUI] WorkspacePanel requested view toggle: " + pname);
	// Safe stream filtering to prevent crashes if clicked during startup
	WorkPanelWrapper wrapper = allPanels.stream()
		  .filter(w -> w.title.equals(pname))
		  .findFirst()
		  .orElse(null);
	
	if (wrapper == null) return; // Panel not instantiated yet
	
	if (activePanels.contains(wrapper)) {
		closePanel(wrapper);
	} else {
		reopenPanel(wrapper);
	}
}

/** Shifts a panel left (-1) or right (1) */
public void shiftPanel(@NotNull WorkPanelWrapper wrapper, int direction) {
	org.halim.nStructure.mhexagon.CrashReporter.log("[GUI] WorkspacePanel shifted view: " + wrapper.title + " | Dir: " + direction);
	int currentIndex = activePanels.indexOf(wrapper);
	if (currentIndex == -1) return;
	
	int newIndex = currentIndex + direction;
	
	// Boundary checks
	if (newIndex < 0 || newIndex >= activePanels.size()) return;
	
	// Swap in the logical list
	Collections.swap(activePanels, currentIndex, newIndex);
	
	// Re-render the physical UI
	renderPanels();
}

/** Wipes the container and re-adds everything in the correct logical order */
private void renderPanels() {
	removeAll(); // Strip the Swing container
	for (WorkPanelWrapper wrapper : activePanels) {
		add(wrapper);
	}
	revalidate();
	repaint();
}
}