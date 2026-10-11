package org.halim.ontology.incubation.acse_beta;

public interface ACSE_Core extends ACSE_Journal.TraceableForUndoingRedoing {

int getArityCount();

ACSE_ArityComponentBase getArity(int arity);

/** Adds a new "Modular Catalog Arity."*/
void addArity(String comment);

void addArity(ACSE_ArityComponentBase acseArityComponent, String comment);

int getStructureCount();
ACSE_StructureComponentBase getStructure(int structure);

void addStructure(ACSE_StructureComponentBase acseArityComponent, String comment);

/** The slot stays empty; later slots do not shift. */
void removeArity(int arity, String comment);
/** The slot stays empty; later slots do not shift. */
void removeStructure(int structure, String comment);

/** Removes the identity from the arity, then every related structure forgets it. Does nothing if the identity is absent, which ends the recursion. */
void delegateRemoval(int arity, int instance, String comment);

/** Takes the arity's lowest free identity, after the structures forget anything left on it. */
long captureIdentity(int arity, String comment);



}
