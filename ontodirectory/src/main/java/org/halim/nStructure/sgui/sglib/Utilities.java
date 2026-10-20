package org.halim.nStructure.sgui.sglib;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class Utilities { // TODO: refactor this, make a GUI config object, make it parseable, use this for small simple static functions.


// --- DRAG AND DROP INFRASTRUCTURE ---
// Replace your existing FLAVOR declarations with these:

public static final java.awt.datatransfer.DataFlavor FILE_LIST_FLAVOR =
	  new java.awt.datatransfer.DataFlavor("application/x-ontodir-file-list;class=java.util.List", "List of FileInterfaces");

public static final java.awt.datatransfer.DataFlavor TAG_LIST_FLAVOR =
	  new java.awt.datatransfer.DataFlavor("application/x-ontodir-tag-list;class=java.util.List", "List of TagNodeDtos");


public record TagNodeDto(int identity, String name) {
	@Override public String toString() { return name; }
}

public static class GenericTransferable implements java.awt.datatransfer.Transferable {
	private final Object data;
	private final java.awt.datatransfer.DataFlavor flavor;
	
	public GenericTransferable(Object data, java.awt.datatransfer.DataFlavor flavor) {
		this.data = data;
		this.flavor = flavor;
	}
	@Override public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() { return new java.awt.datatransfer.DataFlavor[]{flavor}; }
	@Override public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor f) { return flavor.equals(f); }
	@Override public Object getTransferData(java.awt.datatransfer.DataFlavor f) throws java.awt.datatransfer.UnsupportedFlavorException {
		if (!isDataFlavorSupported(f)) throw new java.awt.datatransfer.UnsupportedFlavorException(f);
		return data;
	}
}

public static final Color TRANSPARANT_BLACK = new Color(0, 0, 0, 0);

public static class Pointer<T> { public T target = null; }

public static boolean repaintJFrame(Component component) {
	JFrame frame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, component);
	if (frame != null) { frame.revalidate(); frame.repaint(); return false; }
	return true;
}

/**
 * Loads an image from the Maven resources folder.
 * Pass the path starting with a forward slash, e.g., "/icons/my_icon.png"
 */
public static @Nullable ImageIcon loadIcon(String path) {
	java.net.URL imgURL = Utilities.class.getResource(path);
	if (imgURL != null) {
		return new ImageIcon(imgURL);
	} else {
		System.err.println("Stop! I couldn't find file: " + path + ". Check your src/main/resources folder!");
		return null;
	}
}
/**
 * Strips 1998 Swing defaults off a button so it actually looks modern.
 */
public static void initButton(@NotNull JButton button) {
	button.setFocusable(false); // Kills focus stealing
	button.setFocusPainted(false); // Kills the ugly dotted focus ring
	// This is the line that fixes the "one word gap" nightmare
	button.setMargin(new Insets(0, 0, 0, 0));
	// If you have an icon and text, this sets a sane 8-pixel gap between them
	button.setIconTextGap(8);
	
	// Optional: If FlatLaf isn't doing enough, uncomment these to make it completely flat
	// button.setContentAreaFilled(false);
	// button.setBorderPainted(false);
	// button.setOpaque(false);
}

// Put this inside your Utilities class
public static @Nullable Icon loadSVGIcon(String path, int width, int height) {
	try {
		// FlatSVGIcon automatically scales the vector perfectly
		return new FlatSVGIcon(path, width, height);
	} catch (Exception e) {
		System.err.println("Stop! I couldn't find SVG file: " + path);
		return null;
	}
}

public static Icon loadSVGIcon(String path, int width, int height, Color color) {
	FlatSVGIcon icon = new FlatSVGIcon(path, width, height);
	if (color != null) {
		// This filter replaces the "base" color of the SVG with your custom Color
		icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
	}
	return icon;
}

public static final Color
	  WP_BG = new Color(30, 30, 40),              // Welcome Page Background
	  LIST_BG = new Color(40, 40, 40),            // Scroll List Background
	  LIST_HEADER_BG = new Color(20, 20, 20),     // Scroll List Header
	  ELEMENT_BG = new Color(50, 50, 50),         // List Element Background
	  ELEMENT_TEXT_PRIMARY = new Color(240, 240, 240), // Main Text
	  ELEMENT_TEXT_SECONDARY = new Color(150, 150, 150), // Paths / Subtext
	  GOLDEN_COLOR = new Color(255, 215, 0);

public static final Color sideBarBG = new Color(0, 102, 88),
	  sideButtonBG = new Color(175, 158, 76)
//	  , sideButtonToggledBG = new Color(255, 224, 0)
//	  , welcomeBG = new Color(0, 255, 86)
//	  , sideButtonToggledBG = new Color(0, 0, 0)
;

public static final Dimension box0x8 = new Dimension(0, 8);
@Contract(" -> new")
public static @NotNull Component getBox0x8() { return Box.createRigidArea(box0x8); }

//public static final Dimension buttonExpandedDimension = new Dimension(280, 60);
//public static final Dimension buttonCollapsedDimension = new Dimension(50, 60);


