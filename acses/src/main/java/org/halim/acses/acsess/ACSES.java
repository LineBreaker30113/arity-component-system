package org.halim.acses.acsess;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/**
 * The program a terminal runs, {@code acses <command> [arguments] [comment="<text>"]}: it sends the
 * arguments as one request and prints the answer, a done one on the standard output with exit status 0,
 * a refused one on the standard error with 1. Only {@code boot} starts anything. With no daemon
 * answering, it launches one as a separate process, so this command line stays free, detached from the
 * terminal, so closing the terminal leaves it running. Every other command fails at once when nothing
 * answers, so a mistyped request never leaves a daemon behind.
 * <p>
 * {@code beginSession} runs here alone: it reads requests line by line and sends nothing until a line
 * {@code Commit [+continueOnRefusal]}, which sends them all as one request, so the daemon runs them
 * back to back.
 * */
public class ACSES {

/** By name, not by import: this side knows the daemon only as a class to launch. */
static final String DAEMON = "org.halim.acses.daemon.ACSES_Daemon";
/** How long boot waits for a started daemon's socket to answer. */
static final long WAIT_NANOS = 10_000_000_000L;


public static void main(String[] args) {
	if(args.length == 0) { exit(ACSES_Request.REFUSED, "usage: acses <command> [arguments] [comment=\"<text>\"]"); }
	Path socket = ACSES_Request.socket();
	List<String> request = List.of(args);
	ACSES_Request.Answer answer;
	try { answer = args[0].equals("beginSession") ? session(socket, request) : ACSES_Request.send(socket, request); }
	catch(IOException e) { answer = new ACSES_Request.Answer(ACSES_Request.REFUSED, "the daemon on " + socket + " broke off: " + e.getMessage()); }
	if(answer == null && args[0].equals("boot")) {
		try { answer = boot(socket, request); }
		catch(IOException e) { answer = new ACSES_Request.Answer(ACSES_Request.REFUSED, "cannot start the daemon: " + e.getMessage()); }
	}
	if(answer == null) { answer = new ACSES_Request.Answer(ACSES_Request.REFUSED, "no daemon answers on " + socket + "; run acses boot"); }
	exit(answer.status, answer.text);
}

/**
 * Launches the daemon - the same java, the same class path - under setsid, or nohup where there is
 * none, its input from /dev/null and its output appended to the log, then sends it this request once
 * its socket answers. The daemon's own answer names its pid: a boot racing this one may have won the
 * lock, and then the daemon that answers is not the process started here.
 * */
static ACSES_Request.Answer boot(Path socket, List<String> request) throws IOException {
	ACSES_Request.makeFolder(socket);
	Path log = ACSES_Request.log(socket);
	ProcessBuilder builder = new ProcessBuilder(command());
	builder.redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null")));
	builder.redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile()));
	builder.redirectErrorStream(true);
	Process daemon = builder.start();
	long until = System.nanoTime() + WAIT_NANOS;
	boolean alive = true;
	while(alive && System.nanoTime() - until < 0) {
		// Read before asking: a daemon that lost the lock to a racing boot exits, and the winner is still asked once more.
		alive = daemon.isAlive();
		ACSES_Request.Answer answer = ACSES_Request.send(socket, request);
		if(answer != null) { return answer; }
		pause();
	}
	return new ACSES_Request.Answer(ACSES_Request.REFUSED, (alive ? "no answer on " + socket + " within 10 s" : "the daemon exited with status " + daemon.exitValue()) + "; see " + log);
}

/**
 * Queues one request per line of the standard input, split as {@link #split} says, with a prompt when
 * it is a terminal; a line opening a multi-line quote goes on over the lines until it closes; a line that cannot be split, or a request that cannot be queued, is told on the
 * standard error and the session goes on. A Commit line sends the queue as one request: "Commit", the
 * flag when given, then each request as its argument count and its arguments. The end of the input
 * without a Commit sends nothing.
 * */
