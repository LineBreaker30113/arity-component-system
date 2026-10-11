package org.halim.ontology.incubation.acse_beta;

import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Requests and their steps in flat lists. A client request and every request nested in it form one
 * transaction, which undo and redo move as a whole; a new client request drops the transactions undone
 * before it. Nothing here is multi thread safe.
 * */
public class ACSE_JournalBasic implements ACSE_Journal {

final ACSE_Core core;

/** Steps as [pair, count, params...]. */
final IntArrayList data = new IntArrayList();
final BooleanArrayList booleans = new BooleanArrayList();
final ArrayList<Object> objects = new ArrayList<>();
/** [dataStart, request] per run of steps by one request; a new run opens whenever the recording request changes. */
final IntArrayList segments = new IntArrayList();
/** [transaction, firstSegment, actor, parent, booleansStart, objectsStart] per request, nested ones included; parent is -1 for a client request. */
final IntArrayList requests = new IntArrayList();
static final int FIELDS = 6, TRANSACTION = 0, SEGMENT = 1, ACTOR = 2, PARENT = 3, BOOLEANS = 4, OBJECTS = 5;
/** [methodName, comment] per request. */
final ArrayList<String> texts = new ArrayList<>();
/** First request of each transaction. */
final IntArrayList transactions = new IntArrayList();
/** [request, response] per query. */
final ArrayList<String> queries = new ArrayList<>();

final IntArrayList openRequests = new IntArrayList();
final ArrayList<Object> openParams = new ArrayList<>();
final IntArrayList openQueries = new IntArrayList();

/** Transactions applied; the ones from here on are undone. */
int done;
/** Transactions below this can no longer be undone. */
int horizon;
boolean replaying;

public ACSE_JournalBasic(ACSE_Core core) { this.core = core; }


@Override
public int beginRecordingActivity(TraceableForUndoingRedoing actor, String methodName, Object paramsMemory, String comment) {
	checkNotReplaying();
	int actorId = actorIdOf(actor);
	int parent = openRequests.isEmpty() ? -1 : openRequests.topInt();
	if(parent < 0) {
		dropRedo();
		transactions.add(requestCount());
	}
	int request = requestCount();
	openSegment(request);
	requests.add(transactionCount() - 1);
	requests.add(segmentCount() - 1);
	requests.add(actorId);
	requests.add(parent);
	requests.add(booleans.size());
	requests.add(objects.size());
	texts.add(methodName);
	texts.add(comment);
	openRequests.push(request);
	openParams.add(paramsMemory);
	return request;
}

@Override
public int completeRecordingActivity() {
	checkOpen();
	int request = openRequests.popInt();
	openParams.remove(openParams.size() - 1);
	if(openRequests.isEmpty()) { done = transactionCount(); }
	else { openSegment(openRequests.topInt()); }
	return request;
}

@Override
public int abortRecordingActivity() {
	checkOpen();
	int request = openRequests.topInt();
	int firstSegment = field(request, SEGMENT);
	int dataStart = segments.getInt(2 * firstSegment);
	for(int at = dataStart; at < data.size(); at += 2 + data.getInt(at + 1)) {
		if(data.getInt(at) == 0) { throw new IllegalStateException("request " + request + " recorded a step that cannot be undone"); }
	}
	replaying = true;
	try { for(int segment = segmentCount() - 1; segment >= firstSegment; segment--) { replaySegment(segment, true); } }
	finally { replaying = false; }
	data.size(dataStart);
	booleans.size(field(request, BOOLEANS));
	objects.subList(field(request, OBJECTS), objects.size()).clear();
	segments.size(2 * firstSegment);
	requests.size(FIELDS * request);
	texts.subList(2 * request, texts.size()).clear();
	openRequests.popInt();
	openParams.remove(openParams.size() - 1);
	if(openRequests.isEmpty()) { transactions.size(transactions.size() - 1); }
	else { openSegment(openRequests.topInt()); }
	return request;
}

@Override
public void recordActivityStep(byte stepPair, int paramCount) {
	checkOpen();
	Object memory = openParams.get(openParams.size() - 1);
	int[] params;
	int available;
	if(memory instanceof int[] array) { params = array; available = array.length; }
	else if(memory instanceof IntArrayList list) { params = list.elements(); available = list.size(); }
	else { throw new IllegalArgumentException("params memory is " + className(memory) + ", not an int[] or IntArrayList"); }
	if(paramCount < 0 || paramCount > available) { throw new IndexOutOfBoundsException("paramCount " + paramCount + ", params memory holds " + available); }
	data.add(stepPair);
	data.add(paramCount);
	data.addElements(data.size(), params, 0, paramCount);
	if(stepPair == 0) { horizon = transactionCount(); }
}

@Override
public int recordBoolean(boolean value) {
	checkOpen();
	booleans.add(value);
	return booleans.size() - 1;
}

@Override
public int recordObject(Object value) {
	checkOpen();
	objects.add(value);
	return objects.size() - 1;
}

@Override
public boolean getBoolean(int index) { return booleans.getBoolean(index); }

@Override
public Object getObject(int index) { return objects.get(index); }


@Override
public int recordQueryRequest(String description) {
	int query = queries.size() / 2;
	queries.add(description);
	queries.add(null);
	openQueries.push(query);
	return query;
}

@Override
public int recordQueryResponse(String description) {
	if(openQueries.isEmpty()) { throw new IllegalStateException("no query is open"); }
	int query = openQueries.popInt();
	queries.set(2 * query + 1, description);
	return query;
}


@Override
public void undoLastActivity() {
	checkIdle();
	if(done == 0) { throw new IllegalStateException("nothing to undo"); }
	moveTo(done - 1);
}

@Override
public void redoLastActivity() {
	checkIdle();
	if(done == transactionCount()) { throw new IllegalStateException("nothing to redo"); }
	moveTo(done + 1);
}

/** Leaves the state right after the transaction holding {@code activityIndex}. */
@Override
public void undoUntilActivityIndex(int activityIndex) {
	checkIdle();
	int target = transactionOf(activityIndex) + 1;
	if(target > done) { throw new IllegalStateException("request " + activityIndex + " is undone; redo to reach it"); }
	moveTo(target);
}

/** Leaves the state right after the transaction holding {@code activityIndex}. */
@Override
public void redoUntilActivityIndex(int activityIndex) {
	checkIdle();
	int target = transactionOf(activityIndex) + 1;
	if(target < done) { throw new IllegalStateException("request " + activityIndex + " is applied; undo to reach it"); }
	moveTo(target);
}

/** Leaves the state right after the transaction holding {@code activityIndex}, undoing or redoing as needed. */
@Override
public void checkout(int activityIndex) {
	checkIdle();
	moveTo(transactionOf(activityIndex) + 1);
}

void moveTo(int target) {
	if(target < horizon) { throw new IllegalStateException("undo history is broken before transaction " + horizon); }
	replaying = true;
	try {
		while(done > target) { replayTransaction(done - 1, true); done--; }
		while(done < target) { replayTransaction(done, false); done++; }
	} finally { replaying = false; }
}

/** Segments last-first on undo; the steps inside a segment run forward, since their order does not matter. */
void replayTransaction(int transaction, boolean undo) {
	int first = firstSegmentOf(transaction);
	int end = endSegmentOf(transaction);
	if(undo) { for(int segment = end - 1; segment >= first; segment--) { replaySegment(segment, true); } }
	else { for(int segment = first; segment < end; segment++) { replaySegment(segment, false); } }
}

void replaySegment(int segment, boolean undo) {
	TraceableForUndoingRedoing actor = actorOf(field(segments.getInt(2 * segment + 1), ACTOR));
	int at = segments.getInt(2 * segment);
	int end = segmentEnd(segment);
	int[] elements = data.elements();
	while(at < end) {
		byte pair = (byte) elements[at];
		actor.doTraceableStep(undo ? (byte) -pair : pair, elements, at + 2);
		at += 2 + elements[at + 1];
	}
}


/**
 * JSON Lines: a line naming the components by slot, then one line per transaction holding its
 * requests, their steps and its recorded values, then one line per query.
 * */
@Override
public void storeAll(DataOutputStream dos) throws IOException {
	StringBuilder line = new StringBuilder("{\"arities\":[");
	for(int i = 0; i < core.getArityCount(); i++) { comma(line, i); string(line, className(core.getArity(i))); }
	line.append("],\"structures\":[");
	for(int i = 0; i < core.getStructureCount(); i++) { comma(line, i); string(line, className(core.getStructure(i))); }
	line.append("]}");
	writeLine(dos, line);
	for(int transaction = 0; transaction < transactionCount(); transaction++) {
		appendTransaction(line, transaction);
		writeLine(dos, line);
	}
	for(int query = 0; query < queries.size() / 2; query++) {
		line.append("{\"query\":").append(query).append(",\"request\":");
		string(line, queries.get(2 * query));
		line.append(",\"response\":");
		string(line, queries.get(2 * query + 1));
		line.append('}');
		writeLine(dos, line);
	}
}

void appendTransaction(StringBuilder line, int transaction) {
	String state = transaction < done ? "applied" : !openRequests.isEmpty() && transaction == transactionCount() - 1 ? "open" : "undone";
	line.append("{\"transaction\":").append(transaction).append(",\"state\":\"").append(state)
		.append("\",\"undoable\":").append(transaction >= horizon).append(",\"requests\":[");
	int firstRequest = transactions.getInt(transaction);
	int endRequest = transaction + 1 < transactionCount() ? transactions.getInt(transaction + 1) : requestCount();
	for(int request = firstRequest; request < endRequest; request++) {
		comma(line, request - firstRequest);
		line.append("{\"request\":").append(request).append(",\"parent\":").append(field(request, PARENT))
			.append(",\"actor\":").append(field(request, ACTOR)).append(",\"method\":");
		string(line, texts.get(2 * request));
		line.append(",\"comment\":");
		string(line, texts.get(2 * request + 1));
		line.append(",\"steps\":[");
		boolean first = true;
		for(int segment = firstSegmentOf(transaction); segment < endSegmentOf(transaction); segment++) {
			if(segments.getInt(2 * segment + 1) != request) { continue; }
			for(int at = segments.getInt(2 * segment), end = segmentEnd(segment); at < end; at += 2 + data.getInt(at + 1)) {
				if(!first) { line.append(','); }
				first = false;
				line.append("{\"pair\":").append(data.getInt(at)).append(",\"params\":[");
				for(int i = 0; i < data.getInt(at + 1); i++) { comma(line, i); line.append(data.getInt(at + 2 + i)); }
				line.append("]}");
			}
		}
		line.append("]}");
	}
	int booleansStart = field(firstRequest, BOOLEANS);
	int booleansEnd = endRequest < requestCount() ? field(endRequest, BOOLEANS) : booleans.size();
	line.append("],\"booleans\":[");
	for(int i = booleansStart; i < booleansEnd; i++) { comma(line, i - booleansStart); line.append(booleans.getBoolean(i)); }
	int objectsStart = field(firstRequest, OBJECTS);
	int objectsEnd = endRequest < requestCount() ? field(endRequest, OBJECTS) : objects.size();
	line.append("],\"objects\":[");
	for(int i = objectsStart; i < objectsEnd; i++) { comma(line, i - objectsStart); string(line, className(objects.get(i))); }
	line.append("]}");
}

static void writeLine(DataOutputStream dos, StringBuilder line) throws IOException {
	line.append('\n');
	dos.write(line.toString().getBytes(StandardCharsets.UTF_8));
	line.setLength(0);
}

static void comma(StringBuilder line, int index) { if(index > 0) { line.append(','); } }

static void string(StringBuilder line, String string) {
	if(string == null) { line.append("null"); return; }
	line.append('"');
	for(int i = 0; i < string.length(); i++) {
		char c = string.charAt(i);
		switch(c) {
			case '"' -> line.append("\\\"");
			case '\\' -> line.append("\\\\");
			case '\n' -> line.append("\\n");
			case '\r' -> line.append("\\r");
			case '\t' -> line.append("\\t");
			default -> {
				if(c < 0x20) { line.append(String.format("\\u%04x", (int) c)); }
				else { line.append(c); }
			}
		}
	}
	line.append('"');
}

static String className(Object object) { return object == null ? null : object.getClass().getName(); }


/** Arity k is -(k + 1), the core is 0, structure k is k + 1. */
int actorIdOf(TraceableForUndoingRedoing actor) {
	if(actor == core) { return 0; }
	for(int i = 0; i < core.getArityCount(); i++) { if(core.getArity(i) == actor) { return -i - 1; } }
	for(int i = 0; i < core.getStructureCount(); i++) { if(core.getStructure(i) == actor) { return i + 1; } }
	throw new IllegalArgumentException(className(actor) + " is not added to the core");
}

TraceableForUndoingRedoing actorOf(int actor) {
	if(actor < 0) { return core.getArity(-actor - 1); }
	if(actor > 0) { return core.getStructure(actor - 1); }
	return core;
}

/** Reuses the last segment when nothing was recorded in it. */
void openSegment(int request) {
	int count = segmentCount();
	if(count > 0 && segments.getInt(2 * count - 2) == data.size()) { segments.set(2 * count - 1, request); }
	else {
		segments.add(data.size());
		segments.add(request);
	}
}

void dropRedo() {
	if(done == transactionCount()) { return; }
	int firstRequest = transactions.getInt(done);
	int firstSegment = field(firstRequest, SEGMENT);
	data.size(segments.getInt(2 * firstSegment));
	booleans.size(field(firstRequest, BOOLEANS));
	objects.subList(field(firstRequest, OBJECTS), objects.size()).clear();
	segments.size(2 * firstSegment);
	requests.size(FIELDS * firstRequest);
	texts.subList(2 * firstRequest, texts.size()).clear();
	transactions.size(done);
}

int transactionOf(int request) {
	if(request < 0 || request >= requestCount()) { throw new IndexOutOfBoundsException("request " + request + ", count " + requestCount()); }
	return field(request, TRANSACTION);
}

int field(int request, int field) { return requests.getInt(FIELDS * request + field); }
int firstSegmentOf(int transaction) { return field(transactions.getInt(transaction), SEGMENT); }
int endSegmentOf(int transaction) { return transaction + 1 < transactionCount() ? firstSegmentOf(transaction + 1) : segmentCount(); }
int segmentEnd(int segment) { return segment + 1 < segmentCount() ? segments.getInt(2 * segment + 2) : data.size(); }

int requestCount() { return requests.size() / FIELDS; }
int segmentCount() { return segments.size() / 2; }
int transactionCount() { return transactions.size(); }

void checkNotReplaying() {
	if(replaying) { throw new IllegalStateException("a step function recorded during undo or redo"); }
}

void checkOpen() {
	checkNotReplaying();
	if(openRequests.isEmpty()) { throw new IllegalStateException("no request is open"); }
}

void checkIdle() {
	checkNotReplaying();
	if(!openRequests.isEmpty()) { throw new IllegalStateException("request " + openRequests.topInt() + " is still open"); }
}

}
