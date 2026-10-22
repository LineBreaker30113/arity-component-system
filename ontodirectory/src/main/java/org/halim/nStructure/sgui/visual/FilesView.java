package org.halim.nStructure.sgui.visual;

import org.halim.nStructure.mhexagon.OntoDirectoryService;
import org.halim.nStructure.mhexagon.OntologyClassHandle;
import org.halim.nStructure.mhexagon.OntologyElementHandle;
import org.halim.nStructure.mhexagon.OntologyReadingService;
import org.halim.nStructure.ontology.core.segmentation.query.OntologyElementFilter;
import org.halim.nStructure.sgui.ApplicationController;
import org.halim.nStructure.sgui.sglib.ContentView;
import org.halim.nStructure.sgui.sglib.Utilities;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

public class FilesView extends ContentView {

private final ApplicationController mac;
private final DefaultListModel<OntologyElementHandle> listModel;
private final JList<OntologyElementHandle> fileList;

private OntoDirectoryService.DataLakeService activeLakeService = null;
private List<OntologyElementHandle> currentDomainFiles = new ArrayList<>();

// Infinite Scrolling State
private int loadedCount = 0;
private final int ITEMS_PER_CHUNK = 100;

// AST Builder Container
private final JPanel astContainer;
private FilterNodeUI rootAstNode;

public FilesView(ApplicationController mac) {
	this.mac = mac;
	setLayout(new BorderLayout());
	setBackground(Utilities.WP_BG);

	// --- TOP: AST Filter Track ---
	JPanel topBar = new JPanel(new BorderLayout(10, 0));
	topBar.setBackground(Utilities.LIST_HEADER_BG);
	topBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	topBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

	// 1. Override preferred size AND layout logic natively
	astContainer = new JPanel(null) {
		@Override
		public Dimension getPreferredSize() {
			if (rootAstNode != null) {
				return rootAstNode.getPreferredSize();
			}
			return super.getPreferredSize();
		}

		@Override
		public void doLayout() {
			super.doLayout();
			if (rootAstNode == null) return;

			int containerWidth = getWidth();
			Dimension childSize = rootAstNode.getPreferredSize();

			// Calculate centering coordinates
			int x = (containerWidth - childSize.width) / 2;
			int y = (65 - childSize.height) / 2;

			// If the AST exceeds view bounds, anchor it to 0,0 so scrolling works
			if (x < 0) x = 0;

			// Force the child to take its required space at the centered coordinates
			rootAstNode.setBounds(x, y, childSize.width, childSize.height);
		}
	};

	astContainer.setBackground(Utilities.ELEMENT_BG);
	initEmptyAst();

	JScrollPane astScrollPane = new JScrollPane(astContainer);
	astScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
	astScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
	astScrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
	astScrollPane.setPreferredSize(new Dimension(0, 65));
	astScrollPane.getHorizontalScrollBar().setUnitIncrement(astScrollPane.getHorizontalScrollBar().getUnitIncrement() * 26);

	JPanel actionPanel = new JPanel(new GridLayout(3, 1, 0, 4));
	actionPanel.setPreferredSize(new Dimension(100, 80));
	actionPanel.setOpaque(false);

	actionPanel.add(createColBtn("Save", "plus-circle.svg", e -> {
		OntologyElementFilter f = rootAstNode.buildFilter();
		if (f != null && activeLakeService != null) {
			String name = JOptionPane.showInputDialog(this, "Tag Name:");
			if (name != null) activeLakeService.getOntologyManagingService().filterToClass(f, name);
		}
	}));
	actionPanel.add(createColBtn("Search", "magnifying-glass.svg", e -> performSearch()));
	actionPanel.add(createColBtn("Export", "share.svg", e -> {
		if (activeLakeService != null) {
			JFileChooser c = new JFileChooser(); c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (c.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) activeLakeService.exportSpecificFiles(currentDomainFiles, c.getSelectedFile().toPath());
		}
	}));

	topBar.add(astScrollPane, BorderLayout.CENTER);
	topBar.add(actionPanel, BorderLayout.EAST);

	// --- CENTER: Infinite Scroll List Rendering ---
	listModel = new DefaultListModel<>();

	fileList = new JList<>(listModel);

	fileList.setBackground(Utilities.LIST_BG);
	fileList.setForeground(Utilities.ELEMENT_TEXT_PRIMARY);
	fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	fileList.setFixedCellHeight(30);

	fileList.setCellRenderer(new DefaultListCellRenderer() {
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof OntologyElementHandle fi) {
				label.setText("  " + fi.getActualName());
				// No cheap per-row Path is available here (resolving one means a real lake copy via
				// exportSpecificFiles) -- use the generic file icon instead of a per-type system icon.
				label.setIcon(UIManager.getIcon("FileView.fileIcon"));
			}

			if (isSelected) {
				label.setBackground(Utilities.SLPEB_Idle);
				label.setForeground(Color.BLACK);
			} else {
				label.setBackground(Utilities.LIST_BG);
				label.setForeground(Utilities.ELEMENT_TEXT_PRIMARY);
			}
			return label;
		}
	});

	fileList.addMouseListener(new java.awt.event.MouseAdapter() {
		@Override public void mousePressed(java.awt.event.MouseEvent e) {
			showContextMenu(e);

			// Trigger tag list purely on a single left click
			if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1 && !e.isPopupTrigger()) {
				int index = fileList.locationToIndex(e.getPoint());
				// Ensure they clicked exactly on an item row, not empty space
				if (index != -1 && fileList.getCellBounds(index, index).contains(e.getPoint())) {
					OntologyElementHandle fi = listModel.getElementAt(index);
					showTagListPopup(fi, e.getComponent(), e.getX(), e.getY());
				}
			}
		}
		@Override public void mouseReleased(java.awt.event.MouseEvent e) { showContextMenu(e); }
	});

	// DRAG AND DROP OUT (Files -> TreeView)
	fileList.setDragEnabled(true);
	fileList.setTransferHandler(new TransferHandler() {
		@Override public int getSourceActions(JComponent c) { return COPY_OR_MOVE; }
		@Override protected java.awt.datatransfer.Transferable createTransferable(JComponent c) {
			return new Utilities.GenericTransferable(fileList.getSelectedValuesList(), Utilities.FILE_LIST_FLAVOR);
		}
	});

	JScrollPane scrollPane = new JScrollPane(fileList);
	scrollPane.setBorder(null);

	scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
		if (!e.getValueIsAdjusting()) {
			JScrollBar bar = (JScrollBar) e.getAdjustable();
			if (bar.getValue() + bar.getModel().getExtent() >= bar.getModel().getMaximum() - 100) {
				loadMoreFiles();
			}
		}
	});

	add(topBar, BorderLayout.NORTH);
	add(scrollPane, BorderLayout.CENTER);
}

