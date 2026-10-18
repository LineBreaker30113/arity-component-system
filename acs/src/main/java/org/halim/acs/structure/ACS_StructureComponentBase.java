package org.halim.acs.structure;


import org.halim.acs.ACS_Journal;
import org.halim.acs.ACS_Core;

import java.io.DataOutputStream;
import java.io.IOException;

public interface ACS_StructureComponentBase extends ACS_Journal.TraceableForUndoingRedoing {

int getArityCount();
int getArityIdentity(int index);
boolean isRelatedArity(int identity);

/**
 * Writes the arity slots it spans and the relation, so that the class's {@code public static
 * loadFrom(ACS_Core, ACS_Journal, DataInputStream)} rebuilds the same structure, attached to the core,
 * outside the journal. One file per structure.
 * */
boolean saveTo(DataOutputStream dos) throws IOException;

/**
 * Records its steps, so it undoes with the request that called it. To remove an identity of another
 * arity, call {@link ACS_Core#delegateRemoval} from here: that is the recursive step.
 * */
boolean forget(int arity, int identity);


}
