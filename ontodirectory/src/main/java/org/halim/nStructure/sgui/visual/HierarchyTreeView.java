package org.halim.nStructure.sgui.visual;

import org.halim.nStructure.mhexagon.OntoDirectoryService;
import org.halim.nStructure.mhexagon.OntologyClassHandle;
import org.halim.nStructure.mhexagon.OntologyElementHandle;
import org.halim.nStructure.mhexagon.OntologyReadingService;
import org.halim.nStructure.sgui.ApplicationController;
import org.halim.nStructure.sgui.sglib.HierarchyView;
import org.halim.nStructure.sgui.sglib.Utilities;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.Queue;

public class HierarchyTreeView extends HierarchyView {

private final ApplicationController mac;
private final JTree tree;
private DefaultTreeModel treeModel;
private OntoDirectoryService.DataLakeService currentLakeService = null;

public HierarchyTreeView(ApplicationController mac) {
	this.mac = mac;
	setLayout(new BorderLayout());

	tree = new JTree((javax.swing.tree.TreeNode) null);
	tree.setRootVisible(true);
	tree.setShowsRootHandles(true);
	tree.setBackground(new Color(40, 40, 40));
	tree.setForeground(Color.WHITE);
	tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

	tree.addMouseListener(new MouseAdapter() {
		@Override public void mousePressed(MouseEvent e) { handleMouseClick(e); }
		@Override public void mouseReleased(MouseEvent e) { handleMouseClick(e); }
	});

	// DRAG AND DROP CAPABILITIES
	tree.setDragEnabled(true);
	tree.setDropMode(DropMode.ON);
	tree.setTransferHandler(new TransferHandler() {
		@Override public int getSourceActions(JComponent c) { return COPY; }

		// DRAG OUT (Tags -> FilesView AST)
		@Override protected java.awt.datatransfer.Transferable createTransferable(JComponent c) {
			List<Utilities.TagNodeDto> selectedTags = new ArrayList<>();
			TreePath[] paths = tree.getSelectionPaths();
			if (paths != null) {
				for (TreePath path : paths) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
					selectedTags.add((Utilities.TagNodeDto) node.getUserObject());
				}
			}
			return new Utilities.GenericTransferable(selectedTags, Utilities.TAG_LIST_FLAVOR);
		}

		// DROP IN (FilesView Files -> Tags)
		@Override public boolean canImport(TransferSupport support) {
			return support.isDataFlavorSupported(Utilities.FILE_LIST_FLAVOR);
		}

		@Override public boolean importData(TransferSupport support) {
			if (!canImport(support)) return false;
			try {
				// 1. Extract as a raw object first
				Object data = support.getTransferable().getTransferData(Utilities.FILE_LIST_FLAVOR);

				// 2. Defensively verify it is a List containing OntologyElementHandles
				if (!(data instanceof List<?> rawList) || rawList.isEmpty() || !(rawList.get(0) instanceof OntologyElementHandle)) {
					return false;
				}

				// 3. Now we can safely suppress the warning and cast
				@SuppressWarnings("unchecked")
				List<OntologyElementHandle> files = (List<OntologyElementHandle>) rawList;

				JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
				TreePath path = dropLocation.getPath();
				if (path == null) return false;

				// Determine targets: If dropped on a selected node and multi-selection exists, apply to all.
				// Otherwise, just apply to the single node dropped upon.
				boolean droppedOnSelection = false;
				if (tree.getSelectionPaths() != null) {
					for (TreePath sp : tree.getSelectionPaths()) {
						if (sp.equals(path)) { droppedOnSelection = true; break; }
					}
				}

				List<Utilities.TagNodeDto> targets = new ArrayList<>();
				if (droppedOnSelection && tree.getSelectionCount() > 1) {
					for (TreePath sp : tree.getSelectionPaths()) {
						targets.add((Utilities.TagNodeDto) ((DefaultMutableTreeNode) sp.getLastPathComponent()).getUserObject());
					}
				} else {
					targets.add((Utilities.TagNodeDto) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject());
				}

				String targetNames = targets.size() == 1 ? targets.get(0).name() : targets.size() + " selected tags";
				int res = JOptionPane.showConfirmDialog(HierarchyTreeView.this, "Assign " + files.size() + " files to " + targetNames + "?", "Confirm Assignment", JOptionPane.YES_NO_OPTION);

				if (res == JOptionPane.YES_OPTION) {
					OntologyReadingService.OntologyManagingService oms = currentLakeService.getOntologyManagingService();
					for (Utilities.TagNodeDto target : targets) {
						for (OntologyElementHandle fi : files) {
							oms.addElement(target.identity(), fi.getIdentity());
						}
					}
					return true;
				}
			} catch (Exception ex) { ex.printStackTrace(); }
			return false;
		}
	});