private @NotNull JButton createColBtn(String text, String icon, java.awt.event.ActionListener al) {
	JButton b = new JButton(text, Utilities.loadSVGIcon("icons/"+icon, 16, 16, Color.WHITE));
	b.setBackground(Utilities.sideButtonBG);
	b.setForeground(Color.WHITE);
	b.setMargin(new Insets(0, 0, 0, 0));
	b.addActionListener(al);
	return b;
}

private void initEmptyAst() {
	astContainer.removeAll();
	rootAstNode = new EmptyNodeUI(newNode -> {
		rootAstNode = newNode;
		astContainer.removeAll();
		astContainer.add(rootAstNode);
		astContainer.revalidate();
		astContainer.repaint();
	});
	astContainer.add(rootAstNode);
	astContainer.revalidate();
	astContainer.repaint();
}

private void showTagListPopup(OntologyElementHandle fi, Component anchor, int x, int y) {
	JPopupMenu tagMenu = new JPopupMenu();
	boolean hasTags = false;

	for (OntologyClassHandle tagClass : fi.getClasses()) {
		if (tagClass.getIdentity() == 0) continue; // Skip the Root "File" tag
		JMenuItem item = new JMenuItem(tagClass.getName());
		item.setEnabled(false); // Make it read-only so it acts like a label
		tagMenu.add(item);
		hasTags = true;
	}

	if (hasTags) {
		tagMenu.show(anchor, x, y);
	}
}

