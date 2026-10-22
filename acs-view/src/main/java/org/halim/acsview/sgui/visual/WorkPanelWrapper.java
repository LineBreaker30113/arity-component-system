package org.halim.acsview.sgui.visual;

import org.halim.acsview.sgui.WorkspacePanel;
import org.halim.acsview.sgui.sglib.Utilities;

import javax.swing.*;
import java.awt.*;


/** A view with its header: its golden title and the arrows that move it, as in OntoDirectory. */
public class WorkPanelWrapper extends JPanel {

public final String title;
public final WorkspacePanel parentWorkspace;
public final JPanel content;

public WorkPanelWrapper(String title, String niceTitle, JPanel content, WorkspacePanel parentWorkspace) {
	this.title = title;
	this.content = content;
	this.parentWorkspace = parentWorkspace;
	setLayout(new BorderLayout());
	setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.DARK_GRAY));

	JPanel header = new JPanel(new BorderLayout());
	header.setBackground(new Color(35, 35, 40));
	header.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
	JLabel titleLabel = new JLabel(niceTitle, SwingConstants.CENTER);
	titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
	titleLabel.setForeground(Utilities.GOLDEN_COLOR);
	header.add(titleLabel, BorderLayout.CENTER);

	JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
	controls.setOpaque(false);
	JButton leftBtn = new JButton("<"), rightBtn = new JButton(">");
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
