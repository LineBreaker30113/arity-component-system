package org.halim.ontology.incubation.acse_beta;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface ACSE_StructureComponentBase extends ACSE_Journal.TraceableForUndoingRedoing {

int getArityCount();
int getArityIdentity(int index);
boolean isRelatedArity(int identity);

boolean loadFrom(DataInputStream dis) throws IOException;
boolean saveTo(DataOutputStream dos) throws IOException;

/**
 * Records its steps, so it undoes with the request that called it. To remove an identity of another
 * arity, call {@link ACSE_Core#delegateRemoval} from here: that is the recursive step.
 * */
boolean forget(int arity, int identity);


}
