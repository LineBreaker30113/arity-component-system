package org.halim.acsview.sgui.visual;

import org.halim.acsview.mhexagon.ACSViewService;
import org.halim.acsview.mhexagon.ACSViewService.Arity;
import org.halim.acsview.mhexagon.ACSViewService.Components;
import org.halim.acsview.mhexagon.ACSViewService.Row;
import org.halim.acsview.mhexagon.ACSViewService.Structure;
import org.halim.acsview.sgui.ApplicationController;
import org.halim.acsview.sgui.sglib.WorkSpaceViewPanel;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;


/**
 * The database's components as a tree, as an SQL tool lists a schema's tables and columns: its arities,
 * each with its identity count and its rows, then its structures with the arities they span. Choosing an
 * arity, or a row of one, shows it in the data grid. A structure is listed only: acsess has no request
 * that reads its relations yet.
 * */
public class NavigatorView extends WorkSpaceViewPanel {

/** The ACSE rows offered when adding one; any other class name may be typed. */
static final String[] ROW_CLASSES = {
	"org.halim.acse.arity.catalog.memory.StringValueCatalogArrays",
	"org.halim.acse.arity.catalog.memory.IntegerValueCatalogArraysLong",
	"org.halim.acse.arity.catalog.memory.IntegerValueCatalogArraysInt",
	"org.halim.acse.arity.catalog.memory.FloatingValueCatalogArraysDouble",
	"org.halim.acse.arity.catalog.memory.BooleanValueCatalogBits",
};

final DefaultMutableTreeNode root = new DefaultMutableTreeNode("no database");
final DefaultTreeModel model = new DefaultTreeModel(root);
final JTree tree = new JTree(model);

record ArityNode(Arity arity) {
	@Override public String toString() {
		if(arity.isRemoved()) { return "arity " + arity.slot() + " (removed)"; }
		return "arity " + arity.slot() + ": " + described(arity.description()) + "  [" + arity.identities() + (arity.identities() == 1 ? " identity]" : " identities]");
	}
}

record RowNode(int arity, Row row) {
	@Override public String toString() {
		if(row.isRemoved()) { return "row " + row.slot() + " (removed)"; }
		return "row " + row.slot() + ": " + described(row.description()) + "  (" + simple(row.kind()) + ", " + row.type().name().toLowerCase() + ")";
	}
}

record StructureNode(Structure structure) {
	@Override public String toString() {
		if(structure.isRemoved()) { return "structure " + structure.slot() + " (removed)"; }
		return "structure " + structure.slot() + ": " + described(structure.description()) + "  (" + simple(structure.className()) + ", over arities " + Arrays.toString(structure.arities()) + ")";
	}
}

static String described(String description) { return description == null ? "(no description)" : description; }

static String simple(String kind) { return kind.substring(kind.lastIndexOf('.') + 1); }

public NavigatorView(ApplicationController app) {
	super(app);
	tree.setRootVisible(true);
	tree.addTreeSelectionListener(e -> {
		Object chosen = e.getPath().getLastPathComponent() instanceof DefaultMutableTreeNode node ? node.getUserObject() : null;
		if(chosen instanceof ArityNode node && !node.arity.isRemoved()) { app.wsModeller.broadcastAritySelection(node.arity.slot()); }
		else if(chosen instanceof RowNode node) { app.wsModeller.broadcastAritySelection(node.arity); }
	});
	tree.addMouseListener(new MouseAdapter() {
		@Override public void mousePressed(MouseEvent e) { if(e.isPopupTrigger()) { showMenu(e); } }
		@Override public void mouseReleased(MouseEvent e) { if(e.isPopupTrigger()) { showMenu(e); } }
	});
	add(new JScrollPane(tree), BorderLayout.CENTER);
	JLabel hint = new JLabel("Right click for Add Arity, Add Row and Refresh");
	hint.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
	add(hint, BorderLayout.SOUTH);
	setPreferredSize(new Dimension(360, 600));
}

@Override
public void onSelectedDatabaseChange(ACSViewService.DatabaseService database) {
	if(database == null) {
		show("no database", null);
		return;
	}
	app.request("read the components of " + database.getName(), database::getComponents, components -> show(database.getName(), components));
}

void show(String title, Components components) {
	TreePath selected = tree.getSelectionPath();
	String selectedText = selected == null ? null : selected.getLastPathComponent().toString();
	root.removeAllChildren();
	root.setUserObject(title);
	if(components != null) {
		DefaultMutableTreeNode arities = new DefaultMutableTreeNode("Arities (" + components.arities().size() + ")");
		for(Arity arity : components.arities()) {
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(new ArityNode(arity));
			for(Row row : arity.rows()) { node.add(new DefaultMutableTreeNode(new RowNode(arity.slot(), row))); }
			arities.add(node);
		}
		DefaultMutableTreeNode structures = new DefaultMutableTreeNode("Structures (" + components.structures().size() + ")");
		for(Structure structure : components.structures()) { structures.add(new DefaultMutableTreeNode(new StructureNode(structure))); }
		root.add(arities);
		root.add(structures);
	}
	model.reload();
	for(int i = 0; i < tree.getRowCount(); i++) { tree.expandRow(i); }
	if(selectedText != null) {
		for(int i = 0; i < tree.getRowCount(); i++) {
			if(tree.getPathForRow(i).getLastPathComponent().toString().startsWith(selectedText.split(":")[0] + ":")) {
				tree.getSelectionModel().setSelectionPath(tree.getPathForRow(i));
				break;
			}
		}
	}
}

void showMenu(MouseEvent e) {
	ACSViewService.DatabaseService database = database();
	if(database == null) { return; }
	TreePath path = tree.getPathForLocation(e.getX(), e.getY());
	Object chosen = path != null && path.getLastPathComponent() instanceof DefaultMutableTreeNode node ? node.getUserObject() : null;
	int arity = chosen instanceof ArityNode node ? node.arity.slot() : chosen instanceof RowNode node ? node.arity : -1;

	JPopupMenu menu = new JPopupMenu();
	JMenuItem addArity = new JMenuItem("Add Arity...");
	addArity.addActionListener(a -> {
		String description = JOptionPane.showInputDialog(this, "The new arity's description:", "Add Arity", JOptionPane.PLAIN_MESSAGE);
		if(description != null) { app.perform("add an arity", () -> database.addArity(description), () -> app.wsModeller.triggerDatabaseRefresh(database)); }
	});
	menu.add(addArity);
	if(arity >= 0) {
		JMenuItem addRow = new JMenuItem("Add Row to Arity " + arity + "...");
		addRow.addActionListener(a -> addRow(database, arity));
		menu.add(addRow);
	}
	menu.addSeparator();
	JMenuItem refresh = new JMenuItem("Refresh");
	refresh.addActionListener(a -> app.wsModeller.triggerDatabaseRefresh(database));
	menu.add(refresh);
	menu.show(tree, e.getX(), e.getY());
}

void addRow(ACSViewService.DatabaseService database, int arity) {
	JComboBox<String> className = new JComboBox<>(ROW_CLASSES);
	className.setEditable(true);
	JTextField description = new JTextField(24);
	JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
	form.add(new JLabel("The row's class, as the database's classes name it:"));
	form.add(className);
	form.add(new JLabel("Its description:"));
	form.add(description);
	if(JOptionPane.showConfirmDialog(this, form, "Add Row to Arity " + arity, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) { return; }
	String chosenClass = String.valueOf(className.getSelectedItem()).trim();
	app.request("add a row to arity " + arity, () -> database.addCatalogRow(arity, chosenClass, description.getText()), row -> app.wsModeller.triggerDatabaseRefresh(database));
}

}
