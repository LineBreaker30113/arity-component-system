package org.halim.nStructure.sgui;

import org.halim.nStructure.archive.core.ArchiveManager;
import org.halim.nStructure.mhexagon.DiagnosticStateProvider;
import org.halim.nStructure.mhexagon.OntoDirectoryService;
import org.halim.setSea.SetSeaManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Handles the deployment and primary orchestration of the Swing UI Adapter.
 * It strictly communicates through the OntoDirectoryService Port and manages
 * the background Debouncer thread for atomic state persistence.
 */
public class ApplicationController implements DiagnosticStateProvider {

public final OntoDirectoryService servicePort;
public GUI_RootPanel view;
public WorkspaceController wsModeller;

// --- Concurrency: Debouncer Engine ---
private ScheduledExecutorService backgroundSaver;
private OntoDirectoryService.DataLakeService activeLake;

public ApplicationController(OntoDirectoryService servicePort) {
	this.servicePort = servicePort;
	this.wsModeller = new WorkspaceController(this);
	
	// Initialize the UI strictly on the Event Dispatch Thread
	SwingUtilities.invokeLater(() -> {
		this.view = new GUI_RootPanel(this);
		this.servicePort.addOntoDirectoryServiceListener(new ServiceListenerImpl());
	});
}

public void deployTo(@NotNull JFrame applicationWindow) {
	applicationWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	applicationWindow.add(view);
	applicationWindow.pack();
	applicationWindow.setLocationRelativeTo(null);
	applicationWindow.setVisible(true);
}

/**
 * Executes the lifecycle teardown of the active Lake.
 * Invoked by UI actions (like the Close button on the lake tab).
 */
public void dispatchLakeCloseRequest(OntoDirectoryService.DataLakeService lake) {
	if (this.activeLake == lake) {
		if (backgroundSaver != null && !backgroundSaver.isShutdown()) backgroundSaver.shutdown();
		if (lake instanceof ArchiveManager am && am.isDirty()) am.saveChanges();
		if (lake instanceof SetSeaManager sea && sea.isDirty()) sea.saveChanges();
		this.activeLake = null;
	}
	servicePort.closeLake(lake);
	wsModeller.triggerLakeRefresh(null);
	view.centerPanel.showPage("WELCOME"); // Return home on close
}

// =========================================================================
// RSE TELEMETRY: DIAGNOSTIC STATE PROVIDER
// =========================================================================

@Override
public String getLayerName() {
	return "GUI_ADAPTER";
}

@Override
public String captureStateDump() {
	StringBuilder dump = new StringBuilder();
	dump.append("  Active UI Context : ");
	if (view != null && view.centerPanel != null) {
		dump.append(activeLake == null ? "[WELCOME_PAGE]\n" : "[WORKSPACE_PAGE]\n");
	} else {
		dump.append("[PRE_INIT]\n");
	}
	
	if (activeLake != null && view != null && view.workspacePanel != null) {
		dump.append("  Visible Workspaces: ").append(view.workspacePanel.getActiveViewNames()).append("\n");
	}
	
	dump.append("  Background Saver  : ").append(backgroundSaver != null && !backgroundSaver.isShutdown() ? "ACTIVE" : "INACTIVE").append("\n");
	return dump.toString();
}

// Inner class to handle callbacks from the Server Adapter Boundaries
private class ServiceListenerImpl implements OntoDirectoryService.OntoDirectoryServiceListener {
	@Override
	public void onGUI_Change() {
		SwingUtilities.invokeLater(() -> {
			if (view.centerPanel.welcomePage != null) {
				view.centerPanel.welcomePage.refreshLakes(); // CRITICAL: Sync the UI
			}
			view.leftSidebar.revalidate();
			view.revalidate();
			view.repaint();
		});
	}
	
