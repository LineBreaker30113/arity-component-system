package org.halim.nStructure.sgui.sglib;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ManualParser {

/** Simple Data Transfer Object (DTO) to hold our manual entries */
public static class ManualEntry {
	public final String title;
	public final String summary;
	public final String details;
	
	public ManualEntry(String title, String summary, String details) {
		this.title = title;
		this.summary = summary;
		this.details = details;
	}
}

/**
 * Parses the manual.md file using a linear state machine.
 */
public static List<ManualEntry> loadManual() {
	List<ManualEntry> entries = new ArrayList<>();
	
	try (InputStream is = ManualParser.class.getResourceAsStream("/manual.md")) {
		if (is == null) {
			System.err.println("Could not find manual.md in resources!");
			return entries;
		}
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
		
		String line;
		String currentTitle = "";
		StringBuilder currentSummary = new StringBuilder();
		StringBuilder currentDetails = new StringBuilder();
		
		int htmlBlockCount = 0;
		boolean insideHtmlBlock = false;
		
		while ((line = reader.readLine()) != null) {
			if(line.startsWith("//")) { continue; }
			String trimmed = line.trim();
			
			// STATE 1: Found a new Topic Header
			if (trimmed.startsWith("# ")) {
				currentTitle = trimmed.substring(2).trim();
				currentSummary.setLength(0); // Reset for new topic
				currentDetails.setLength(0);
				htmlBlockCount = 0;
				insideHtmlBlock = false;
			}
			// STATE 2: Found a code block boundary (``` or ```html)
			else if (trimmed.startsWith("````")) {
				if (!insideHtmlBlock) {
					// Opening a new block
					insideHtmlBlock = true;
					htmlBlockCount++;
				} else {
					// Closing the current block
					insideHtmlBlock = false;
					
					// If we just closed the second block, the entry is complete. Package it up.
					if (htmlBlockCount == 2 && !currentTitle.isEmpty()) {
						entries.add(new ManualEntry(
							  currentTitle,
							  (//"<span style=\"font-size: 14px\">" +
								    currentSummary.toString()
								    //+ "</span>"
							  ).trim(),
							  (//"<span style=\"font-size: 14px\">" +
								    currentDetails.toString()
								    //+ "</span>"
							  ).trim()
						));
					}
				}
			}
			// STATE 3: Reading raw HTML text inside a block
			else if (insideHtmlBlock) {
				if (htmlBlockCount == 1) {
					currentSummary.append(line).append("\n"); // Append raw line to preserve spacing
				} else if (htmlBlockCount == 2) {
					currentDetails.append(line).append("\n");
				}
			}
		}
	} catch (Exception e) {
		System.err.println("Failed to parse manual.md: " + e.getMessage());
	}
	
	return entries;
}
}