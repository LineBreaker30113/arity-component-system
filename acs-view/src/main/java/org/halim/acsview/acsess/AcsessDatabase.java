package org.halim.acsview.acsess;

import org.halim.acsview.mhexagon.ACSViewService;
import org.halim.acsview.mhexagon.ACSViewService.Arity;
import org.halim.acsview.mhexagon.ACSViewService.Components;
import org.halim.acsview.mhexagon.ACSViewService.Refusal;
import org.halim.acsview.mhexagon.ACSViewService.Row;
import org.halim.acsview.mhexagon.ACSViewService.Structure;
import org.halim.acsview.mhexagon.ACSViewService.ValueType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/**
 * One database acsed holds, addressed as acsess addresses it: {@code <name>:core}, {@code <name>:journal},
 * {@code <name>:arity-<k>} and {@code <name>:arity-<k>.row-<j>}, and its name after a database command.
 * Every parameter goes by name, {@code identity=5}, so a value that looks like a flag stays a value; a
 * boolean goes as {@code +value} or not at all, since acsess takes a boolean no other way.
 * */
public class AcsessDatabase implements ACSViewService.DatabaseService {

public static final String COMMENT = "acs-view";

final Acsess acsess;
final String name;
final Path folder;

AcsessDatabase(Acsess acsess, String name, Path folder) {
	this.acsess = acsess;
	this.name = name;
	this.folder = folder;
}

@Override public String getName() { return name; }

@Override public Path getFolder() { return folder; }

String address(String actor) { return name + ":" + actor; }

static String arity(int arity) { return "arity-" + arity; }

static String row(int arity, int row) { return "arity-" + arity + ".row-" + row; }

static String comment() { return "comment=" + COMMENT; }

/** A string parameter by name; none for "", since acsess refuses {@code name=} with nothing after it and takes a string not given as "". */
static List<String> named(String name, String value) { return value.isEmpty() ? List.of() : List.of(name + "=" + value); }

/**
 * Read from the tab-separated lines {@code components} prints, "-" for a field with nothing in it:
 * {@code arity k}, its class, description and identity count; under it a line per row,
 * {@code catalog j}, its kind and description; then {@code structure k}, its class, description and
 * the arities it spans. A removed slot prints {@code (removed)} in place of its class.
 * */
@Override
public Components getComponents() {
	String printed = acsess.command("components", name);
	ArrayList<Arity> arities = new ArrayList<>();
	ArrayList<Structure> structures = new ArrayList<>();
	if(printed.equals("no components")) { return new Components(arities, structures); }
	int slot = -1;
	String className = null, description = null;
	long identities = 0;
	ArrayList<Row> rows = null;
	for(String line : printed.split("\n")) {
		String[] fields = line.split("\t", -1);
		if(line.startsWith("arity ")) {
			if(rows != null) { arities.add(new Arity(slot, className, description, identities, rows)); }
			slot = number(fields[0].substring(6), line);
			boolean removed = fields[1].equals("(removed)");
			className = removed ? null : fields[1];
			description = removed || fields.length < 3 ? null : field(fields[2]);
			identities = removed || fields.length < 4 ? 0 : Long.parseLong(fields[3].substring(0, fields[3].indexOf(' ')));
			rows = new ArrayList<>();
		} else if(line.startsWith("\tcatalog ")) {
			if(rows == null) { throw new Refusal("components printed a row before any arity: " + line); }
			boolean removed = fields[2].equals("(removed)");
			rows.add(new Row(number(fields[1].substring(8), line), removed ? null : fields[2], removed || fields.length < 4 ? null : field(fields[3])));
		} else if(line.startsWith("structure ")) {
			boolean removed = fields[1].equals("(removed)");
			int[] spanned = new int[0];
			if(!removed && fields.length >= 4 && fields[3].startsWith("arities")) {
				String[] parts = fields[3].substring(7).trim().split(" +");
				spanned = parts[0].isEmpty() ? new int[0] : java.util.Arrays.stream(parts).mapToInt(Integer::parseInt).toArray();
			}
			structures.add(new Structure(number(fields[0].substring(10), line), removed ? null : fields[1], removed || fields.length < 3 ? null : field(fields[2]), spanned));
		} else { throw new Refusal("components printed a line this program does not read: " + line); }
	}
	if(rows != null) { arities.add(new Arity(slot, className, description, identities, rows)); }
	return new Components(arities, structures);
}

static String field(String printed) { return printed.equals("-") ? null : printed; }

static int number(String digits, String line) {
	try { return Integer.parseInt(digits.trim()); }
	catch(NumberFormatException e) { throw new Refusal("components printed a slot this program does not read: " + line); }
}

@Override
public long[] getIdentities(int arity) {
	String printed = acsess.command(address(arity(arity)), "occupiedIdentities");
	if(printed.isEmpty()) { return new long[0]; }
	return printed.lines().mapToLong(Long::parseLong).toArray();
}

/** One session for the whole page, run past refusals, so a cell acsed refuses shows its reason and costs no other cell. */
@Override
public String[][] getValues(int arity, List<Row> rows, long[] identities) {
	ArrayList<List<String>> requests = new ArrayList<>();
	for(long identity : identities) {
		for(Row row : rows) {
			if(row.type().requestPart == null) { continue; }
			requests.add(List.of(address(row(arity, row.slot())), "get" + row.type().requestPart + "For", "identity=" + identity));
		}
	}
	List<ACSViewService.Answer> answers = acsess.session(requests, true);
	String[][] values = new String[identities.length][rows.size()];
	int next = 0;
	for(int i = 0; i < identities.length; i++) {
		for(int j = 0; j < rows.size(); j++) {
			if(rows.get(j).type().requestPart == null) { continue; }
			ACSViewService.Answer answer = answers.get(next++);
			values[i][j] = answer.done() ? answer.text() : "(" + answer.text() + ")";
		}
	}
	return values;
}

@Override
public void setValue(int arity, Row row, long identity, String value) {
	ValueType type = row.type();
	if(type.requestPart == null) { throw new Refusal("row " + row.slot() + " holds " + row.kind() + ", which acsess cannot write"); }
	ArrayList<String> arguments = new ArrayList<>(List.of(address(row(arity, row.slot())), "set" + type.requestPart + "For", "identity=" + identity));
	if(type == ValueType.BOOLEAN) {
		if(!value.equals("true") && !value.equals("false")) { throw new Refusal(value + " is no boolean; true or false"); }
		if(value.equals("true")) { arguments.add("+value"); }
	} else { arguments.addAll(named("value", value)); }
	arguments.add(comment());
	acsess.command(arguments);
}

@Override
public long captureIdentity(int arity) { return Long.parseLong(acsess.command(address("core"), "captureIdentity", "arity=" + arity, comment())); }

@Override
public void deleteIdentity(int arity, long identity) { acsess.command(address("core"), "delegateRemoval", "arity=" + arity, "instance=" + identity, comment()); }

@Override
public void addArity(String description) {
	ArrayList<String> arguments = new ArrayList<>(List.of(address("core"), "addArity"));
	arguments.addAll(named("description", description));
	arguments.add(comment());
	acsess.command(arguments);
}

@Override
public int addCatalogRow(int arity, String className, String description) {
	ArrayList<String> arguments = new ArrayList<>(List.of(address("core"), "addCatalogRow", "arity=" + arity, "className=" + className));
	arguments.addAll(named("description", description));
	arguments.add(comment());
	return Integer.parseInt(acsess.command(arguments));
}

@Override public void undo() { acsess.command(address("journal"), "undoLastActivity"); }

@Override public void redo() { acsess.command(address("journal"), "redoLastActivity"); }

@Override public int getCurrentActivityIndex() { return Integer.parseInt(acsess.command(address("journal"), "getCurrentActivityIndex")); }

@Override public int getPendingActivityCount() { return Integer.parseInt(acsess.command(address("journal"), "getPendingActivityCount")); }

@Override public String store() { return acsess.command("storeDB", name, comment()); }

/** Forgets the database in acsed without storing it. */
public String unload() { return acsess.command("unloadDB", name, comment()); }

@Override public String toString() { return name; }

@Override public boolean equals(Object other) { return other instanceof AcsessDatabase database && database.name.equals(name); }

@Override public int hashCode() { return name.hashCode(); }

}