	JScrollPane scrollPane = new JScrollPane(tree);
	scrollPane.setBorder(null);
	add(scrollPane, BorderLayout.CENTER);
}

private void handleMouseClick(@NotNull MouseEvent e) {
	int selRow = tree.getClosestRowForLocation(e.getX(), e.getY());
	if (selRow == -1) return;

	if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
		tree.requestFocusInWindow();

		// Respect Multi-Selection on Right Click
		boolean inSelection = false;
		if (tree.getSelectionRows() != null) {
			for (int r : tree.getSelectionRows()) {
				if (r == selRow) { inSelection = true; break; }
			}
		}
		if (!inSelection) {
			tree.setSelectionRow(selRow);
		}

		showContextMenu(e);

	} else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
		TreePath path = tree.getPathForRow(selRow);
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
		Utilities.TagNodeDto clickedTag = (Utilities.TagNodeDto) node.getUserObject();
		mac.wsModeller.broadcastTagSelection(clickedTag.identity());
	}
}

private void showContextMenu(MouseEvent e) {
	if (currentLakeService == null) return;
	OntologyReadingService.OntologyManagingService manager = currentLakeService.getOntologyManagingService();

	List<Utilities.TagNodeDto> selectedTags = new ArrayList<>();
	if (tree.getSelectionPaths() != null) {
		for (TreePath path : tree.getSelectionPaths()) {
			selectedTags.add((Utilities.TagNodeDto) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject());
		}
	}

	if (selectedTags.isEmpty()) return;

	JPopupMenu contextMenu = new JPopupMenu();

	if (selectedTags.size() == 1) {
		Utilities.TagNodeDto clickedTag = selectedTags.get(0);
		OntologyClassHandle targetClass = manager.getClassFromIdentity(clickedTag.identity());

		JMenuItem createTag = new JMenuItem("Create Sub-Tag Here");
		JMenuItem addParent = new JMenuItem("Add Parent...");
		JMenuItem addChild = new JMenuItem("Add Existing Tag as Child...");
		JMenuItem removeParent = new JMenuItem("Remove a Parent...");
		JMenuItem removeChild = new JMenuItem("Remove a Child...");
		JMenuItem deleteTag = new JMenuItem("Delete Tag Completely");
		JMenuItem renameTag = new JMenuItem("Rename Tag");
		JMenuItem copyContents = new JMenuItem("Copy Contents To...");

		createTag.addActionListener(a -> {
			String newName = JOptionPane.showInputDialog(this, "Name for new tag under " + clickedTag.name() + ":");
			if (newName != null && !newName.trim().isEmpty()) {
				manager.createOntologyClass(newName.trim(), List.of(clickedTag.identity()), null);
				refreshTreeUI();
			}
		});

		addParent.addActionListener(a -> {
			Integer parentId = showTagSelector("Select New Parent", getAllTags());
			if (parentId != null) {
				manager.addParent(clickedTag.identity(), parentId);
				refreshTreeUI();
			}
		});

		addChild.addActionListener(a -> {
			Integer childId = showTagSelector("Select Existing Tag to make a Child", getAllTags());
			if (childId != null) {
				manager.addParent(childId, clickedTag.identity());
				refreshTreeUI();
			}
		});

		removeParent.addActionListener(a -> {
			List<Utilities.TagNodeDto> parents = targetClass.getSafeParents().stream()
				  .map(p -> new Utilities.TagNodeDto(p.getIdentity(), p.getName())).toList();
			Integer parentId = showTagSelector("Remove which Parent?", parents);
			if (parentId != null) {
				manager.removeParent(clickedTag.identity(), parentId);
				refreshTreeUI();
			}
		});

		removeChild.addActionListener(a -> {
			List<Utilities.TagNodeDto> children = targetClass.getSafeChildren().stream()
				  .map(c -> new Utilities.TagNodeDto(c.getIdentity(), c.getName())).toList();
			Integer childId = showTagSelector("Remove which Child?", children);
			if (childId != null) {
				manager.removeParent(childId, clickedTag.identity());
				refreshTreeUI();
			}
		});

		deleteTag.addActionListener(a -> {
			int result = JOptionPane.showConfirmDialog(this, "Delete tag '" + clickedTag.name() + "'? This will orphan its children.", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.YES_OPTION) {
				manager.removeOntologyClass(clickedTag.identity());
				refreshTreeUI();
			}
		});

		renameTag.addActionListener(a -> {
			String newName = (String) JOptionPane.showInputDialog(this, "New name for tag:", "Rename", JOptionPane.PLAIN_MESSAGE, null, null, clickedTag.name());
			if (newName != null && !newName.trim().isEmpty() && !newName.equals(clickedTag.name())) {
				manager.renameOntologyClass(clickedTag.identity(), newName.trim());
				refreshTreeUI();
			}
		});

		copyContents.addActionListener(a -> showCopyContentsDialog(clickedTag.identity()));

		contextMenu.add(createTag);
		contextMenu.add(addParent);
		contextMenu.add(addChild);
		contextMenu.addSeparator();
		contextMenu.add(removeParent);
		contextMenu.add(removeChild);
		contextMenu.add(deleteTag);
		contextMenu.addSeparator();
		contextMenu.add(renameTag);
		contextMenu.add(copyContents);

	} else {
		// Bulk Operations for Multi-Selection
		JMenuItem bulkDelete = new JMenuItem("Delete All Selected Tags");
		bulkDelete.addActionListener(a -> {
			int result = JOptionPane.showConfirmDialog(this, "Delete " + selectedTags.size() + " tags? This cannot be undone in bulk.", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.YES_OPTION) {
				for (Utilities.TagNodeDto tag : selectedTags) {
					manager.removeOntologyClass(tag.identity());
				}
				refreshTreeUI();
			}
		});
		contextMenu.add(bulkDelete);
	}

	contextMenu.addSeparator();
	JMenuItem undoAct = new JMenuItem("Undo Last Action");
	JMenuItem redoAct = new JMenuItem("Redo Last Action");
	undoAct.addActionListener(a -> { manager.undo(); refreshTreeUI(); });
	redoAct.addActionListener(a -> { manager.redo(); refreshTreeUI(); });
	contextMenu.add(undoAct);
	contextMenu.add(redoAct);

	contextMenu.show(tree, e.getX(), e.getY());
}

