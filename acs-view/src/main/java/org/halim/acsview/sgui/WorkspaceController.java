package org.halim.acsview.sgui;

import org.halim.acsview.mhexagon.ACSViewService;
import org.halim.acsview.sgui.sglib.WorkspaceListener;

import java.util.ArrayList;
import java.util.List;


/** The workspace's state, the shown database and its chosen arity, fanned out to the views, as OntoDirectory's WorkspaceController fans out the lake and the tag. */
public class WorkspaceController {

public final ApplicationController owner;
final List<WorkspaceListener> workspaceListeners = new ArrayList<>();
int selectedArity = -1;

public WorkspaceController(ApplicationController owner) { this.owner = owner; }

public void registerListener(WorkspaceListener listener) {
	if(!workspaceListeners.contains(listener)) { workspaceListeners.add(listener); }
}

/** After any change, or with null when no database is shown: every view reads again. */
public void triggerDatabaseRefresh(ACSViewService.DatabaseService database) {
	if(database == null) { selectedArity = -1; }
	for(WorkspaceListener listener : workspaceListeners) { listener.onSelectedDatabaseChange(database); }
}

public void broadcastAritySelection(int arity) {
	selectedArity = arity;
	for(WorkspaceListener listener : workspaceListeners) { listener.onSelectedArityChange(arity); }
}

public int getSelectedArity() { return selectedArity; }

}
