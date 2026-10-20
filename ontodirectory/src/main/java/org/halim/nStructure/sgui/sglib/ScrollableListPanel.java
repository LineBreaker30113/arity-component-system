package org.halim.nStructure.sgui.sglib;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ScrollableListPanel<ET extends Component> extends JScrollPane implements Iterable<ET> {

private static final long serialVersionUID = -3640149793522466660L;
protected int elementSpacer = 6;

public static class ElementContainerPanel<ET extends Component> extends JPanel {
	private static final long serialVersionUID = 5121706301328318919L;
	public final ET element;
	
	public ElementContainerPanel(ET element, int elementSpacer) {
		this.element = element;
		// CRITICAL FIX: BorderLayout forces the inner element to expand horizontally to fill the viewport
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(elementSpacer, 6, elementSpacer, 6));
		setBackground(Utilities.TRANSPARANT_BLACK);
		setOpaque(false);
		add(element, BorderLayout.CENTER);
	}
	
	// CRITICAL FIX: Dynamically calculate height based on the child component's needs
	@Override
	public Dimension getMaximumSize() {
		Dimension childPref = element.getPreferredSize();
		return new Dimension(Integer.MAX_VALUE, childPref.height + getInsets().top + getInsets().bottom);
	}
	
	@Override
	public Dimension getPreferredSize() {
		Dimension childPref = element.getPreferredSize();
		return new Dimension(super.getPreferredSize().width, childPref.height + getInsets().top + getInsets().bottom);
	}
}

private static class ScrollableListContentPanel extends JPanel implements Scrollable {
	public ScrollableListContentPanel() {
		super();
		super.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	}
	@Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
	@Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 12; }
	@Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return 60; }
	// THE GENIUS MOVE: This forces elements to match the width of the JScrollPane, enabling text wrapping.
	@Override public boolean getScrollableTracksViewportWidth() { return true; }
	@Override public boolean getScrollableTracksViewportHeight() { return false; }
}

private final ArrayList<ElementContainerPanel<ET>> elementContainers = new ArrayList<>();
private final ScrollableListContentPanel contentPanel;

public Iterator<ET> getIterator() { // Typo fixed
	return new Iterator<ET>() {
		int iteration = 0;
		@Override public boolean hasNext() { return iteration < elementContainers.size(); }
		@Override public ET next() {
			if(!hasNext()) { throw new NoSuchElementException(); }
			return elementContainers.get(iteration++).element;
		}
	};
}

public int getElementCount() { return elementContainers.size(); }

public ScrollableListPanel(JPanel headerPanel, Dimension minD, Dimension maxD) {
	super(new ScrollableListContentPanel());
	
	this.contentPanel = (ScrollableListContentPanel) super.getViewport().getView();
	
	super.setBorder(null);
	super.setColumnHeaderView(headerPanel);
	super.setMinimumSize(minD);
	super.setPreferredSize(minD);
	super.setMaximumSize(maxD);
	
	// Remove the ugly border Swing adds to JScrollPanes by default
	super.setViewportBorder(null);
}

public int getIndex(ET element) {
	for (int i = 0; i < elementContainers.size(); i++) {
		if(elementContainers.get(i).element == element) { return i; }
	}
	return -1;
}

public ET getElement(int elementIndex) { return elementContainers.get(elementIndex).element; }

public void addElement(ET newElement) {
	// No more hardcoded 35 pixel height. The panel calculates itself.
	ElementContainerPanel<ET> elementToAdd = new ElementContainerPanel<>(newElement, elementSpacer);
	elementContainers.add(elementToAdd);
	contentPanel.add(elementToAdd);
	
	// SENIOR FIX: Localized Layout Validation
	contentPanel.revalidate();
	contentPanel.repaint();
	
	// Safely auto-scroll to the bottom after layout calculations finish
	SwingUtilities.invokeLater(() -> {
		javax.swing.JScrollBar vertical = getVerticalScrollBar();
		vertical.setValue(vertical.getMaximum());
	});
}

public void removeElement(ET element) {
	int index = getIndex(element);
	if(index >= 0) removeElement(index);
}

public void removeElement(int index) {
	ElementContainerPanel<ET> target = elementContainers.get(index);
	contentPanel.remove(target);
	elementContainers.remove(index);
	contentPanel.revalidate();
	contentPanel.repaint();
}

public void setInnerColor(Color bg) {
	super.setBackground(bg);
	super.getViewport().setBackground(bg);
	contentPanel.setBackground(bg);
	contentPanel.repaint();
}

@SuppressWarnings("static-access")
public void setSidePanelsColor(Color spc) {
	super.getColumnHeader().setBackground(spc);
	super.setRowHeader(new JViewport(){{ super.setBackground(spc); }});
	super.setCorner(super.UPPER_RIGHT_CORNER, new JPanel(){{ super.setBackground(spc); }});
	super.repaint();
}

@Override public Iterator<ET> iterator() { return getIterator(); }
}