package org.halim.nStructure.sgui.sglib;

import javax.swing.*;
import java.awt.*;

/** UI UX Component: A modern, toggleable accordion block for Progressive Disclosure */
public class AccordionPanel extends JPanel {

private final JPanel detailsPanel;
private final JButton toggleButton;
private boolean isExpanded = false;

public AccordionPanel(String title, String summary, String htmlDetails) {
	setLayout(new BorderLayout());
	setOpaque(false);
	setBorder(BorderFactory.createCompoundBorder(
		  BorderFactory.createMatteBorder(0, 0, 1, 0, Utilities.LIST_HEADER_BG), // Subtle bottom separator
		  BorderFactory.createEmptyBorder(10, 0, 15, 0)
	));
	
	// Prevent it from stretching infinitely in a vertical BoxLayout
	setMaximumSize(new Dimension(850, Integer.MAX_VALUE));
	setAlignmentX(Component.LEFT_ALIGNMENT);
	
	// --- ALWAYS VISIBLE (Header & Summary) ---
	JPanel headerPanel = new JPanel();
	headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
	headerPanel.setOpaque(false);
	
	JLabel titleLabel = new JLabel(title);
	titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
	titleLabel.setForeground(Utilities.ELEMENT_TEXT_PRIMARY);
	titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
	
	// The Summary: Always visible, wraps nicely
	JLabel summaryLabel = new JLabel("<html><div style='width: 550px; padding-top: 6px; line-height: 1.3;'>" + summary + "</div></html>");
	summaryLabel.setForeground(Utilities.ELEMENT_TEXT_SECONDARY);
	summaryLabel.setFont(summaryLabel.getFont().deriveFont(16f));
	summaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
	
	// The Toggle Button: Now acts as the gatekeeper for the deep dive
	toggleButton = new JButton("▶ Read Details");
	Utilities.initButton(toggleButton);
	toggleButton.setHorizontalAlignment(SwingConstants.LEFT);
	toggleButton.setFont(toggleButton.getFont().deriveFont(Font.BOLD, 12f));
	toggleButton.setForeground(Utilities.sideButtonBG); // Use your accent color for interactivity
	toggleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
	toggleButton.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
	toggleButton.setAlignmentX(Component.LEFT_ALIGNMENT);
	
	headerPanel.add(titleLabel);
	headerPanel.add(summaryLabel);
	headerPanel.add(toggleButton);
	
	// --- HIDDEN DETAILS (The Deep Dive) ---
	detailsPanel = new JPanel(new BorderLayout());
	detailsPanel.setOpaque(false);
	detailsPanel.setVisible(false); // Hidden by default
	detailsPanel.setBorder(BorderFactory.createEmptyBorder(12, 10, 0, 0)); // Indent to show hierarchy
	
	// Uses a CSS left-border to visually separate the deep-dive text from the summary
	JLabel detailsLabel = new JLabel("<html><div style='width: 520px; color: #A0A0A0; border-left: 3px solid #505050; padding-left: 12px; line-height: 1.4;'>" + htmlDetails + "</div></html>");
	detailsLabel.setFont(detailsLabel.getFont().deriveFont(16f));
	detailsPanel.add(detailsLabel, BorderLayout.CENTER);
	
	// --- THE INTERACTION LOGIC ---
	toggleButton.addActionListener(e -> {
		isExpanded = !isExpanded;
		detailsPanel.setVisible(isExpanded);
		toggleButton.setText(isExpanded ? "▼ Hide Details" : "▶ Read Details");
		
		// Force the parent to recalculate layout seamlessly
		revalidate();
		repaint();
		
		Container parent = getParent();
		if (parent instanceof JComponent) {
			((JComponent) parent).revalidate();
			parent.repaint();
		}
	});
	
	add(headerPanel, BorderLayout.NORTH);
	add(detailsPanel, BorderLayout.CENTER);
}
}