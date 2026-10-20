package org.halim.nStructure.sgui.visual;

import org.halim.nStructure.mhexagon.OntoDirectoryService;
import org.halim.nStructure.sgui.LeftSidebar;
import org.halim.nStructure.sgui.sglib.Utilities;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LoadedLakesSection extends JPanel {

private final LeftSidebar leftSidebar;
private final JPanel lakesContainer;
private final JLabel dataLakeHeaderLabel;
private final List<DataLakeHeaderTab> lakeItems = new ArrayList<>();
private final Component bottomGlue;

public LoadedLakesSection(LeftSidebar leftSidebar) {
	this.leftSidebar = leftSidebar;
	setBackground(leftSidebar.getBackground().darker());
	setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	
	JPanel headerPanel = new JPanel(new BorderLayout());
	headerPanel.setBackground(Color.BLACK);
	headerPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
	headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
	headerPanel.setPreferredSize(new Dimension(leftSidebar.expanded, 56));
	
	Icon lockersIcon = Utilities.loadSVGIcon("icons/lockers.svg", 40, 40, Utilities.GOLDEN_COLOR);
	dataLakeHeaderLabel = new JLabel("DATA LAKES", lockersIcon, SwingConstants.CENTER);
	dataLakeHeaderLabel.setForeground(Utilities.GOLDEN_COLOR);
	dataLakeHeaderLabel.setIconTextGap(8);
	dataLakeHeaderLabel.setHorizontalAlignment(SwingConstants.RIGHT);
	dataLakeHeaderLabel.setHorizontalTextPosition(SwingConstants.LEFT);
	headerPanel.add(dataLakeHeaderLabel, BorderLayout.EAST);
	
	lakesContainer = new JPanel();
	lakesContainer.setOpaque(false);
	lakesContainer.setLayout(new BoxLayout(lakesContainer, BoxLayout.Y_AXIS));
	lakesContainer.setAlignmentX(Component.RIGHT_ALIGNMENT);
	bottomGlue = Box.createVerticalGlue();
	lakesContainer.add(bottomGlue);
	
	add(headerPanel);
	add(Box.createVerticalStrut(8));
	add(lakesContainer);
}

public void registerDataLake(Path lakeRootPath) {
	for (DataLakeHeaderTab tab : lakeItems) if (tab.identity.equals(lakeRootPath)) return;
	DataLakeHeaderTab nlake = new DataLakeHeaderTab(this, lakeRootPath);
	lakeItems.add(nlake);
	lakesContainer.remove(bottomGlue);
	lakesContainer.add(nlake.container);
	lakesContainer.add(bottomGlue);
	revalidate(); repaint();
}

public void unsignDataLake(Path lakeRootPath) {
	DataLakeHeaderTab targetTab = lakeItems.stream().filter(t -> t.identity.equals(lakeRootPath)).findFirst().orElse(null);
	if (targetTab != null) {
		lakeItems.remove(targetTab);
		lakesContainer.remove(targetTab.container);
		revalidate(); repaint();
	}
}

public void updateCollapseState(boolean isCollapsed) {
	dataLakeHeaderLabel.setText(isCollapsed ? "" : "DATA LAKES");
	for (DataLakeHeaderTab item : lakeItems) {
		item.mainButton.setText(isCollapsed ? "" : item.mainButton.getActionCommand());
		item.mainButton.setIcon(isCollapsed ? (Icon) item.mainButton.getClientProperty("collapseIcon") : null);
		item.mainButton.setHorizontalAlignment(isCollapsed ? SwingConstants.CENTER : SwingConstants.LEFT);
		item.actionBar.setVisible(!isCollapsed);
	}
}

private static class DataLakeHeaderTab {
	public final LoadedLakesSection section;
	public final JPanel container;
	public final JButton mainButton;
	public final JPanel actionBar;
	public final Path identity;
	public final String lakeName;
	
	public DataLakeHeaderTab(LoadedLakesSection section, @NotNull Path lakeRootPath) {
		this.section = section;
		this.identity = lakeRootPath;
		this.lakeName = lakeRootPath.getFileName().toString();
		
		container = new JPanel();
		container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
		container.setOpaque(false);
		container.setAlignmentX(Component.RIGHT_ALIGNMENT);
		
		mainButton = new JButton(lakeName);
		mainButton.addActionListener(__ -> section.leftSidebar.owner.appController.servicePort.dispatchLakeChooseRequest(identity));
		mainButton.setActionCommand(lakeName);
		mainButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
		mainButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
		mainButton.setPreferredSize(new Dimension(40, 40));
		mainButton.setHorizontalAlignment(SwingConstants.LEFT);
		mainButton.setMargin(new Insets(0, 10, 0, 0));
		mainButton.putClientProperty("collapseIcon", Utilities.loadSVGIcon("icons/folder.svg", 40, 40));
		Utilities.initButton(mainButton);
		
		// Replaced FlowLayout with BoxLayout.X_AXIS so buttons strictly never wrap
		actionBar = new JPanel();
		actionBar.setLayout(new BoxLayout(actionBar, BoxLayout.X_AXIS));
		actionBar.setOpaque(true);
		actionBar.setAlignmentX(Component.RIGHT_ALIGNMENT);
		actionBar.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2));
		actionBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
//		actionBar.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 8));
		
		// This glue dynamically fills left-over space, anchoring the buttons firmly to the right side
		actionBar.add(Box.createHorizontalGlue());
		
		JButton importBtn = createSolidActionBtn("Import", "arrow-square-in.svg", Utilities.SLPEB_Hover, Utilities.ELEMENT_TEXT_PRIMARY);
		JButton closeBtn  = createSolidActionBtn("Close", "x-square.svg", new Color(255, 100, 100), Color.WHITE); // Pass Red Foreground directly
		
		closeBtn.addActionListener(e -> {
			int res = JOptionPane.showConfirmDialog(section.leftSidebar.owner, "Save and close Data Lake '" + lakeName + "'?", "Confirm Close", JOptionPane.YES_NO_OPTION);
			if (res == JOptionPane.YES_OPTION) {
				section.leftSidebar.owner.appController.servicePort.dispatchLakeChooseRequest(identity);
				SwingUtilities.invokeLater(() -> {
					OntoDirectoryService.DataLakeService lake = section.leftSidebar.owner.appController.servicePort.getActiveDataLake();
					if (lake != null && lake.getRootPath().equals(identity)) section.leftSidebar.owner.appController.dispatchLakeCloseRequest(lake);
					section.unsignDataLake(identity);
				});
			}
		});
		
		importBtn.addMouseListener(new MouseAdapter() {
			@Override public void mousePressed(MouseEvent e) {
				section.leftSidebar.owner.appController.servicePort.dispatchLakeChooseRequest(identity);
				OntoDirectoryService.DataLakeService lake = section.leftSidebar.owner.appController.servicePort.getActiveDataLake();
				if (lake == null) return;
				
				if (SwingUtilities.isLeftMouseButton(e)) {
					JFileChooser chooser = new JFileChooser();
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					if (chooser.showOpenDialog(section) == JFileChooser.APPROVE_OPTION) {
						SwingWorker worker = new SwingWorker() {
							@Override
							protected Object doInBackground() throws Exception {
								try {
									lake.importFiles(chooser.getSelectedFile().toPath());
								} catch(Exception ex) { ex.printStackTrace(); }
								return null;
							}
							@Override
							protected void done() {
								section.leftSidebar.owner.appController.wsModeller.triggerLakeRefresh(lake);
							}
						};
						worker.execute();
					}
				} else if (SwingUtilities.isRightMouseButton(e)) {
					SwingWorker worker = new SwingWorker() {
						@Override
						protected Object doInBackground() throws Exception {
							try {
								lake.importFiles();
							} catch(Exception ex) { ex.printStackTrace(); }
							return null;
						}
						@Override
						protected void done() {
							section.leftSidebar.owner.appController.wsModeller.triggerLakeRefresh(lake);
						}
					};
					worker.execute();
				}
			}
		});
		
		// Vacuum is hidden for now: the ontology engine backing ArchiveManager has no compaction
		// support yet, so DataLakeService.executeVacuum() is unimplemented.

		actionBar.add(importBtn);
		actionBar.add(Box.createHorizontalStrut(4));
		actionBar.add(closeBtn);
		
		container.add(mainButton);
		container.add(actionBar);
	}
	
	private JButton createSolidActionBtn(String text, String iconName, Color hoverColor, Color defaultForeground) {
		JButton btn = new JButton(text);
		btn.setFont(btn.getFont().deriveFont(Font.BOLD, 12f));
		
		// Set Maximum size to support the BoxLayout
		btn.setPreferredSize(new Dimension(85, 30));
		btn.setMaximumSize(new Dimension(85, 30));
		
		btn.setFocusPainted(false);
		btn.setOpaque(true);
		btn.setBackground(Utilities.ELEMENT_BG);
		btn.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		btn.setForeground(defaultForeground);
		
		// Generated both SVGs up front to perform quick swapping during the mouse event
		Icon defaultIcon = Utilities.loadSVGIcon("icons/" + iconName, 14, 14, defaultForeground);
		Icon hoverIcon = Utilities.loadSVGIcon("icons/" + iconName, 14, 14, hoverColor);
		
		if (defaultIcon != null) btn.setIcon(defaultIcon);
		
		Color defaultBg = Utilities.ELEMENT_BG;
		
		btn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				btn.setForeground(hoverColor);
				btn.setBackground(hoverColor.equals(Color.WHITE) ? new Color(220, 50, 50) : Utilities.ELEMENT_BG.brighter());
				if (hoverIcon != null) btn.setIcon(hoverIcon);
			}
			@Override
			public void mouseExited(MouseEvent e) {
				btn.setForeground(defaultForeground);
				btn.setBackground(defaultBg);
				if (defaultIcon != null) btn.setIcon(defaultIcon);
			}
		});
		
		return btn;
	}
	
}

}