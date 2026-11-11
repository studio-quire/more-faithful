package org.studioquire.oldfaithful.core.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.studioquire.oldfaithful.options.settings.ConfigFile;

/**
 * {@code <world>/faithful-world.properties}: beside the world rather than in level.dat, whose extra keys die the first time
 * vanilla saves it. Written only when there's something to say, whole or not at all. Other mods' lines stay as they are.
 * The install's {@code config/faithful-rules.properties}, what its new worlds start with, is kept the same way under words of its own.
 */
final class WorldFile {
	static final String NAME = "faithful-world.properties";
	private static final String HEADER = "# Faithful Library: this world's rules and the content mods it has seen. Safe to edit with the world closed.\nformat=1\n";

	private final Path file;
	private final String header;
	private String text = "";

	WorldFile(Path file) {
		this(file, HEADER);
	}

	/** With the words a file the game makes here starts with. */
	WorldFile(Path file, String header) {
		this.file = file;
		this.header = header;
		if (Files.isRegularFile(file)) {
			try {
				text = ConfigFile.read(file);
			} catch (IOException e) {
				System.out.println("[faithful-core] WARN Could not read " + file + ", so it's taken as empty: " + e);
			}
		}
	}

	Properties values() {
		return ConfigFile.parse(text);
	}

	boolean set(String key, String value) {
		return set(Collections.singletonMap(key, value));
	}

	/** Several lines in one write, in the map's order. */
	boolean set(Map<String, String> lines) {
		String next = text.isEmpty() ? header : text;
		for (Map.Entry<String, String> e : lines.entrySet()) next = ConfigFile.set(next, e.getKey(), e.getValue());
		try {
			Files.createDirectories(file.getParent());
			ConfigFile.write(file, next);
		} catch (IOException e) {
			System.out.println("[faithful-core] WARN Could not save " + String.join(", ", lines.keySet()) + " in " + file + ": " + e);
			return false;
		}
		text = next;
		return true;
	}
}
