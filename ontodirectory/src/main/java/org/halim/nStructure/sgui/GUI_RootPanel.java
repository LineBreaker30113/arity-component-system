package org.halim.nStructure.sgui;

import javax.swing.*;
import java.awt.*;

public class GUI_RootPanel extends JPanel {

public final LeftSidebar leftSidebar;
public final CenterPanel centerPanel;
public final WorkspacePanel workspacePanel;
public final ApplicationController appController; // The Controller orchestrates, not the Port

public GUI_RootPanel(ApplicationController appController) {
	this.appController = appController;
	setLayout(new BorderLayout());
	
	// WorkspacePanel must be instantiated before CenterPanel adds it
	workspacePanel = new WorkspacePanel(this);
	leftSidebar = new LeftSidebar(this);
	centerPanel = new CenterPanel(this);
	
	// Pin them to the layout
	add(leftSidebar, BorderLayout.WEST);
	add(centerPanel, BorderLayout.CENTER);
	
	// Global Hotkeys for Undo/Redo
	InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
	ActionMap am = getActionMap();
	
	im.put(KeyStroke.getKeyStroke("control Z"), "UndoAction");
	am.put("UndoAction", new AbstractAction() {
		@Override public void actionPerformed(java.awt.event.ActionEvent e) {
			var lake = appController.servicePort.getActiveDataLake();
			if (lake != null) { lake.getOntologyManagingService().undo(); appController.wsModeller.triggerLakeRefresh(lake); }
		}
	});
	
	im.put(KeyStroke.getKeyStroke("control Y"), "RedoAction");
	am.put("RedoAction", new AbstractAction() {
		@Override public void actionPerformed(java.awt.event.ActionEvent e) {
			var lake = appController.servicePort.getActiveDataLake();
			if (lake != null) { lake.getOntologyManagingService().redo(); appController.wsModeller.triggerLakeRefresh(lake); }
		}
	});
	
	centerPanel.add(workspacePanel, "WORKSPACE");
}
}