static ACSES_Request.Answer session(Path socket, List<String> request) throws IOException {
	if(request.size() > 1) { return new ACSES_Request.Answer(ACSES_Request.REFUSED, "usage: beginSession"); }
	BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
	boolean prompting = System.console() != null;
	ArrayList<String> batch = new ArrayList<>(List.of("Commit"));
	while(true) {
		if(prompting) {
			System.out.print("acses> ");
			System.out.flush();
		}
		String text = in.readLine();
		if(text == null) { return new ACSES_Request.Answer(ACSES_Request.REFUSED, "the session ended without a Commit; nothing was sent"); }
		List<String> arguments;
		try {
			while((arguments = split(text)) == null) {
				if(prompting) {
					System.out.print("...> ");
					System.out.flush();
				}
				String more = in.readLine();
				if(more == null) { return new ACSES_Request.Answer(ACSES_Request.REFUSED, "the session ended inside a multi-line quote; nothing was sent"); }
				text += "\n" + more;
			}
		} catch(IllegalArgumentException e) {
			System.err.println(e.getMessage() + "; the line is not queued");
			continue;
		}
		if(arguments.isEmpty()) { continue; }
		String command = arguments.get(0);
		if(command.equals("beginSession")) { System.err.println("a session is open already; the line is not queued"); continue; }
		if(command.equals("Commit")) {
			if(arguments.equals(List.of("Commit", "+continueOnRefusal"))) { batch.add(1, "+continueOnRefusal"); }
			else if(arguments.size() > 1) { System.err.println("usage: Commit [+continueOnRefusal]; the session goes on"); continue; }
			ACSES_Request.Answer answer = ACSES_Request.send(socket, batch);
			return answer != null ? answer : new ACSES_Request.Answer(ACSES_Request.REFUSED, "no daemon answers on " + socket + "; run acses boot; nothing was run");
		}
		batch.add(Integer.toString(arguments.size()));
		batch.addAll(arguments);
	}
}

/**
 * Splits a request as a shell would, without its expansions, or gives back null while a multi-line
 * quote is open, for the caller to add the next line. Blanks and line breaks separate arguments, and a
 * quote may sit inside an argument, as in {@code comment="a b"}. {@code ```...```} and
 * {@code """..."""} span lines and keep everything inside, every line break included; single
 * quotes and backquotes keep everything inside, on one line; double quotes the same, but for a
 * backslash before a double quote or a backslash; outside quotes a backslash keeps the next character.
 * A tilde is not expanded, so a path must be written absolute.
 * */
static List<String> split(String text) {
	ArrayList<String> arguments = new ArrayList<>();
	StringBuilder current = new StringBuilder();
	boolean inArgument = false;
	for(int i = 0; i < text.length(); i++) {
		char c = text.charAt(i);
		String fence = text.startsWith("```", i) ? "```" : text.startsWith("\"\"\"", i) ? "\"\"\"" : null;
		if(fence != null) {
			int end = text.indexOf(fence, i + 3);
			if(end < 0) { return null; }
			current.append(text, i + 3, end);
			i = end + 2;
			inArgument = true;
		} else if(c == '\'' || c == '`') {
			int end = text.indexOf(c, i + 1);
			if(end < 0 || text.substring(i + 1, end).indexOf('\n') >= 0) { throw new IllegalArgumentException("a " + (c == '`' ? "backquote" : "single quote") + " is not closed on its line"); }
			current.append(text, i + 1, end);
			i = end;
			inArgument = true;
		} else if(c == '"') {
			for(i++; i < text.length() && text.charAt(i) != '"' && text.charAt(i) != '\n'; i++) {
				if(text.charAt(i) == '\\' && i + 1 < text.length() && (text.charAt(i + 1) == '"' || text.charAt(i + 1) == '\\')) { i++; }
				current.append(text.charAt(i));
			}
			if(i == text.length() || text.charAt(i) == '\n') { throw new IllegalArgumentException("a double quote is not closed on its line"); }
			inArgument = true;
		} else if(c == '\\') {
			if(++i == text.length() || text.charAt(i) == '\n') { throw new IllegalArgumentException("a backslash ends the line"); }
			current.append(text.charAt(i));
			inArgument = true;
		} else if(Character.isWhitespace(c)) {
			if(inArgument) { arguments.add(current.toString()); }
			current.setLength(0);
			inArgument = false;
		} else {
			current.append(c);
			inArgument = true;
		}
	}
	if(inArgument) { arguments.add(current.toString()); }
	return arguments;
}

/** The same java and class path as this program, under setsid when the system has it, else nohup: either way the daemon outlives the terminal. */
static List<String> command() {
	String java = ProcessHandle.current().info().command().orElseGet(() -> Path.of(System.getProperty("java.home"), "bin", "java").toString());
	ArrayList<String> command = new ArrayList<>();
	command.add(onPath("setsid") ? "setsid" : "nohup");
	command.addAll(List.of(java, "-cp", System.getProperty("java.class.path"), DAEMON));
	return command;
}

static boolean onPath(String program) {
	String path = System.getenv("PATH");
	if(path == null) { return false; }
	for(String folder : path.split(File.pathSeparator)) {
		if(!folder.isEmpty() && Files.isExecutable(Path.of(folder, program))) { return true; }
	}
	return false;
}

static void pause() {
	try { Thread.sleep(50); }
	catch(InterruptedException e) { Thread.currentThread().interrupt(); }
}

/** A done text on the standard output, anything else on the standard error, and the exit status to match. */
static void exit(int status, String text) {
	(status == ACSES_Request.DONE ? System.out : System.err).println(text);
	System.exit(status == ACSES_Request.DONE ? 0 : 1);
}

}
