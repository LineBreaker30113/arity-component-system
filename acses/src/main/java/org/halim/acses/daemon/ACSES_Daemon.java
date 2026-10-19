package org.halim.acses.daemon;

import org.halim.acses.acsess.ACSES_Request;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;


/**
 * The process that holds the databases. It serves one request at a time on one thread, which makes it
 * the single writer: no core is ever reached by two requests at once. Each request is logged on the
 * standard output - its time, its arguments with the comment among them, and its outcome - and boot
 * points the standard output at the log. A waitInspectionReturn during an inspection is held open
 * without a thread and answered after a later request; a Commit runs its requests back to back.
 * <p>
 * The lock beside the socket is held for the daemon's whole life, and only its holder removes a socket
 * file nothing answers on and binds a new one, so two boots racing cannot take the socket from each
 * other. The lock file itself is never deleted: a daemon deleting it on the way out could let one
 * starting daemon lock the old file while another locks a new one.
 * */
public class ACSES_Daemon {

public static void main(String[] args) {
	try { serve(ACSES_Request.socket()); }
	catch(IOException | RuntimeException e) {
		log(OffsetDateTime.now(), "stopped: " + ACSES_Commands.reason(e));
		System.exit(1);
	}
	log(OffsetDateTime.now(), "stopped");
}

/** Takes the lock, replaces a socket file nothing answers on, and serves until a shutdown; the socket file goes with the daemon, the lock file stays. */
static void serve(Path socket) throws IOException {
	ACSES_Request.makeFolder(socket);
	Path lockFile = ACSES_Request.lock(socket);
	try(FileChannel lockChannel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
		FileLock lock = lockChannel.tryLock();
		if(lock == null) { throw new IOException("another daemon holds " + lockFile); }
		if(answers(socket)) { throw new IOException("a daemon answers on " + socket + " without holding " + lockFile); }
		Files.deleteIfExists(socket);
		ACSES_Commands commands = new ACSES_Commands(socket);
		try(ServerSocketChannel server = ServerSocketChannel.open(StandardProtocolFamily.UNIX)) {
			server.bind(UnixDomainSocketAddress.of(socket));
			log(OffsetDateTime.now(), "listening on " + socket + ", pid " + ProcessHandle.current().pid());
			while(!commands.stopping) {
				SocketChannel client = server.accept();
				if(!answer(client, commands)) { client.close(); }
				answerHeld(commands);
			}
		} finally { Files.deleteIfExists(socket); }
	}
}

/** Whether something answers on the socket; the probe closes at once, which the other end logs as an unreadable request. */
static boolean answers(Path socket) {
	try(SocketChannel probe = SocketChannel.open(UnixDomainSocketAddress.of(socket))) { return true; }
	catch(IOException e) { return false; }
}

/**
 * Reads one request, runs it and answers; a request that cannot be read, or an answer that cannot be
 * sent, is logged and dropped, so a broken peer never stops the daemon. Gives back whether a wait holds
 * the client, which then stays open until {@link #answerHeld} answers it.
 * */
static boolean answer(SocketChannel client, ACSES_Commands commands) {
	OffsetDateTime time = OffsetDateTime.now();
	List<String> arguments;
	try { arguments = ACSES_Request.readRequest(new DataInputStream(new BufferedInputStream(Channels.newInputStream(client)))); }
	catch(IOException e) {
		log(time, "unreadable request: " + ACSES_Commands.reason(e));
		return false;
	}
	ACSES_Request.Answer answer = commands.run(arguments);
	if(answer == null) {
		commands.hold(new ACSES_Commands.Held(client, time, arguments));
		log(time, arguments.stream().map(ACSES_Daemon::quoted).collect(Collectors.joining(" ")) + " -> held");
		return true;
	}
	send(client, time, arguments, answer);
	return false;
}

/** Sends the answers that came ready for held clients, each logged under the time of its request, and closes them. */
static void answerHeld(ACSES_Commands commands) throws IOException {
	for(ACSES_Commands.Held held : commands.ready) {
		try(held.client) { send(held.client, held.time, held.arguments, held.answer); }
	}
	commands.ready.clear();
}

static void send(SocketChannel client, OffsetDateTime time, List<String> arguments, ACSES_Request.Answer answer) {
	String sent = "";
	try {
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Channels.newOutputStream(client)));
		ACSES_Request.writeAnswer(out, answer);
		out.flush();
	} catch(IOException e) { sent = " (not sent: " + ACSES_Commands.reason(e) + ")"; }
	log(time, arguments.stream().map(ACSES_Daemon::quoted).collect(Collectors.joining(" ")) + " -> " + outcome(answer) + sent);
}

/** The status as a word and the text's first line, with a count of the lines left out, so a request stays one line of the log. */
static String outcome(ACSES_Request.Answer answer) {
	String[] lines = answer.text.split("\n", -1);
	int more = lines.length - 1;
	return (answer.status == ACSES_Request.DONE ? "done: " : "refused: ") + lines[0] + (more == 0 ? "" : more == 1 ? " (+1 line)" : " (+" + more + " lines)");
}

/** An argument as it would be typed: quoted when it is empty or holds a space, a quote or a backslash, with line breaks written as \n. */
static String quoted(String argument) {
	if(!argument.isEmpty() && argument.chars().noneMatch(c -> Character.isWhitespace(c) || c == '"' || c == '\\')) { return argument; }
	return '"' + argument.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + '"';
}

static void log(OffsetDateTime time, String line) { System.out.println(time + " " + line); }

}
