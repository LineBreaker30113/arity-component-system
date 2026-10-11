package org.halim.ontology.incubation.acse_beta;

import org.halim.ontology.incubation.acse_beta.sets.IdentitiesSetNegativeInt;

import java.util.ArrayList;

/** Components by slot, with their addition and removal journaled. Nothing here is multi thread safe. */
public class ACSE_CoreBasic implements ACSE_Core {

public final ACSE_Journal journal = new ACSE_JournalBasic(this);
final ArrayList<ACSE_ArityComponentBase> arities = new ArrayList<>();
final ArrayList<ACSE_StructureComponentBase> structures = new ArrayList<>();
final int[] params = new int[2];

static final byte ARITY = 1, STRUCTURE = 2;


@Override
public int getArityCount() { return arities.size(); }

@Override
public ACSE_ArityComponentBase getArity(int arity) { return arities.get(arity); }

@Override
public int getStructureCount() { return structures.size(); }

@Override
public ACSE_StructureComponentBase getStructure(int structure) { return structures.get(structure); }


@Override
public void addArity(String comment) { addArity(new ACSE_ModularArityCatalog(new IdentitiesSetNegativeInt(-1), journal), comment); }

@Override
public void addArity(ACSE_ArityComponentBase acseArityComponent, String comment) { addComponent(arities, acseArityComponent, ARITY, "addArity", comment); }

@Override
public void addStructure(ACSE_StructureComponentBase acseArityComponent, String comment) { addComponent(structures, acseArityComponent, STRUCTURE, "addStructure", comment); }

@Override
public void removeArity(int arity, String comment) { removeComponent(arities, arity, ARITY, "removeArity", comment); }

@Override
public void removeStructure(int structure, String comment) { removeComponent(structures, structure, STRUCTURE, "removeStructure", comment); }

@Override
public void delegateRemoval(int arity, int instance, String comment) {
	if(!arities.get(arity).occupied().contains(instance)) { return; }
	int request = journal.beginRecordingActivity(this, "delegateRemoval", params, comment);
	try {
		arities.get(arity).removeIf(instance, null);
		forgetEverywhere(arity, instance);
	} catch(RuntimeException | Error e) {
		abort(request);
		throw e;
	}
	complete(request);
}

@Override
public long captureIdentity(int arity, String comment) {
	int request = journal.beginRecordingActivity(this, "captureIdentity", params, comment);
	int identity;
	try {
		ACSE_ArityComponentBase target = arities.get(arity);
		identity = Math.toIntExact(target.occupied().lowestFree());
		forgetEverywhere(arity, identity);
		target.registerIf(identity, null);
	} catch(RuntimeException | Error e) {
		abort(request);
		throw e;
	}
	complete(request);
	return identity;
}

void forgetEverywhere(int arity, int identity) {
	for(ACSE_StructureComponentBase structure : structures) {
		if(structure != null && structure.isRelatedArity(arity)) { structure.forget(arity, identity); }
	}
}


<T> void addComponent(ArrayList<T> list, T component, byte pair, String methodName, String comment) {
	int request = journal.beginRecordingActivity(this, methodName, params, comment);
	list.add(component);
	params[0] = list.size() - 1;
	params[1] = journal.recordObject(component);
	journal.recordActivityStep(pair, 2);
	complete(request);
}

<T> void removeComponent(ArrayList<T> list, int slot, byte pair, String methodName, String comment) {
	T removed = list.get(slot);
	if(removed == null) { throw new IllegalStateException(methodName + ": slot " + slot + " is already empty"); }
	int request = journal.beginRecordingActivity(this, methodName, params, comment);
	list.set(slot, null);
	params[0] = slot;
	params[1] = journal.recordObject(removed);
	journal.recordActivityStep((byte) -pair, 2);
	complete(request);
}

@Override
public void doTraceableStep(byte stepPair, int[] params, int index) {
	switch(stepPair) {
		case ARITY -> put(arities, params[index], (ACSE_ArityComponentBase) journal.getObject(params[index + 1]));
		case -ARITY -> arities.set(params[index], null);
		case STRUCTURE -> put(structures, params[index], (ACSE_StructureComponentBase) journal.getObject(params[index + 1]));
		case -STRUCTURE -> structures.set(params[index], null);
		default -> throw new IllegalArgumentException("no step " + stepPair);
	}
}

static <T> void put(ArrayList<T> list, int slot, T component) {
	if(slot == list.size()) { list.add(component); }
	else { list.set(slot, component); }
}

/** Aborts nested requests left open by the failure too, up to and including {@code request}. */
void abort(int request) {
	while(journal.abortRecordingActivity() != request) { }
}

void complete(int request) {
	int completed = journal.completeRecordingActivity();
	if(completed != request) { throw new IllegalStateException("began request " + request + ", completed " + completed); }
}

}
