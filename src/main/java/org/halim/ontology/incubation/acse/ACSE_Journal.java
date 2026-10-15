package org.halim.ontology.incubation.acse;

import java.io.DataOutputStream;
import java.io.IOException;

public interface ACSE_Journal {

/** Begins recording a mutation on the structure. The paramsMemory is for communicating parameters using a single allocation, and itself will not be recorded, defined as an object so client choose to use an int[] or IntArrayList. */
int beginRecordingActivity(TraceableForUndoingRedoing actor, String methodName, Object paramsMemory, String comment);
/** Completes the last recording of mutation on the structure. */
int completeRecordingActivity();
/** Undoes what the innermost open request recorded, nested requests included, and closes it. */
int abortRecordingActivity();
/** Whether a request is open, so what must run between requests - storing, loading - can refuse. */
boolean isRecordingActivity();
/** paramCount states how many first elements of the paramsMemory must be copied. */
void recordActivityStep(byte stepPair, int paramCount);
/** Returns the index to pass among a step's params. */
int recordBoolean(boolean value);
/** Returns the index to pass among a step's params. */
int recordObject(Object value);
boolean getBoolean(int index);
Object getObject(int index);
/** Begins a request that does not mutate, it is not bound to be a single method and provider gives the full description */
int recordQueryRequest(String description);
/** Completes a request that does not mutate, it is not bound to be a single method and provider gives the full description */
int recordQueryResponse(String description);

void undoLastActivity();
void redoLastActivity();
void undoUntilActivityIndex(int activityIndex);
void redoUntilActivityIndex(int activityIndex);
/** Undoes or redoes as needed. */
void checkout(int activityIndex);

void storeAll(DataOutputStream dos) throws IOException;

/**
 * Since multistep activities require each step being undone for restoring the former structure, we need to record the steps each.
 * A step is a unit action that changes only one thing, deterministic, doesn't matter at which order it is done, and undoable.
 * Important detail is the structure is responsible only for restoring the data, it doesn't matter how the data is represented,
 * adding an element may increase the capacity of the arraylist, but undoing is not responsible for reducing its capacity back.
 **/
interface TraceableForUndoingRedoing {
	/** stepPair index starts from 1 instead of 0, and negative means the inverse, and the 0 means not undoable */
	void doTraceableStep(byte stepPair, int[] params, int index);
//	default boolean isOrderDependent() { return false; } out of scope
}

}
