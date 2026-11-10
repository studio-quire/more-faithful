package org.studioquire.oldfaithful.core.impl.client;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

/**
 * The pack this game was installed as, if any. A content pack (More Faithful) carries {@code mods-library/pack.properties}
 * with its {@code name} and {@code version}, and while any of its mods is on (so this core is loaded) the game names it where
 * it names its edition: the title screen's line, the settings button in Options and the settings screen's title. The jars
 * put into an instance by hand bring no such file, so a Still Faithful instance with them stays Still Faithful; and with
 * every mod off the pack is Still Faithful, as it plays.
 */
public final class Pack {
	public static final String FILE = "mods-library/pack.properties";
	/** The settings button Faithful Settings puts in Options (its {@code OptionsScreenMixin}'s id), whatever an edition calls it. */
	public static final int SETTINGS_BUTTON = 7071;

	private static final String NAME, VERSION;

	static {
		String name = null, version = null;
		Path file = FabricLoader.getInstance().getGameDir().resolve(FILE);
		if (Files.isRegularFile(file)) {
			Properties p = new Properties();
			try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				p.load(in);
				name = p.getProperty("name", "").trim();
				version = p.getProperty("version", "").trim();
			} catch (IOException | IllegalArgumentException e) {
				System.out.println("[faithful-core] WARN couldn't read " + FILE + ", so the game names its edition as it would without it: " + e);
			}
			if (name != null && name.isEmpty()) name = null;
			if (name != null) System.out.println("[faithful-core] This is " + name + " " + version + " (" + FILE + ")");
		}
		NAME = name;
		VERSION = version;
	}

	private Pack() {
	}

	/** The pack's name, or null where there's no pack file. */
	public static String name() {
		return NAME;
	}

	/** The title screen's line: the pack's name and version, or the line as it was. */
	public static String titleLine(String edition) {
		return NAME == null ? edition : VERSION.isEmpty() ? NAME : NAME + " " + VERSION;
	}
}
