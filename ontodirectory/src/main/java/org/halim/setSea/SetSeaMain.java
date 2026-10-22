package org.halim.setSea;

import org.halim.legacy.SettingLogic;
import org.halim.nStructure.mhexagon.CrashReporter;
import org.halim.nStructure.sgui.ApplicationController;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;


/** The nStructure GUI over Set Seas: NewMain's boot with a {@link SetSeaServer} in place of the ontology package's server. */
public class SetSeaMain {

public static SetSeaServer servicePort;
public static ApplicationController guiAdapter;

static void onGuiLoad() {
	JFrame frame = new JFrame("Onto Directory (Set Sea)");
	frame.setSize(1200, 800);
	guiAdapter.deployTo(frame);
}

public static void main(String @NotNull [] args) {
	Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
		System.err.println("Uncaught Exception intercepted on thread: " + thread.getName());
		if(servicePort != null) {
			servicePort.reportFatalError(exception);
		} else {
			System.err.println("Pre-boot failure detected. Routing to global fallback.");
			java.nio.file.Path fallback = java.nio.file.Paths.get(System.getProperty("user.home"), ".config", "onto-directory");
			CrashReporter.generateGlobalDump(exception, fallback);
		}
	});

	SettingLogic.onLoad();
	servicePort = new SetSeaServer();

	com.formdev.flatlaf.FlatDarkLaf.setup();
	guiAdapter = new ApplicationController(servicePort);
	CrashReporter.registerProvider(guiAdapter);
	SwingUtilities.invokeLater(SetSeaMain::onGuiLoad);
}

}
