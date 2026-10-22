package org.halim.nStructure.mhexagon;

import org.halim.legacy.dlake.DataLakeManager;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Refactored, unified, and thread-safe Crash Reporting Engine.
 */
public class CrashReporter {

public static final int MAX_BREADCRUMBS = 1000;
private static final List<DiagnosticStateProvider> stateProviders = new ArrayList<>();
private static final CircularFifoQueue<String> globalBreadcrumbs = new CircularFifoQueue<>(MAX_BREADCRUMBS);
private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

/**
 * Universal OS-agnostic path resolver targeting the user's physical Documents directory.
 */
public static Path getUniversalBugReportsDir() {
	String userHome = System.getProperty("user.home");
	Path documentsPath = Paths.get(userHome, "Documents");
	
	// Windows specific check: If OneDrive hijacked the user space, resolve to the live synced folder
	String os = System.getProperty("os.name").toLowerCase();
	if (os.contains("win")) {
		Path oneDriveDocs = Paths.get(userHome, "OneDrive", "Documents");
		if (Files.exists(oneDriveDocs)) {
			documentsPath = oneDriveDocs;
		}
	}
	
	Path finalBugDir = documentsPath.resolve("ontoDirectory").resolve("bugReports");
	try {
		Files.createDirectories(finalBugDir);
		return finalBugDir;
	} catch (Exception e) {
		// Absolute emergency fallback to execution context if OS permissions block the Documents track
		Path fallback = Paths.get(userHome, ".config", "onto-directory", "bugReports");
		try { Files.createDirectories(fallback); } catch (Exception ignored) {}
		return fallback;
	}
}

// --- REGISTRATION & LOGGING API ---

public static synchronized void registerProvider(DiagnosticStateProvider provider) {
	if (provider != null) {
		stateProviders.add(provider);
	}
}

public static void log(String message) {
	globalBreadcrumbs.add(System.currentTimeMillis() + " | " + message);
}

// --- MAIN REPORTING ENTRY POINTS ---

/**
 * Generates a deeply contextualized dump for a specific Data Lake.
 */
public static Path generateCrashDump(Throwable ex, DataLakeManager activeLake) {
	if (activeLake == null) {
		System.err.println("CRITICAL: Crash occurred, but no Set Sea was active. Redirecting to global dump.");
		return generateGlobalDump(ex, Paths.get(System.getProperty("user.dir")));
	}
	
	try {
		Path bugDir = getUniversalBugReportsDir();
		Path dumpFile = initializeDumpFile(bugDir, "crash_dump_");
		
		try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(dumpFile))) {
			writeHeader(writer, "ONTO DIRECTORY CRASH DUMP");
			writeSystemMetrics(writer);
			writer.println("Active Sea   : [REDACTED FOR PRIVACY]");
			writer.println("=========================================\n");
			
			writer.println("--- BREADCRUMB TRAIL (LAST " + MAX_BREADCRUMBS + " ACTIONS) ---");
			for (String bc : activeLake.getBreadcrumbs()) {
				writer.println(bc);
			}
			
			if (activeLake.ontologyHierarchy != null) writer.println("\n" + activeLake.ontologyHierarchy.generatePrivacyMaskedDagDump());
			
			writeArchitecturalStateAndGlobalBreadcrumbs(writer);
			writeStackTrace(writer, ex);
		}
		
		System.err.println("\nFATAL ERROR: A critical failure occurred.");
		System.err.println("A privacy-masked crash dump has been generated at: " + dumpFile.toAbsolutePath());
		return dumpFile;
		
	} catch (Exception e) {
		return handleEmergencyFallback(ex, e, "domain");
	}
}

/**
 * Generates a generic JVM dump when no Data Lake is active (e.g., GUI boot failures).
 */
