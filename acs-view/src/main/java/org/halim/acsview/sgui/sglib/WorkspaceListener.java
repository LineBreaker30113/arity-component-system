package org.halim.acsview.sgui.sglib;

import org.halim.acsview.mhexagon.ACSViewService;


/** What a workspace view hears: the shown database, null when none, read again after any change; and the arity chosen in the navigator. */
public interface WorkspaceListener {

void onSelectedDatabaseChange(ACSViewService.DatabaseService database);

void onSelectedArityChange(int arity);

}
