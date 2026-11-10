package org.studioquire.oldfaithful.core.impl.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundEntry;
import org.studioquire.oldfaithful.core.mixin.client.SoundEntryAccessor;

/**
 * Every mod's {@code assets/<modid>/sounds/}, straight from its jar. {@code SoundEntry.loadStatic} wants a {@code File}, but a {@code Sound} only holds a URL, so
 * these go in its tables by hand, under the names it would have made.
 */
public final class ModSounds {
	private ModSounds() {
	}

	/** Each time the sound manager is made: it starts empty after a resource reload too. */
	public static void load(SoundEntry sounds, SoundEntry streaming, SoundEntry music) {
		for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
			String id = mod.getMetadata().getId();
			for (Path root : mod.getRootPaths()) {
				Path dir = root.resolve("assets").resolve(id).resolve("sounds");
				if (!Files.isDirectory(dir)) continue;
				try (Stream<Path> walk = Files.walk(dir)) {
					for (Path file : walk.filter(Files::isRegularFile).sorted().collect(Collectors.toList())) load(id, dir, file, sounds, streaming, music);
				} catch (IOException e) {
					System.out.println("[faithful-core] WARN Could not read the sounds of " + id + ": " + e);
				}
			}
		}
	}

	private static void load(String mod, Path dir, Path file, SoundEntry sounds, SoundEntry streaming, SoundEntry music) throws MalformedURLException {
		String path = dir.relativize(file).toString().replace('\\', '/');
		int dot = path.lastIndexOf('.');
		String type = dot < 0 ? "" : path.substring(dot + 1);
		if (!type.equals("ogg") && !type.equals("wav")) return;
		String stem = path.substring(0, dot);
		SoundEntry entry = sounds;
		if (stem.startsWith("music/")) {
			entry = music;
			stem = stem.substring("music/".length());
		} else if (stem.startsWith("streaming/")) {
			entry = streaming;
			stem = stem.substring("streaming/".length());
		}
		if (entry.isRandom) {
			while (!stem.isEmpty() && Character.isDigit(stem.charAt(stem.length() - 1))) stem = stem.substring(0, stem.length() - 1);
		}
		add(entry, mod + "." + stem.replace('/', '.'), new Sound(mod + "/" + path, file.toUri().toURL()));
	}

	@SuppressWarnings("unchecked")
	private static void add(SoundEntry entry, String name, Sound sound) {
		SoundEntryAccessor tables = (SoundEntryAccessor) entry;
		((List<Sound>) tables.faithful$sets().computeIfAbsent(name, n -> new ArrayList<Sound>())).add(sound);
		tables.faithful$all().add(sound);
		entry.loadedSoundCount++;
	}
}