private Integer showTagSelector(String title, List<Utilities.TagNodeDto> tags) {
	if (tags.isEmpty()) {
		JOptionPane.showMessageDialog(this, "No valid tags available for this operation.", "Empty List", JOptionPane.INFORMATION_MESSAGE);
		return null;
	}
	JList<Utilities.TagNodeDto> list = new JList<>(tags.toArray(new Utilities.TagNodeDto[0]));
	list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	JScrollPane scrollPane = new JScrollPane(list);
	scrollPane.setPreferredSize(new Dimension(250, 300));

	int result = JOptionPane.showConfirmDialog(this, scrollPane, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
	if (result == JOptionPane.OK_OPTION && list.getSelectedValue() != null) {
		return list.getSelectedValue().identity();
	}
	return null;
}

private void showCopyContentsDialog(int sourceId) {
	List<Utilities.TagNodeDto> allTags = getAllTags();
	JComboBox<Utilities.TagNodeDto> targetDropdown = new JComboBox<>(allTags.toArray(new Utilities.TagNodeDto[0]));
	JCheckBox copyParents = new JCheckBox("Copy Parents (Link upwards)", true);
	JCheckBox copyChildren = new JCheckBox("Copy Children (Link downwards)", true);
	JCheckBox copyFiles = new JCheckBox("Copy Associated Files", true);

	JPanel panel = new JPanel(new GridLayout(5, 1, 0, 5));
	panel.add(new JLabel("Select Target Tag:"));
	panel.add(targetDropdown);
	panel.add(copyParents);
	panel.add(copyChildren);
	panel.add(copyFiles);

	int result = JOptionPane.showConfirmDialog(this, panel, "Copy Graph Contents", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
	if (result == JOptionPane.OK_OPTION && targetDropdown.getSelectedItem() != null) {
		Utilities.TagNodeDto target = (Utilities.TagNodeDto) targetDropdown.getSelectedItem();
		currentLakeService.getOntologyManagingService().copyContentsTo(
			  sourceId, target.identity(), copyParents.isSelected(), copyChildren.isSelected(), copyFiles.isSelected()
		);
		refreshTreeUI();
	}
}

private List<Utilities.TagNodeDto> getAllTags() {
	List<Utilities.TagNodeDto> tags = new ArrayList<>();
	OntologyReadingService reader = currentLakeService.getOntologyReadingService();
	OntologyClassHandle root = reader.getRootOntologyClass();
	if (root == null) return tags;

	Queue<OntologyClassHandle> q = new LinkedList<>();
	Set<Integer> visited = new HashSet<>();
	q.add(root);
	visited.add(root.getIdentity());

	while (!q.isEmpty()) {
		OntologyClassHandle curr = q.poll();
		tags.add(new Utilities.TagNodeDto(curr.getIdentity(), curr.getName()));
		for (OntologyClassHandle child : curr.getSafeChildren()) {
			if (visited.add(child.getIdentity())) {
				q.add(child);
			}
		}
	}
	tags.sort(Comparator.comparing(Utilities.TagNodeDto::name));
	return tags;
}

private void refreshTreeUI() {
	mac.wsModeller.triggerLakeRefresh(currentLakeService);
}

@Override
protected void clearHierarchy() {
	if (treeModel != null) {
		treeModel.setRoot(null);
		treeModel.reload();
	}
}

@Override
public void onSelectedLakeChange(OntoDirectoryService.DataLakeService dataLakeService) {
	Enumeration<TreePath> expandedPaths = null;
	if (tree.getModel() != null && tree.getModel().getRoot() != null) {
		expandedPaths = tree.getExpandedDescendants(new TreePath(tree.getModel().getRoot()));
	}
	clearHierarchy();
	this.currentLakeService = dataLakeService;

	if (dataLakeService == null) return;
	OntologyReadingService readerService = dataLakeService.getOntologyReadingService();
	if (readerService == null) return;

	OntologyClassHandle rootClass = readerService.getRootOntologyClass();
	if (rootClass == null) return;

	Utilities.TagNodeDto rootDto = new Utilities.TagNodeDto(rootClass.getIdentity(), rootClass.getName());
	DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootDto);
	treeModel = new DefaultTreeModel(rootNode);

	Queue<Object[]> queue = new LinkedList<>();
	queue.add(new Object[]{rootClass, rootNode});

	while (!queue.isEmpty()) {
		Object[] current = queue.poll();
		OntologyClassHandle currentClass = (OntologyClassHandle) current[0];
		DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) current[1];

		for (OntologyClassHandle childClass : currentClass.getSafeChildren()) {
			Utilities.TagNodeDto childDto = new Utilities.TagNodeDto(childClass.getIdentity(), childClass.getName());
			DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(childDto);
			currentNode.add(childNode);
			queue.add(new Object[]{childClass, childNode});
		}
	}

	tree.setModel(treeModel);

	if (expandedPaths != null) {
		while (expandedPaths.hasMoreElements()) {
			TreePath path = expandedPaths.nextElement();
			restorePath(tree, rootNode, path, 0);
		}
	} else {
		tree.expandRow(0);
	}
}

private void restorePath(JTree tree, DefaultMutableTreeNode currentNode, TreePath oldPath, int depth) {
	if (depth >= oldPath.getPathCount()) return;

	String targetName = oldPath.getPathComponent(depth).toString();
	if (currentNode.getUserObject().toString().equals(targetName)) {
		tree.expandPath(new TreePath(currentNode.getPath()));

		for (int i = 0; i < currentNode.getChildCount(); i++) {
			restorePath(tree, (DefaultMutableTreeNode) currentNode.getChildAt(i), oldPath, depth + 1);
		}
	}
}

@Override public void onSelectedClassChange(OntologyClassHandle ontologyClass) { }
@Override public void onDomainChange(OntologyClassHandle newDomain) { }
}