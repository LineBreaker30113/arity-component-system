package org.halim.acsview.sgui.visual;

import org.halim.acsview.mhexagon.ACSViewService;
import org.halim.acsview.sgui.ApplicationController;
import org.halim.acsview.sgui.sglib.Utilities;
import org.halim.acsview.sgui.sglib.WorkSpaceViewPanel;

import javax.swing.*;
import java.awt.*;


/** The shown database's journal: where it stands and how much redo holds, undo and redo of one activity, and storing. */
public class JournalView extends WorkSpaceViewPanel {

final JLabel state = new JLabel("No database is shown.");

public JournalView(ApplicationController app) {
	super(app);
	JPanel body = new JPanel();
	body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
	body.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
	state.setForeground(Utilities.ELEMENT_TEXT_PRIMARY);
	state.setAlignmentX(Component.LEFT_ALIGNMENT);
	body.add(state);
	body.add(Box.createVerticalStrut(12));

	JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
	tools.setAlignmentX(Component.LEFT_ALIGNMENT);
	JButton undo = new JButton("Undo (Ctrl+Z)");
	undo.addActionListener(e -> app.undo());
	JButton redo = new JButton("Redo (Ctrl+Y)");
	redo.addActionListener(e -> app.redo());
	JButton store = new JButton("Store");
	store.addActionListener(e -> {
		ACSViewService.DatabaseService database = database();
		if(database != null) { app.request("store " + database.getName(), database::store, answer -> JOptionPane.showMessageDialog(this, answer, "Stored", JOptionPane.INFORMATION_MESSAGE)); }
	});
	tools.add(undo);
	tools.add(redo);
	tools.add(store);
	body.add(tools);
	add(body, BorderLayout.NORTH);
	setPreferredSize(new Dimension(320, 600));
}

record Standing(int index, int pending) {}

@Override
public void onSelectedDatabaseChange(ACSViewService.DatabaseService database) {
	if(database == null) {
		state.setText("No database is shown.");
		return;
	}
	app.request("read the journal of " + database.getName(), () -> new Standing(database.getCurrentActivityIndex(), database.getPendingActivityCount()), standing ->
		state.setText("<html>" + WelcomePage.escaped(database.getName()) + "<br>activity index: " + standing.index() + "<br>undone, redo holds: " + standing.pending() + "</html>"));
}

}
