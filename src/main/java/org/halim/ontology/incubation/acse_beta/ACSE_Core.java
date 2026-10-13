package org.halim.ontology.incubation.acse_beta;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface ACSE_Core extends ACSE_Journal.TraceableForUndoingRedoing {

int getArityCount();

ACSE_ArityComponentBase getArity(int arity);

/** Adds a new "Composable Catalog Arity."*/
void addArity(String description, String comment);

void addArity(ACSE_ArityComponentBase acseArityComponent, String description, String comment);

/** Delegated to the arity, so only an {@link ACSE_ComposableArityCatalog} takes one; returns the row's slot. */
int addCatalogRow(int arity, ACSE_ArityCatalogRowBase row, String description, String comment);

int getStructureCount();
ACSE_StructureComponentBase getStructure(int structure);

void addStructure(ACSE_StructureComponentBase acseArityComponent, String description, String comment);

/** The slot stays empty; later slots do not shift. */
void removeArity(int arity, String comment);
/** The slot stays empty; later slots do not shift. */
void removeStructure(int structure, String comment);

/** Removes the identity from the arity, then every related structure forgets it. Does nothing if the identity is absent, which ends the recursion. */
void delegateRemoval(int arity, int instance, String comment);

/** Takes the arity's lowest free identity, after the structures forget anything left on it. */
long captureIdentity(int arity, String comment);


/**
 * Writes every component, every description, the journal's log and the classes they need into
 * {@code folder}, between requests. A component or catalog row whose class has no static loadFrom
 * likely does not need to persist: it is left out, loads back empty, and is named in the warnings this
 * gives back.
 * */
List<String> store(Path folder) throws IOException;

/**
 * Fills this core, which must hold no component, from what {@link #store} wrote into {@code folder},
 * outside the journal, so undo history begins here. The folder's classes are found through this core's
 * own class loader, so a folder carrying its classes is loaded through
 * {@link ACSE_PersistenceManager#loadCore}, which builds the core, and the journal the folder names, from
 * them. A failed load leaves the core as it was.
 * */
void load(Path folder) throws IOException;


}
