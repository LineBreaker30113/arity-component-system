package org.halim.acsview.sgui.visual;

import org.halim.acsview.sgui.LeftSidebar;
import org.halim.acsview.sgui.WorkspacePanel;
import org.halim.acsview.sgui.sglib.Utilities;

import javax.swing.*;
import java.util.ArrayList;


/** The toggles of the workspace's views. */
public class WorkspaceSection extends JPanel {

final ArrayList<JButton> buttons = new ArrayList<>();

public WorkspaceSection(LeftSidebar leftSidebar) {
	setBackground(leftSidebar.getBackground().brighter());
	setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	toggle(leftSidebar, "Navigator", "tree-view.svg", WorkspacePanel.navigatorVN);
	toggle(leftSidebar, "Data Grid", "files.svg", WorkspacePanel.gridVN);
	toggle(leftSidebar, "Request Console", "note.svg", WorkspacePanel.consoleVN);
	toggle(leftSidebar, "Journal", "notebook.svg", WorkspacePanel.journalVN);
}

void toggle(LeftSidebar leftSidebar, String name, String icon, String view) {
	JButton btn = Utilities.sidebarButton(name, icon, icon);
	btn.addActionListener(e -> leftSidebar.owner.workspacePanel.panelToggled(view));
	if(!buttons.isEmpty()) { add(Box.createVerticalStrut(8)); }
	buttons.add(btn);
	add(btn);
}

public void updateCollapseState(boolean isCollapsed) {
	for(JButton btn : buttons) { Utilities.updateCollapseState(btn, isCollapsed); }
}

}
