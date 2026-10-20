package org.halim.nStructure.sgui.visual;

import org.halim.nStructure.sgui.WorkspacePanel;
import org.halim.nStructure.sgui.sglib.Utilities;

import javax.swing.*;
import java.awt.*;

public class WorkPanelWrapper extends JPanel {

public final String title;
public final WorkspacePanel parentWorkspace;
public final JPanel content;
public final JPanel titlePanel;

public WorkPanelWrapper(String title, JPanel content, WorkspacePanel parentWorkspace) {
	this.title = title;
	this.content = content;
	this.parentWorkspace = parentWorkspace;
	this.titlePanel = new JPanel() {
	
	};
	
	setLayout(new BorderLayout());
	setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.DARK_GRAY)); // Right separator
	
	// Build the Header
	JPanel header = new JPanel(new BorderLayout());
	header.setBackground(new Color(35, 35, 40));
	header.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
	
	String niceTitle = switch (title) {
		case WorkspacePanel.treeVN -> "Hierarchy Tree";
		case WorkspacePanel.filesVN -> "File Explorer";
		case WorkspacePanel.notesVN -> "Notes & Attachments";
		case WorkspacePanel.graphVN -> "Directed Graph";
		case WorkspacePanel.vennVN -> "Venn Diagram";
		default -> title;
	};
	
	JLabel titleLabel = new JLabel(niceTitle, SwingConstants.CENTER);
	titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
	titleLabel.setForeground(new Color(255, 215, 0)); // Golden text
	header.add(titleLabel, BorderLayout.CENTER);
	
	// Build Controls
	JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
	controls.setOpaque(false);
	
	JButton leftBtn = new JButton("<"); // Replace with 16x16 icon later
	JButton rightBtn = new JButton(">"); // Replace with 16x16 icon later
	
	Utilities.initButton(leftBtn);
	Utilities.initButton(rightBtn);
	
	leftBtn.addActionListener(e -> parentWorkspace.shiftPanel(this, -1));
	rightBtn.addActionListener(e -> parentWorkspace.shiftPanel(this, 1));
	
	controls.add(leftBtn);
	controls.add(rightBtn);
	header.add(controls, BorderLayout.EAST);
	
	add(header, BorderLayout.NORTH);
	add(content, BorderLayout.CENTER);
}
}