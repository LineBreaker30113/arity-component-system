package org.halim.nStructure.sgui.sglib;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DailyTipParser {

private static final List<String> cachedTips = new ArrayList<>();
private static final Random random = new Random();

public static String getRandomTip() {
	if (cachedTips.isEmpty()) {
		loadTips();
	}
	if (cachedTips.isEmpty()) {
		return "Organize your files dynamically using the Ontology Engine.";
	}
	return cachedTips.get(random.nextInt(cachedTips.size()));
}

private static void loadTips() {
	try (InputStream is = DailyTipParser.class.getResourceAsStream("/tips.md")) {
		if (is == null) {
			System.err.println("Could not find tips.md in resources!");
			return;
		}
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
		StringBuilder currentTip = new StringBuilder();
		String line;
		
		while ((line = reader.readLine()) != null) {
			String trimmed = line.trim();
			if (trimmed.equals("---")) {
				if (!currentTip.isEmpty()) {
					cachedTips.add(currentTip.toString().trim());
					currentTip.setLength(0);
				}
			} else if (!trimmed.isEmpty()) {
				currentTip.append(trimmed).append(" ");
			}
		}
		// Add the last tip if the file doesn't end with ---
		if (!currentTip.isEmpty()) {
			cachedTips.add(currentTip.toString().trim());
		}
		
	} catch (Exception e) {
		System.err.println("Failed to parse tips.md: " + e.getMessage());
	}
}
}