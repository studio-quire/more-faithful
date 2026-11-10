package org.studioquire.oldfaithful.core.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.studioquire.oldfaithful.options.settings.ConfigFile;

/**
 * {@code <world>/faithful-world.properties}: beside the world rather than in level.dat, whose extra keys die the first time
 * vanilla saves it. Written only when there's something to say, whole or not at all. Other mods' lines stay as they are.
 */
final class WorldFile {
	static final String NAME = "faithful-world.properties";
	private static final String HEADER = "# Faithful Library: this world's rules and the content mods it has seen. Safe to edit with the world closed.\nformat=1\n";

	private final Path file;
	private String text = "";

	WorldFile(Path file) {
		this.file = file;
		if (Files.isRegularFile(file)) {
			try {
				text = ConfigFile.read(file);
			} catch (IOException e) {
				System.out.println("[faithful-core] WARN Could not read " + file + ", so this world plays on its defaults: " + e);
			}
		}
	}

	Properties values() {
		return ConfigFile.parse(text);
	}

	boolean set(String key, String value) {
		String next = ConfigFile.set(text.isEmpty() ? HEADER : text, key, value);
		try {
			Files.createDirectories(file.getParent());
			ConfigFile.write(file, next);
		} catch (IOException e) {
			System.out.println("[faithful-core] WARN Could not save " + key + " in " + file + ": " + e);
			return false;
		}
		text = next;
		return true;
	}
}