public static final Dimension maxDimension = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
public static final Dimension box10x10 = new Dimension(10, 10);
public static final Dimension box20x20 = new Dimension(20, 20);
public static final Dimension box30x30 = new Dimension(30, 30);
public static final Dimension box700x700 = new Dimension(700, 700);
public static final Dimension box700x720 = new Dimension(700, 720);
public static final Color
	  SLPCP_BG = new Color(0, 63, 95), // Scroll List Panel Content Pane
	  SLPC_BG = Color.DARK_GRAY, // Scroll List Panel Corners Background
	  SLPEB_Idle = new Color(159, 191, 223), // Scroll List Panel Element Button
	  SLPEB_Hover = new Color(239, 207, 0), // Scroll List Panel Element Button
	  SLPE_BG = new Color(63, 95, 0), // Scroll List Element

SCPSB_Idle = new Color(159, 191, 223), // Switcher Control Panel Switch Button
	  SCPSB_Chosen = new Color(239, 207, 0), // Switcher Control Panel Switch Button
	  SCPSB_Border = new Color(39, 31, 39), // Switcher Control Panel Switch Button
	  SCPSB_BG = new Color(127, 143, 159) // Switcher Control Panel Switch Button
//			SLPEB_Border = new Color(39, 31, 39), // Scroll List Panel Element Button Border
//			SLPEB_BG = new Color(127, 143, 159), // Scroll List Element Element Button Background
//			SLPEB_FG = new Color(127, 143, 159) // Scroll List Element Element Button Foreground
		    ;



public final static void setCertainSize(Dimension size, JComponent component) {
	component.setPreferredSize(size);
	component.setMaximumSize(size);
	component.setMinimumSize(size);
	component.setSize(size);
}

public static interface Action { void act(); }


public static final Border Button_IB = new LineBorder(new Color(191, 223, 239), 2); // Button idle border
public static final Border Button_HB = new LineBorder(new Color(239, 223, 0), 2);  // Button hover border

public static void main(String[] args) {
	// TODO Auto-generated method stub
	
}



public static class StaticCoordinateTestClass {
	public static final double graphLineUnitLength2 = Math.exp(3.) * 2;
	private static double virtualX = 0., virtualY = 0., zoomLevel = 1.;
	
	public static int getWidth() { return 400; }
	public static int getHeight() { return 300; }
	
	public static double getVirtualXbyDisplayX(double dx) {
		return virtualX + (((double) dx - getWidth()/2) / getWidth()* graphLineUnitLength2) / zoomLevel;
	}
	public static double getDisplayXbyVirtualX(double dx) {
		return (dx - virtualX) * zoomLevel * getWidth()/ graphLineUnitLength2 + getWidth()/2;
	}
	public static double getVirtualYbyDisplayY(double dy) {
		return virtualY + ((getHeight()/2 - (double) dy) / getHeight()* graphLineUnitLength2) / zoomLevel;
	}
	public static double getDisplayYbyVirtualY(double dy) {
		return -((dy - virtualX) * zoomLevel * getHeight()/ graphLineUnitLength2) + getHeight()/2;
	}
	
	public static String testBoth(int x, int y) {
		String result = "Dx: " + x + "\tDy: " + y;
		result += "\tResults: x: " + getVirtualXbyDisplayX(x) + "\ty: " + getVirtualYbyDisplayY(y);
		return result;
	}
	public static String testX(int x) {
		String result = "Dx: " + x + "\tRx: " + getVirtualXbyDisplayX(x);
		return result;
	}
	public static String testY(int y) {
		String result = "Dy: " + y + "\tRy: " + getVirtualYbyDisplayY(y);
		return result;
	}
	
	public static void runTests() {
		System.out.println(graphLineUnitLength2);
		System.out.println("W: " + getWidth() + "\tH: " + getHeight());
		System.out.println("X: " + virtualX + "\tY: " + virtualY + "\tZ: "+ zoomLevel);
		System.out.println(testX(0));
		System.out.println(testX(10));
		System.out.println(testX(200));
		System.out.println(testX(400));
		System.out.println(testX(390));
		
		System.out.println(testY(0));
		System.out.println(testY(10));
		System.out.println(testY(150));
		System.out.println(testY(300));
		System.out.println(testY(290));
	}
	
}

public static JButton createElementButton(Consumer<Graphics2D> painter, ActionListener purpose) {
	return new JButton() {
		{
			super.addActionListener(purpose);
			Utilities.setCertainSize(Utilities.box30x30, this);
			super.setBackground(Utilities.SLPE_BG);
			super.setForeground(Utilities.SLPE_BG);
			super.addMouseListener(new MouseAdapter() {
				@Override public void mouseExited(MouseEvent e) { setBorder(Utilities.Button_IB); }
				@Override public void mouseEntered(MouseEvent e) { setBorder(Utilities.Button_HB); }
			});
			super.setBorder(Utilities.Button_IB);
		}
		@Override
		public void paintComponent(Graphics gdepr) {
			Graphics2D g = (Graphics2D) gdepr;
			g.setBackground(getBackground());
			g.clearRect(0, 0, getWidth(), getHeight());
			g.setColor(getForeground());
			painter.accept(g);
		}
	};
}

}