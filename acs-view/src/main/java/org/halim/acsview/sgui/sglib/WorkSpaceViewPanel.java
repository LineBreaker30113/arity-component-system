package org.halim.acsview.sgui.sglib;

import org.halim.acsview.mhexagon.ACSViewService;
import org.halim.acsview.sgui.ApplicationController;

import javax.swing.*;
import java.awt.*;


/** A workspace view: it hears the workspace and reads through the controller's request thread; what it does not care about it leaves as a no-op. */
public abstract class WorkSpaceViewPanel extends JPanel implements WorkspaceListener {

protected final ApplicationController app;

protected WorkSpaceViewPanel(ApplicationController app) {
	this.app = app;
	setLayout(new BorderLayout());
	setBackground(Utilities.WP_BG);
}

protected ACSViewService.DatabaseService database() { return app.servicePort.getActiveDatabase(); }

@Override public void onSelectedDatabaseChange(ACSViewService.DatabaseService database) { }

@Override public void onSelectedArityChange(int arity) { }

}