private void showContextMenu(java.awt.event.MouseEvent e) {
	if (e.isPopupTrigger()) {
		int row = fileList.locationToIndex(e.getPoint());
		if (row != -1 && !fileList.isSelectedIndex(row)) {
			fileList.setSelectedIndex(row);
		}

		List<OntologyElementHandle> selectedFiles = fileList.getSelectedValuesList();
		if (selectedFiles.isEmpty() || activeLakeService == null) return;

		JPopupMenu contextMenu = new JPopupMenu();
		JMenuItem openFile = new JMenuItem("Open File (Read-Only Copy)");
		JMenuItem addTag = new JMenuItem("Add Tag to Selected...");
		JMenuItem removeTag = new JMenuItem("Remove Tag from Selected...");
		JMenuItem exportCopy = new JMenuItem("Export Selected Files...");
		JMenuItem rename = new JMenuItem("Rename Actual Name");
		JMenuItem delete = new JMenuItem("Delete The File From the Base");

		openFile.addActionListener(a -> {
			Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "ontoDirTemp");
			try {
				activeLakeService.exportSpecificFiles(selectedFiles, tempDir);
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(this, "Could not copy file(s): " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			for (OntologyElementHandle fi : selectedFiles) {
				try {
					Path tempFile = tempDir.resolve(fi.getActualName());
					tempFile.toFile().setReadOnly();
					Desktop.getDesktop().open(tempFile.toFile());
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(this, "Could not open file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		addTag.addActionListener(a -> {
			Integer tagId = pickTagDialog();
			if (tagId != null) {
				for (OntologyElementHandle fi : selectedFiles) {
					activeLakeService.getOntologyManagingService().addElement(tagId, fi.getIdentity());
				}
				fileList.repaint();
			}
		});

		removeTag.addActionListener(a -> {
			Integer tagId = pickTagDialog();
			if (tagId != null) {
				for (OntologyElementHandle fi : selectedFiles) {
					activeLakeService.getOntologyManagingService().removeElement(tagId, fi.getIdentity());
				}
				performSearch();
			}
		});

		exportCopy.addActionListener(a -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setDialogTitle("Select Export Destination");
			if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				activeLakeService.exportSpecificFiles(selectedFiles, chooser.getSelectedFile().toPath());
			}
		});

		rename.addActionListener(a -> {
			OntologyElementHandle target = selectedFiles.get(0);
			String newName = (String) JOptionPane.showInputDialog(this, "New file name:", "Rename File", JOptionPane.PLAIN_MESSAGE, null, null, target.getActualName());
			if (newName != null && !newName.trim().isEmpty()) {
				activeLakeService.getOntologyManagingService().renameElementActualName(target.getIdentity(), newName);
				fileList.repaint();
			}
		});

		delete.addActionListener(a -> {
			int decision = JOptionPane.showConfirmDialog(this, "Deletion from the Set Sea, this action cannot be \"undo\"ne!", "Confirm Deletion.", JOptionPane.OK_CANCEL_OPTION);
			if (decision == JOptionPane.OK_OPTION) {
				try {
					activeLakeService.getOntologyManagingService().deleteElements(selectedFiles);
					performSearch();
				} catch (Exception ex) {
					// Manually bridge the Swing Thread failure to your Application's Port
					mac.servicePort.reportFatalError(ex);
				}
			}
		});

		contextMenu.add(openFile);
		contextMenu.addSeparator();
		contextMenu.add(addTag);
		contextMenu.add(removeTag);
		contextMenu.addSeparator();
		contextMenu.add(exportCopy);
		contextMenu.add(rename);
		contextMenu.addSeparator();
		contextMenu.add(delete);
		contextMenu.show(fileList, e.getX(), e.getY());
	}
}

@Override
public void onSelectedLakeChange(OntoDirectoryService.DataLakeService s) {
	this.activeLakeService = s;
	performSearch();
}

@Override
public void onSelectedClassChange(OntologyClassHandle ontologyClass) {
	if (ontologyClass == null || activeLakeService == null) return;

	rootAstNode = new LeafNodeUI(newNode -> {
		rootAstNode = newNode;
		astContainer.removeAll();
		astContainer.add(rootAstNode);
		astContainer.revalidate();
		astContainer.repaint();
	}, ontologyClass.getIdentity(), false);

	astContainer.removeAll();
	astContainer.add(rootAstNode);
	astContainer.revalidate();
	astContainer.repaint();

	performSearch();
}

@Override public void onDomainChange(OntologyClassHandle ontologyClass) { onSelectedClassChange(ontologyClass); }

private void performSearch() {
	if (rootAstNode == null || activeLakeService == null) return;
	OntologyElementFilter filter = rootAstNode.buildFilter();
	OntologyReadingService reader = activeLakeService.getOntologyReadingService();

	currentDomainFiles = new ArrayList<>(
		  filter == null ? reader.getAllOntologyElements(0) : reader.getOntologyElements(filter));

	currentDomainFiles.sort(Comparator.comparingInt(OntologyElementHandle::getIdentity));

	listModel.clear();
	loadedCount = 0;
	loadMoreFiles();
}

private void loadMoreFiles() {
	if (currentDomainFiles == null) return;
	int total = currentDomainFiles.size();
	if (loadedCount >= total) return;

	int end = Math.min(loadedCount + ITEMS_PER_CHUNK, total);
	for (int i = loadedCount; i < end; i++) {
		listModel.addElement(currentDomainFiles.get(i));
	}
	loadedCount = end;
}

private Integer pickTagDialog() {
	if (activeLakeService == null) return null;
	OntologyReadingService reader = activeLakeService.getOntologyReadingService();
	OntologyClassHandle root = reader.getRootOntologyClass();
	if (root == null) return null;

	List<String> displayNames = new ArrayList<>();
	List<Integer> ids = new ArrayList<>();

	Queue<OntologyClassHandle> q = new LinkedList<>();
	Set<Integer> visited = new HashSet<>();
	q.add(root);
	visited.add(root.getIdentity());

	while (!q.isEmpty()) {
		OntologyClassHandle curr = q.poll();
		displayNames.add(curr.getName());
		ids.add(curr.getIdentity());
		for (OntologyClassHandle child : curr.getSafeChildren()) {
			if (visited.add(child.getIdentity())) q.add(child);
		}
	}

	JComboBox<String> combo = new JComboBox<>(displayNames.toArray(new String[0]));
	int res = JOptionPane.showConfirmDialog(this, combo, "Select Conceptual Tag", JOptionPane.OK_CANCEL_OPTION);
	if (res == JOptionPane.OK_OPTION && combo.getSelectedIndex() != -1) {
		return ids.get(combo.getSelectedIndex());
	}
	return null;
}

private abstract class FilterNodeUI extends JPanel {
	public FilterNodeUI() {
		setOpaque(true);
		setAlignmentY(Component.CENTER_ALIGNMENT); // Perfectly centers in BoxLayout.X_AXIS
		setBackground(Utilities.LIST_HEADER_BG);
		// Replaced FlowLayout gap with empty border margins so BoxLayout spaces them correctly
		setBorder(BorderFactory.createCompoundBorder(
			  BorderFactory.createEmptyBorder(2, 4, 2, 4),
			  BorderFactory.createCompoundBorder(
				    BorderFactory.createLineBorder(Color.BLACK, 1),
				    BorderFactory.createEmptyBorder(4, 6, 4, 6)
			  )
		));
		setLayout(new FlowLayout(FlowLayout.CENTER, 4, 0));
	}
	public abstract OntologyElementFilter buildFilter();
}

private class EmptyNodeUI extends FilterNodeUI {
	private final Consumer<FilterNodeUI> onReplace;
	public EmptyNodeUI(Consumer<FilterNodeUI> onReplace) {
		this.onReplace = onReplace;
		setBackground(Utilities.WP_BG);

		JButton btn = new JButton("[ + Drop Tag or Click ]");
		Utilities.initButton(btn);
		btn.setForeground(Color.GRAY);
		btn.addActionListener(e -> showMenu(btn));
		add(btn);

		setTransferHandler(new TransferHandler() {
			@Override public boolean canImport(TransferSupport support) {
				return support.isDataFlavorSupported(Utilities.TAG_LIST_FLAVOR);
			}
			@Override public boolean importData(TransferSupport support) {
				try {
					@SuppressWarnings("unchecked")
					List<Utilities.TagNodeDto> tags = (List<Utilities.TagNodeDto>) support.getTransferable().getTransferData(Utilities.TAG_LIST_FLAVOR);
					if (tags != null && !tags.isEmpty()) {
						int tagId = tags.get(0).identity();
						JPopupMenu dropMenu = new JPopupMenu();
						dropMenu.add(createItem("Filter as Domain of " + tags.get(0).name(), () -> onReplace.accept(new LeafNodeUI(onReplace, tagId, false))));
						dropMenu.add(createItem("Filter as Direct Element of " + tags.get(0).name(), () -> onReplace.accept(new LeafNodeUI(onReplace, tagId, true))));
						dropMenu.show(EmptyNodeUI.this, 0, getHeight());
						return true;
					}
				} catch (Exception ex) { ex.printStackTrace(); }
				return false;
			}
		});
	}

	private void showMenu(Component anchor) {
		JPopupMenu menu = new JPopupMenu();
		menu.add(createItem("Direct Element Of", () -> replaceWithLeaf(true)));
		menu.add(createItem("Under Domain Of", () -> replaceWithLeaf(false)));
		menu.addSeparator();
		menu.add(createItem("AND Operator", () -> onReplace.accept(new BinaryNodeUI(onReplace, "AND"))));
		menu.add(createItem("OR Operator", () -> onReplace.accept(new BinaryNodeUI(onReplace, "OR"))));
		menu.add(createItem("SUBTRACT Operator", () -> onReplace.accept(new BinaryNodeUI(onReplace, "SUB"))));
		menu.addSeparator();
		menu.add(createItem("Name Contains", () -> onReplace.accept(new ContainsNodeUI(onReplace))));
		menu.add(createItem("Clear Block", () -> onReplace.accept(new EmptyNodeUI(onReplace))));
		menu.show(anchor, 0, anchor.getHeight());
	}

	private JMenuItem createItem(String label, Runnable action) {
		JMenuItem item = new JMenuItem(label);
		item.addActionListener(e -> action.run());
		return item;
	}

	private void replaceWithLeaf(boolean direct) {
		Integer tagId = pickTagDialog();
		if (tagId != null) onReplace.accept(new LeafNodeUI(onReplace, tagId, direct));
	}

	@Override public OntologyElementFilter buildFilter() { return null; }
}

private class LeafNodeUI extends FilterNodeUI {
	private final int tagId;
	private final boolean direct;
	public LeafNodeUI(Consumer<FilterNodeUI> onReplace, int tagId, boolean direct) {
		this.tagId = tagId;
		this.direct = direct;
		String name = activeLakeService.getOntologyReadingService().getClassFromIdentity(tagId).getName();

		JButton resetBtn = new JButton("↻");
		Utilities.initButton(resetBtn);
		resetBtn.setForeground(Color.RED.darker());
		resetBtn.addActionListener(e -> onReplace.accept(new EmptyNodeUI(onReplace)));

		JLabel lbl = new JLabel((direct ? "Direct[" : "Domain[") + name + "]");
		lbl.setForeground(Utilities.GOLDEN_COLOR);

		add(resetBtn); add(lbl);
	}
	@Override public OntologyElementFilter buildFilter() {
		return direct ? new OntologyElementFilter.DirectElementOf(tagId) : new OntologyElementFilter.UnderDomain(tagId);
	}
}

private class BinaryNodeUI extends FilterNodeUI {
	private FilterNodeUI left, right;
	private final String operator;
	public BinaryNodeUI(Consumer<FilterNodeUI> onReplace, String operator) {
		this.operator = operator;
		setBackground(Utilities.sideBarBG.darker());

		JButton resetBtn = new JButton("↻");
		Utilities.initButton(resetBtn);
		resetBtn.setForeground(Color.RED.darker());
		resetBtn.addActionListener(e -> onReplace.accept(new EmptyNodeUI(onReplace)));

		left = new EmptyNodeUI(n -> { remove(left); left = n; add(left, 1); revalidate(); repaint(); });
		right = new EmptyNodeUI(n -> { remove(right); right = n; add(right); revalidate(); repaint(); });

		JLabel opLbl = new JLabel(" " + operator + " ");
		opLbl.setFont(opLbl.getFont().deriveFont(Font.BOLD));
		opLbl.setForeground(Color.WHITE);

		add(resetBtn); add(left); add(opLbl); add(right);
	}
	@Override public OntologyElementFilter buildFilter() {
		OntologyElementFilter l = left.buildFilter(), r = right.buildFilter();
		if (l == null || r == null) return null;
		if (operator.equals("AND")) return new OntologyElementFilter.And(l, r);
		if (operator.equals("OR")) return new OntologyElementFilter.Or(l, r);
		return new OntologyElementFilter.Subtract(l, r);
	}
}

/** A standalone name predicate, not a wrapper: {@code NameContaining} carries no base filter of its
 *  own, so narrowing is expressed the same way every other combination is -- drop this under an AND
 *  next to a tag node. On its own it searches every element in the lake. */
private class ContainsNodeUI extends FilterNodeUI {
	private final JTextField textField;
	public ContainsNodeUI(Consumer<FilterNodeUI> onReplace) {
		setBackground(Utilities.sideBarBG.darker());

		JButton resetBtn = new JButton("↻");
		Utilities.initButton(resetBtn);
		resetBtn.setForeground(Color.RED.darker());
		resetBtn.addActionListener(e -> onReplace.accept(new EmptyNodeUI(onReplace)));

		JLabel opLbl = new JLabel(" Contains: ");
		opLbl.setFont(opLbl.getFont().deriveFont(Font.BOLD));
		opLbl.setForeground(Color.WHITE);

		textField = new JTextField(10);
		textField.setBackground(Utilities.ELEMENT_BG);
		textField.setForeground(Color.WHITE);
		textField.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

		add(resetBtn); add(opLbl); add(textField);
	}
	@Override public OntologyElementFilter buildFilter() {
		String query = textField.getText().trim();
		return query.isEmpty() ? null : new OntologyElementFilter.NameContaining(query);
	}
}
}