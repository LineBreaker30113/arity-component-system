package org.halim.nStructure.sgui;

import org.halim.nStructure.sgui.sglib.Utilities;
import org.halim.nStructure.sgui.visual.AnchorSection;
import org.halim.nStructure.sgui.visual.LoadedLakesSection;
import org.halim.nStructure.sgui.visual.WorkspaceSection;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

public class LeftSidebar extends JPanel {

public final GUI_RootPanel owner;
private boolean isCollapsed = false;

public final int expanded = 260, contracted = 56;

public final AnchorSection anchorSection;
public final LoadedLakesSection loadedLakesSection;
public final WorkspaceSection workspaceSection;

public LeftSidebar(GUI_RootPanel owner) {
	this.owner = owner;
	setBackground(Utilities.sideBarBG);
	setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	setSidebarWidth(expanded);
	
	anchorSection = new AnchorSection(this);
	loadedLakesSection = new LoadedLakesSection(this);
	workspaceSection = new WorkspaceSection(this);
	
	anchorSection.setAlignmentX(Component.RIGHT_ALIGNMENT);
	loadedLakesSection.setAlignmentX(Component.RIGHT_ALIGNMENT);
	workspaceSection.setAlignmentX(Component.RIGHT_ALIGNMENT);
	
	add(anchorSection);
	add(Box.createVerticalStrut(48));
	add(loadedLakesSection);
	add(Box.createVerticalStrut(32));
	add(workspaceSection);
	add(Box.createVerticalStrut(40));
}

private void setSidebarWidth(int width) {
	Dimension dim = new Dimension(width, Short.MAX_VALUE);
	setPreferredSize(dim);
	setMinimumSize(dim);
	setMaximumSize(dim);
}

public void toggleCollapse() {
	isCollapsed = !isCollapsed;
	org.halim.nStructure.mhexagon.CrashReporter.log("[GUI] LeftSidebar collapsed state updated to: " + isCollapsed);
	setSidebarWidth(isCollapsed ? contracted : expanded);
	
	anchorSection.updateCollapseState(isCollapsed);
	loadedLakesSection.updateCollapseState(isCollapsed);
	workspaceSection.updateCollapseState(isCollapsed);
	
	revalidate();
	repaint();
}

public void registerDataLake(Path lakeRootPath) {
	loadedLakesSection.registerDataLake(lakeRootPath);
}
}