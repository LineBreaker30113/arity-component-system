package org.halim.nStructure.sgui.visual;

import org.halim.nStructure.sgui.LeftSidebar;
import org.halim.nStructure.sgui.sglib.Utilities;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class AnchorSection extends JPanel {

private final LeftSidebar leftSidebar;
private final ArrayList<JButton> buttons = new ArrayList<>();

public AnchorSection(LeftSidebar leftSidebar) {
	this.leftSidebar = leftSidebar;
	setBackground(leftSidebar.getBackground().brighter());
	setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	
	JButton collapseBtn = registerButton("Toggle Sidebar", "arrow-line-left.svg", "caret-right.svg");
	collapseBtn.addActionListener(e -> leftSidebar.toggleCollapse());
	
	JButton welcomeBtn = registerButton("Welcome Page", "house.svg", "house.svg");
	welcomeBtn.addActionListener(a -> leftSidebar.owner.centerPanel.showPage("WELCOME"));
	
	JButton instructionsBtn = registerButton("Instruction Manual", "notebook.svg", "notebook.svg");
	instructionsBtn.addActionListener(a -> leftSidebar.owner.centerPanel.showPage("INSTRUCTIONS"));
	
	add(collapseBtn);
	add(Box.createVerticalStrut(8));
	add(welcomeBtn);
	add(Box.createVerticalStrut(8));
	add(instructionsBtn);
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