	@Override
	public void onDataLakeLoad(OntoDirectoryService.DataLakeService dataLakeService) {
		if (ApplicationController.this.activeLake == dataLakeService) {
			SwingUtilities.invokeLater(() -> view.centerPanel.showPage("WORKSPACE"));
			return;
		}
		// 1. Safely tear down existing debouncer if switching lakes
		if (backgroundSaver != null && !backgroundSaver.isShutdown()) {
			backgroundSaver.shutdown();
		}
		
		activeLake = dataLakeService;
		
		// 2. Initialize the Debouncer Worker Thread
		backgroundSaver = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "OntoDir-Debouncer-Thread");
			t.setDaemon(true); // Don't block JVM shutdown
			return t;
		});
		
		// 3. The Flush Contract: Check dirty flag every 3 seconds
		backgroundSaver.scheduleAtFixedRate(() -> {
			if (activeLake instanceof ArchiveManager am) {
				if (am.isDirty()) {
					am.saveChanges();
				}
			}
			if (activeLake instanceof SetSeaManager sea && sea.isDirty()) sea.saveChanges();
		}, 3, 3, TimeUnit.SECONDS);
		
		// 4. Update the Swing UI
		SwingUtilities.invokeLater(() -> {
			view.leftSidebar.registerDataLake(dataLakeService.getRootPath());
			view.centerPanel.showPage("WORKSPACE");
			wsModeller.triggerLakeRefresh(dataLakeService);
		});
	}
	
	@Override
	public void showBugReport(java.nio.file.Path reportFile) {
		SwingUtilities.invokeLater(() -> {
			org.halim.nStructure.mhexagon.CrashReporter.log("[GUI] Displaying Fatal Error Dialog to User.");
			
			JDialog crashDialog = new JDialog((java.awt.Frame) null, "Critical System Failure", true);
			crashDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			crashDialog.setLayout(new java.awt.BorderLayout(15, 15));
			crashDialog.getContentPane().setBackground(new java.awt.Color(30, 30, 40)); // Match WP_BG
			
			// Header
			JLabel alertLabel = new JLabel("<html><div style='padding: 10px;'>" +
				  "<h2 style='color: #FF5555;'>Onto Directory Has Halted</h2>" +
				  "<p style='color: #EEEEEE;'>A fatal desynchronization occurred. The system has safely halted to prevent data corruption.</p>" +
				  "<p style='color: #EEEEEE;'>A privacy-masked diagnostic dump has been generated for forensic analysis.</p>" +
				  "</div></html>");
			crashDialog.add(alertLabel, java.awt.BorderLayout.NORTH);
			
			// File Path Area
			JTextArea pathArea = new JTextArea(reportFile.toAbsolutePath().toString());
			pathArea.setEditable(false);
			pathArea.setLineWrap(true);
			pathArea.setWrapStyleWord(true);
			pathArea.setBackground(new java.awt.Color(20, 20, 20));
			pathArea.setForeground(new java.awt.Color(150, 150, 150));
			pathArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			crashDialog.add(new JScrollPane(pathArea), java.awt.BorderLayout.CENTER);
			
			// Actions
			JPanel buttonPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
			buttonPanel.setOpaque(false);
			
			JButton openLogBtn = new JButton("Open Log Directory");
			openLogBtn.addActionListener(e -> {
				try {
					java.awt.Desktop.getDesktop().open(reportFile.getParent().toFile());
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			});
			
			JButton exitBtn = new JButton("Force Terminate System");
			exitBtn.addActionListener(e -> {
				System.err.println("System terminated via user interaction with Crash Dialog.");
				if (activeLake != null) {
					try {
						System.err.println("Attempting emergency memory rescue...");
						activeLake.saveChanges();
						System.err.println("Emergency rescue successful.");
					} catch (Exception ex) {
						System.err.println("Emergency rescue failed. Memory state is unsalvageable.");
					}
				}
				System.exit(1);
			});
			
			JButton dismissBtn = new JButton("Dismiss Dialog");
			dismissBtn.addActionListener(e -> crashDialog.dispose());
			
			buttonPanel.add(openLogBtn);
			buttonPanel.add(dismissBtn);
			buttonPanel.add(exitBtn);
			crashDialog.add(buttonPanel, java.awt.BorderLayout.SOUTH);
			
			crashDialog.setSize(500, 300);
			crashDialog.setLocationRelativeTo(null); // Center on screen
			crashDialog.setVisible(true);
		});
	}
	
}
}