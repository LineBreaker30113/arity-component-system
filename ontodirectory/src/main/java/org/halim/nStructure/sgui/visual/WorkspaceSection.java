package org.halim.nStructure.sgui.visual;

import org.halim.nStructure.sgui.LeftSidebar;
import org.halim.nStructure.sgui.WorkspacePanel;
import org.halim.nStructure.sgui.sglib.Utilities;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class WorkspaceSection extends JPanel {

private final LeftSidebar leftSidebar;
private final ArrayList<JButton> buttons = new ArrayList<>();

public WorkspaceSection(LeftSidebar leftSidebar) {
	this.leftSidebar = leftSidebar;
	setBackground(leftSidebar.getBackground().brighter());
	setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	
	JButton treeBtn = registerButton("Tree View", "tree-view.svg", "tree-view.svg");
	treeBtn.addActionListener(e -> leftSidebar.owner.workspacePanel.panelToggled(WorkspacePanel.treeVN));
	
	JButton filesBtn = registerButton("Files View", "files.svg", "files.svg");
	filesBtn.addActionListener(e -> leftSidebar.owner.workspacePanel.panelToggled(WorkspacePanel.filesVN));
	
	JButton notesBtn = registerButton("Notes View", "note.svg", "note.svg");
	notesBtn.addActionListener(e -> leftSidebar.owner.workspacePanel.panelToggled(WorkspacePanel.notesVN));
	
	JButton graphBtn = registerButton("Graph View", "graph.svg", "graph.svg");
	graphBtn.addActionListener(e -> leftSidebar.owner.workspacePanel.panelToggled(WorkspacePanel.graphVN));
	
	JButton vennBtn = registerButton("Venn View", "intersect-three.svg", "intersect-three.svg");
	vennBtn.addActionListener(e -> leftSidebar.owner.workspacePanel.panelToggled(WorkspacePanel.vennVN));
	
	add(treeBtn);
	add(Box.createVerticalStrut(8));
	add(filesBtn);
	add(Box.createVerticalStrut(8));
	add(notesBtn);
	add(Box.createVerticalStrut(8));
	add(graphBtn);
	add(Box.createVerticalStrut(8));
	add(vennBtn);
}

private JButton registerButton(String name, String expandedIconName, String collapsedIconName) {
	JButton btn = new JButton(name);
	btn.setActionCommand(name);
	btn.setAlignmentX(Component.RIGHT_ALIGNMENT);
	btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
	btn.setMinimumSize(new Dimension(56, 56));
	btn.setPreferredSize(new Dimension(56, 56));
	
	btn.setHorizontalAlignment(SwingConstants.RIGHT);
	btn.setHorizontalTextPosition(SwingConstants.LEFT);
	btn.setIconTextGap(8);
	
	Icon expandedIcon = Utilities.loadSVGIcon("icons/" + expandedIconName, 40, 40);
	Icon collapsedIcon = Utilities.loadSVGIcon("icons/" + collapsedIconName, 40, 40);
	
	if (expandedIcon != null) btn.setIcon(expandedIcon);
	
	btn.putClientProperty("expandedIcon", expandedIcon);
	btn.putClientProperty("collapsedIcon", collapsedIcon);
	
	Utilities.initButton(btn);
	buttons.add(btn);
	return btn;
}

public void updateCollapseState(boolean isCollapsed) {
	for (JButton btn : buttons) {
		if (isCollapsed) {
			btn.setText("");
			Icon cIcon = (Icon) btn.getClientProperty("collapsedIcon");
			if (cIcon != null) btn.setIcon(cIcon);
			btn.setHorizontalAlignment(SwingConstants.CENTER);
		} else {
			btn.setText(btn.getActionCommand());
			Icon eIcon = (Icon) btn.getClientProperty("expandedIcon");
			if (eIcon != null) btn.setIcon(eIcon);
			btn.setHorizontalAlignment(SwingConstants.TRAILING);
		}
	}
}
}