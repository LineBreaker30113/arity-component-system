package org.halim;

import org.halim.legacy.SettingLogic;
import org.halim.nStructure.archive.core.ArchiverServer;
import org.halim.nStructure.mhexagon.CrashReporter;
import org.halim.nStructure.mhexagon.OntoDirectoryService;
import org.halim.nStructure.sgui.ApplicationController;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Manual entry point for the nStructure app-shell (ontology engine + archive + mhexagon + sgui),
 * launched via `Main --nstructure`. Not the default boot path -- see Main.main.
 */
public class NewMain {

public static OntoDirectoryService servicePort;
public static ApplicationController guiAdapter;

private static void onGuiLoad() {
	JFrame frame = new JFrame("Onto Directory (nStructure)");
	frame.setSize(1200, 800);
	guiAdapter.deployTo(frame);
}

public static void main(String @NotNull [] args) {
	// 1. ARM THE TRIPWIRE FIRST
	Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
		System.err.println("Uncaught Exception intercepted on thread: " + thread.getName());
		if (servicePort != null) {
			servicePort.reportFatalError(exception);
		} else {
			// PRE-BOOT FAILURE: servicePort isn't alive yet. Force a global dump manually.
			System.err.println("Pre-boot failure detected. Routing to global fallback.");
			java.nio.file.Path fallback = java.nio.file.Paths.get(System.getProperty("user.home"), ".config", "onto-directory");
			CrashReporter.generateGlobalDump(exception, fallback);
		}
	});

	// 2. NOW PROCEED WITH BOOT SEQUENCE (GUI only -- nStructure has no CLI adapter yet)
	SettingLogic.onLoad();
	servicePort = new ArchiverServer();

	com.formdev.flatlaf.FlatDarkLaf.setup();
	guiAdapter = new ApplicationController(servicePort);
	CrashReporter.registerProvider(guiAdapter);
	SwingUtilities.invokeLater(NewMain::onGuiLoad);
}

}
