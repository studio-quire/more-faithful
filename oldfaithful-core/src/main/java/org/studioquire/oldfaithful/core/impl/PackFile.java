package org.studioquire.oldfaithful.core.impl;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The file a content pack (More Faithful) carries and jars put in by hand don't: {@code mods-library/pack.properties} in a pack's game folder,
 * {@code pack.properties} beside a server package's start scripts. It's the pack's own, never the player's: nothing in the game writes it, and it sits
 * outside {@code config/}, which a player copies from an old instance into a new one. It holds the pack's {@code name} and {@code version}, which a
 * client shows ({@code client.Pack}), and may hold {@code rule.<mod>.<rule>} lines, what the pack starts its new worlds with where the player hasn't
 * chosen ({@link Rules}).
 */
public final class PackFile {
	public static final String IN_GAME = "mods-library/pack.properties", ON_SERVER = "pack.properties";

	private static final String WHERE = FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER ? ON_SERVER : IN_GAME;
	private static final Properties VALUES = read();

	private PackFile() {
	}

	private static Properties read() {
		Properties p = new Properties();
		Path file = FabricLoader.getInstance().getGameDir().resolve(WHERE);
		if (!Files.isRegularFile(file)) return p;
		try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			p.load(in);
		} catch (IOException | IllegalArgumentException e) {
			System.out.println("[faithful-core] WARN couldn't read " + WHERE + ", so the game goes on as it would without it: " + e);
			p.clear();
		}
		return p;
	}

	/** Where the file is here, from the game's folder. */
	public static String where() {
		return WHERE;
	}

	/** A line's value, trimmed, or null where the file doesn't say (or there's no file). */
	public static String get(String key) {
		String v = VALUES.getProperty(key);
		return v == null ? null : v.trim();
	}
}
