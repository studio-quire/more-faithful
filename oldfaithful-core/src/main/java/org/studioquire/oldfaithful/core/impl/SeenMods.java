package org.studioquire.oldfaithful.core.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Which content mods a world has seen, in its file. Blocks survive a removed mod while the core's here; items, entities and block
 * entities don't. So a world missing one says so before it loads: this is the only thing about a world that ever asks.
 */
public final class SeenMods {
	private static final String PREFIX = "mod.";
	/** On a server, set by hand in the world's file once a backup is made. */
	static final String ACCEPT = "accept-missing-content";

	private SeenMods() {
	}

	/** The content mods this world has seen that aren't here. */
	public static List<String> missing(Path world) {
		Properties p = new WorldFile(world.resolve(WorldFile.NAME)).values();
		List<String> gone = new ArrayList<>();
		for (String key : new TreeSet<>(p.stringPropertyNames())) {
			if (!key.startsWith(PREFIX) || p.getProperty(key).trim().isEmpty()) continue;
			String id = key.substring(PREFIX.length());
			boolean here = false;
			for (ModInfo m : Core.mods()) here |= m.content && m.id.equals(id);
			if (!here) gone.add(id);
		}
		return gone;
	}

	/** On a server: stops rather than lose what it can't read, unless the owner has said to go on. */
	static void checkServer(Path world) {
		List<String> gone = missing(world);
		if (gone.isEmpty()) return;
		WorldFile file = new WorldFile(world.resolve(WorldFile.NAME));
		if (Boolean.parseBoolean(file.values().getProperty(ACCEPT, "false").trim())) {
			accept(world, gone);
			// For this removal only: a later one is refused again.
			new WorldFile(world.resolve(WorldFile.NAME)).set(ACCEPT, "false");
			return;
		}
		throw new IllegalStateException("This world has content from " + gone + ", which isn't installed. Its blocks would show as stand-ins and come back with the mod, but its items, "
				+ "entities and block entities would be lost as chunks save. Back the world up, then set " + ACCEPT + "=true in " + world.resolve(WorldFile.NAME) + " to go on.");
	}

	/** Going on without them: the world stops asking about these. */
	public static void accept(Path world, List<String> gone) {
		WorldFile file = new WorldFile(world.resolve(WorldFile.NAME));
		for (String id : gone) file.set(PREFIX + id, "");
	}

	/** Notes the content mods here; called as a world opens. */
	static void note(Path world) {
		WorldFile file = null;
		for (ModInfo m : Core.mods()) {
			if (!m.content) continue;
			if (file == null) file = new WorldFile(world.resolve(WorldFile.NAME));
			String line = m.version + ";data=" + m.network;
			if (!line.equals(file.values().getProperty(PREFIX + m.id, "").trim())) file.set(PREFIX + m.id, line);
		}
	}

	/** A zip of the world beside it; its name, or null if it couldn't be made. */
	public static String backup(Path world) {
		String name = world.getFileName() + "-backup-" + new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date()) + ".zip";
		Path zip = world.resolveSibling(name);
		try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip)); java.util.stream.Stream<Path> walk = Files.walk(world)) {
			for (Path p : (Iterable<Path>) walk::iterator) {
				if (Files.isDirectory(p)) continue;
				out.putNextEntry(new ZipEntry(world.relativize(p).toString().replace('\\', '/')));
				Files.copy(p, out);
				out.closeEntry();
			}
			return name;
		} catch (IOException e) {
			System.out.println("[faithful-core] WARN Could not back up " + world + ": " + e);
			return null;
		}
	}
}
