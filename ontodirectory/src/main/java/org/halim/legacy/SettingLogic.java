package org.halim.legacy;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Manages the global application state and configuration settings via the W3C DOM.
 * <p>
 * <b>Architectural Note:</b> This class utilizes a "Rebuild on Save" strategy.
 * The Java static variables are the Source of Truth. The XML DOM is freshly
 * constructed from these variables upon every save operation to ensure absolute synchronization.
 */
public class SettingLogic {

private static final Path OS_HOME = Paths.get(System.getProperty("user.home"));
private static final Path DEFAULT_PROD_DIR = OS_HOME.resolve(".config/onto-directory");

// 1. Check for a custom property passed at startup. If it doesn't exist, use the production directory.
static final Path ACTIVE_CONFIG_DIR = Paths.get(System.getProperty("onto.dev.dir", DEFAULT_PROD_DIR.toString()));

// 2. Build the final path
private static final Path LINUX_SFP = ACTIVE_CONFIG_DIR.resolve("settings.xml");

public static boolean isSystemFileChooser = false;
public static boolean isDarkTheme = true; // Placeholder for your forgotten boolean
public static ArrayList<Path> dataLakes = new ArrayList<>();

/**
 * The bootstrap method called upon application startup to load configurations.
 */
public static void onLoad() {
	if (Files.exists(LINUX_SFP)) {
		readSetts(LINUX_SFP.toFile());
		System.out.println("SF:" + isSystemFileChooser + "\t DT:" + isDarkTheme);
		dataLakes.forEach(dl -> System.out.println(dl.toAbsolutePath().toString()));
	} else {
		initSetts();
		saveSetts(); // Generate the initial XML file immediately
	}
}

/**
 * Initializes a blank state when no settings file is present.
 */
private static void initSetts() {
	isSystemFileChooser = false;
	isDarkTheme = true;
	dataLakes = new ArrayList<>();
}

/**
 * Deserializes the XML settings file into memory using the DOM.
 */
private static void readSetts(File xmlFile) {
	try {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(xmlFile);
		doc.getDocumentElement().normalize();
		
		// Read Booleans
		isSystemFileChooser = parseBoolean(doc, "isSystemFileChooser", false);
		isDarkTheme = parseBoolean(doc, "isDarkTheme", true);
		
		// Read Data Lakes
		dataLakes.clear();
		NodeList lakeNodes = doc.getElementsByTagName("lake");
		for (int i = 0; i < lakeNodes.getLength(); i++) {
			String pathStr = lakeNodes.item(i).getTextContent();
			dataLakes.add(Path.of(pathStr));
		}
		
	} catch (Exception e) {
		System.err.println("Critical failure reading settings.xml: " + e.getMessage());
		initSetts(); // Fallback to safe defaults if the file is corrupted
	}
}

/**
 * Helper method to safely extract a boolean from an XML element.
 */
private static boolean parseBoolean(Document doc, String tagName, boolean defaultValue) {
	NodeList nodes = doc.getElementsByTagName(tagName);
	if (nodes.getLength() > 0) {
		return Boolean.parseBoolean(nodes.item(0).getTextContent());
	}
	return defaultValue;
}

/**
 * Serializes the current in-memory state back to the XML file.
 */
private static void saveSetts() {
	try {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.newDocument();
		
		// 1. Create Root Element
		Element rootElement = doc.createElement("settings");
		doc.appendChild(rootElement);
		
		// 2. Append Booleans
		appendElement(doc, rootElement, "isSystemFileChooser", String.valueOf(isSystemFileChooser));
		appendElement(doc, rootElement, "isDarkTheme", String.valueOf(isDarkTheme));
		
		// 3. Append Data Lakes
		Element lakesElement = doc.createElement("dataLakes");
		rootElement.appendChild(lakesElement);
		for (Path lake : dataLakes) {
			appendElement(doc, lakesElement, "lake", lake.toString());
		}
		
		// 4. Transform and Write to Disk
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		
		// CRITICAL: Use NIO to create the parent directories safely
		Files.createDirectories(LINUX_SFP.getParent());
		
		// Convert Path to File for StreamResult compatibility
		File file = LINUX_SFP.toFile();
		
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(file);
		transformer.transform(source, result);
		
	} catch (Exception e) {
		System.err.println("Failed to save settings.xml: " + e.getMessage());
	}
}

/**
 * Helper method to create a text-only XML node and attach it to a parent.
 */
private static void appendElement(@NotNull Document doc, @NotNull Element parent, String tagName, String textContent) {
	Element element = doc.createElement(tagName);
	element.setTextContent(textContent);
	parent.appendChild(element);
}

// =========================================================================
// Public Mutator API (State Modification Methods)
// =========================================================================

/** Adds a new Data Lake path and immediately saves the configuration. */
public static void addDataLake(Path lakePath) {
	if (!dataLakes.contains(lakePath)) {
		dataLakes.add(lakePath);
		saveSetts();
	}
}

/** Removes a Data Lake path and immediately saves the configuration. */
public static void removeDataLake(Path lakePath) {
	if (dataLakes.remove(lakePath)) {
		saveSetts();
	}
}

/** Updates the file chooser preference and immediately saves the configuration. */
public static void setSystemFileChooser(boolean value) {
	if (isSystemFileChooser != value) {
		isSystemFileChooser = value;
		saveSetts();
	}
}

/** Updates the theme preference and immediately saves the configuration. */
public static void setSystemTheme(boolean value) {
	if (isDarkTheme != value) {
		isDarkTheme = value;
		saveSetts();
	}
}
}