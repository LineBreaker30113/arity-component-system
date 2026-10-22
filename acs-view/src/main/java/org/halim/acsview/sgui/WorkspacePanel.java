package org.halim.acsview.sgui;

import org.halim.acsview.sgui.visual.ConsoleView;
import org.halim.acsview.sgui.visual.DataGridView;
import org.halim.acsview.sgui.visual.JournalView;
import org.halim.acsview.sgui.visual.NavigatorView;
import org.halim.acsview.sgui.visual.WorkPanelWrapper;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;


/** The views side by side, each shown, hidden and moved by the sidebar's toggles and its own arrows, as in OntoDirectory's WorkspacePanel. */
public class WorkspacePanel extends JPanel {

public static final String navigatorVN = "navigatorV", gridVN = "gridV", consoleVN = "consoleV", journalVN = "journalV";

final ArrayList<WorkPanelWrapper> allPanels = new ArrayList<>();
final ArrayList<WorkPanelWrapper> activePanels = new ArrayList<>();
public final GUI_RootPanel owner;

public WorkspacePanel(GUI_RootPanel owner) {
	this.owner = owner;
	setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
	SwingUtilities.invokeLater(this::initViews);
}

void initViews() {
	ApplicationController app = owner.appController;
	NavigatorView navigator = new NavigatorView(app);
	DataGridView grid = new DataGridView(app);
	ConsoleView console = new ConsoleView(app);
	JournalView journal = new JournalView(app);

	WorkspaceController wsc = app.wsModeller;
	wsc.registerListener(navigator);
	wsc.registerListener(grid);
	wsc.registerListener(console);
	wsc.registerListener(journal);

	allPanels.add(new WorkPanelWrapper(navigatorVN, "Navigator", navigator, this));
	allPanels.add(new WorkPanelWrapper(gridVN, "Data Grid", grid, this));
	allPanels.add(new WorkPanelWrapper(consoleVN, "Request Console", console, this));
	allPanels.add(new WorkPanelWrapper(journalVN, "Journal", journal, this));

	panelToggled(navigatorVN);
	panelToggled(gridVN);
}

public void closePanel(WorkPanelWrapper wrapper) {
	activePanels.remove(wrapper);
	renderPanels();
}

public void reopenPanel(WorkPanelWrapper wrapper) {
	activePanels.add(wrapper);
	renderPanels();
}

public void panelToggled(String pname) {
	WorkPanelWrapper wrapper = allPanels.stream().filter(w -> w.title.equals(pname)).findFirst().orElse(null);
	if(wrapper == null) { return; }
	if(activePanels.contains(wrapper)) { closePanel(wrapper); } else { reopenPanel(wrapper); }
}

/** Moves a view left (-1) or right (1). */
public void shiftPanel(WorkPanelWrapper wrapper, int direction) {
	int currentIndex = activePanels.indexOf(wrapper);
	int newIndex = currentIndex + direction;
	if(currentIndex < 0 || newIndex < 0 || newIndex >= activePanels.size()) { return; }
	Collections.swap(activePanels, currentIndex, newIndex);
	renderPanels();
}

void renderPanels() {
	removeAll();
	for(WorkPanelWrapper wrapper : activePanels) { add(wrapper); }
	revalidate();
	repaint();
}

}