public static Path generateGlobalDump(Throwable ex, Path fallbackConfigDir) {
	try {
		Path bugDir = getUniversalBugReportsDir();;
		Path dumpFile = initializeDumpFile(bugDir, "crashD");
		
		try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(dumpFile))) {
			writeHeader(writer, "ONTO DIRECTORY GLOBAL CRASH DUMP");
			writeSystemMetrics(writer);
			writer.println("State        : NO ACTIVE SET SEA");
			writer.println("=========================================\n");
			
			writeArchitecturalStateAndGlobalBreadcrumbs(writer);
			writeStackTrace(writer, ex);
		}
		
		System.err.println("\nFATAL GLOBAL ERROR: A critical system failure occurred.");
		System.err.println("A global crash dump has been generated at: " + dumpFile.toAbsolutePath());
		return dumpFile;
		
	} catch (Exception e) {
		return handleEmergencyFallback(ex, e, "global");
	}
}

// --- REFACTORED UTILITY METHODS (DRY PRINCIPLE) ---

private static Path initializeDumpFile(Path directory, String prefix) throws Exception {
	if (!Files.exists(directory)) {
		Files.createDirectories(directory);
	}
	String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMATTER);
	return directory.resolve(prefix + timestamp + ".txt");
}

private static void writeHeader(PrintWriter writer, String title) {
	writer.println("=========================================");
	writer.printf("   %-34s  \n", title);
	writer.println("=========================================");
	writer.println("Timestamp    : " + LocalDateTime.now());
}

private static void writeSystemMetrics(PrintWriter writer) {
	Runtime rt = Runtime.getRuntime();
	writer.println("Total Memory : " + (rt.totalMemory() / 1024 / 1024) + " MB");
	writer.println("Free Memory  : " + (rt.freeMemory() / 1024 / 1024) + " MB");
}

private static synchronized void writeArchitecturalStateAndGlobalBreadcrumbs(PrintWriter writer) {
	// From commented out CrashDirector: Poll architectural matrix
	if (!stateProviders.isEmpty()) {
		writer.println("\n--- ARCHITECTURAL STATE MATRIX ---");
		for (DiagnosticStateProvider provider : stateProviders) {
			writer.println("\n[" + provider.getLayerName() + " STATE]");
			writer.println(provider.captureStateDump());
		}
	}
	
	// From commented out CrashDirector: Dump system-wide global breadcrumbs
	if (!globalBreadcrumbs.isEmpty()) {
		writer.println("\n--- GLOBAL SYSTEM BREADCRUMBS ---");
		for (String bc : globalBreadcrumbs) {
			writer.println(bc);
		}
	}
}

private static void writeStackTrace(PrintWriter writer, Throwable ex) {
	writer.println("\n--- EXCEPTION STACK TRACE ---");
	if (ex != null) {
		ex.printStackTrace(writer);
	} else {
		writer.println("No exception context provided.");
	}
}

/**
 * Ultimate defense mechanism: If target directories are read-only or full,
 * this drops the crash dump directly into the OS temporary directory.
 */
private static Path handleEmergencyFallback(Throwable originalEx, Exception engineEx, String type) {
	System.err.println("CRITICAL: CrashReporter failed to write the primary " + type + " dump due to: " + engineEx.getMessage());
	try {
		Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
		Path emergencyFile = tempDir.resolve("EMERGENCY_" + type + "_dump_" + System.currentTimeMillis() + ".txt");
		
		try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(emergencyFile))) {
			writer.println("!!! EMERGENCY CRASH REPORTER FALLBACK !!!");
			writer.println("Primary logger crashed because: " + engineEx.getMessage());
			writeStackTrace(writer, originalEx);
		}
		System.err.println("EMERGENCY: Crash dump safely salvaged at: " + emergencyFile.toAbsolutePath());
		return emergencyFile;
	} catch (Exception criticalError) {
		System.err.println("CRITICAL FAILURE: Crash logs could not be salvaged to storage disk.");
		if (originalEx != null) originalEx.printStackTrace();
		return null;
	}
}
}