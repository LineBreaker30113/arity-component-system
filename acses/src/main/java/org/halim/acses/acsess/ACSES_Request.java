package org.halim.acses.acsess;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;


/**
 * The one format both sides speak, and the place where they meet. A connection carries one request and
 * its answer: the request is the argument count, then each argument; the answer is a status,
 * {@link #DONE} or {@link #REFUSED}, then one text. A string is its UTF-8 byte length as an int, then the
 * bytes, and an int is four bytes, high first, as {@link DataOutputStream} writes it. Nothing ends a
 * request but its own count, so a side that reads less or more than the count says is talking to a
 * broken peer, not to a short message.
 * <p>
 * The socket is {@code $ACSES_SOCKET} when it is set, for checks; else {@code acses/acses.sock} under
 * {@code $XDG_RUNTIME_DIR}; else {@code acses-<user>/acses.sock} under the JDK's temporary folder. The
 * log and the lock sit beside it, and its folder is made open to its owner only, which is all the access
 * control there is for now.
 * <p>
 * This side imports only the JDK, so it can move into a program of its own unchanged.
 * */
public class ACSES_Request {

public static final int DONE = 0, REFUSED = 1;
public static final String LOG = "acses.log", LOCK = "acses.lock";


/**
 * Where the daemon listens, read from the environment the same way on both sides; boot hands its
 * environment to the daemon it starts. A relative {@code $XDG_RUNTIME_DIR} is ignored, as its
 * specification says.
 * */
public static Path socket() {
	String override = System.getenv("ACSES_SOCKET");
	if(override != null && !override.isEmpty()) { return Path.of(override).toAbsolutePath(); }
	String runtime = System.getenv("XDG_RUNTIME_DIR");
	if(runtime != null && Path.of(runtime).isAbsolute()) { return Path.of(runtime, "acses", "acses.sock"); }
	return Path.of(System.getProperty("java.io.tmpdir"), "acses-" + System.getProperty("user.name"), "acses.sock");
}

public static Path log(Path socket) { return socket.resolveSibling(LOG); }

public static Path lock(Path socket) { return socket.resolveSibling(LOCK); }

/** Makes the socket's folder, open to its owner only, when it is not there yet; a folder already there is taken as it is. */
public static void makeFolder(Path socket) throws IOException {
	Path folder = socket.getParent();
	if(!Files.isDirectory(folder)) { Files.createDirectories(folder, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))); }
}


/** Sends one request and gives back its answer, or null when nothing answers on the socket. A daemon that answers and then breaks off is an IOException, not a null: it was there. */
public static Answer send(Path socket, List<String> arguments) throws IOException {
	SocketChannel channel;
	try { channel = SocketChannel.open(UnixDomainSocketAddress.of(socket)); }
	catch(IOException e) { return null; }
	try(channel) {
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Channels.newOutputStream(channel)));
		writeRequest(out, arguments);
		out.flush();
		return readAnswer(new DataInputStream(new BufferedInputStream(Channels.newInputStream(channel))));
	}
}

public static void writeRequest(DataOutputStream out, List<String> arguments) throws IOException {
	out.writeInt(arguments.size());
	for(String argument : arguments) { writeString(out, argument); }
}

/** Refuses a negative count; a count larger than what follows ends in an EOFException, so a garbled request costs nothing but itself. */
public static List<String> readRequest(DataInputStream in) throws IOException {
	int count = in.readInt();
	if(count < 0) { throw new IOException("argument count " + count); }
	ArrayList<String> arguments = new ArrayList<>();
	for(int i = 0; i < count; i++) { arguments.add(readString(in)); }
	return arguments;
}

public static void writeAnswer(DataOutputStream out, Answer answer) throws IOException {
	out.writeInt(answer.status);
	writeString(out, answer.text);
}

public static Answer readAnswer(DataInputStream in) throws IOException {
	int status = in.readInt();
	return new Answer(status, readString(in));
}

public static void writeString(DataOutputStream out, String string) throws IOException {
	byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
	out.writeInt(bytes.length);
	out.write(bytes);
}

/** Takes the bytes as they arrive rather than allocating the length first, so a garbled length ends in an EOFException, not in the whole heap. */
public static String readString(DataInputStream in) throws IOException {
	int length = in.readInt();
	if(length < 0) { throw new IOException("string length " + length); }
	byte[] bytes = in.readNBytes(length);
	if(bytes.length < length) { throw new EOFException("a string of " + length + " bytes ends after " + bytes.length); }
	return new String(bytes, StandardCharsets.UTF_8);
}


/** A status and its text. */
public static final class Answer {
	public final int status;
	public final String text;

	public Answer(int status, String text) {
		this.status = status;
		this.text = text;
	}
}

}
