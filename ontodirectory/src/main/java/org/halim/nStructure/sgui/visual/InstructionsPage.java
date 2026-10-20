package org.halim.nStructure.sgui.visual;

import org.halim.nStructure.sgui.sglib.ManualParser.ManualEntry;
import org.halim.nStructure.sgui.sglib.AccordionPanel;
import org.halim.nStructure.sgui.sglib.ManualParser;
import org.halim.nStructure.sgui.sglib.Utilities;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class InstructionsPage extends JPanel {

public InstructionsPage() {
	setLayout(new BorderLayout());
	setBackground(Utilities.WP_BG);
	
	JPanel contentPanel = new JPanel();
	contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
	contentPanel.setBackground(Utilities.WP_BG);
	contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));
	
	// Header
	JLabel titleLabel = new JLabel("Onto Directory Manual");
	titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 32f));
	titleLabel.setForeground(Utilities.ELEMENT_TEXT_PRIMARY);
	titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
	contentPanel.add(titleLabel);
	
	JLabel subtitleLabel = new JLabel("Scan the topics below. Expand any section for a deep dive into the mechanics.");
	subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(18f));
	subtitleLabel.setForeground(Utilities.ELEMENT_TEXT_SECONDARY);
	subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
	contentPanel.add(subtitleLabel);
	
	contentPanel.add(Box.createVerticalStrut(40));
	
	// --- DYNAMICALLY LOAD ACCORDIONS ---
	List<ManualEntry> entries = ManualParser.loadManual();
	
	for (ManualEntry entry : entries) {
		contentPanel.add(new AccordionPanel(entry.title, entry.summary, entry.details));
		// Add a small spacer between accordions unless it's the last one
		contentPanel.add(Box.createVerticalStrut(10));
	}
	
	contentPanel.add(Box.createVerticalGlue());
	
	JScrollPane scrollPane = new JScrollPane(contentPanel);
	scrollPane.setBorder(null);
	scrollPane.getViewport().setBackground(Utilities.WP_BG);
	scrollPane.getVerticalScrollBar().setUnitIncrement(16);
	
	add(scrollPane, BorderLayout.CENTER);
}
}