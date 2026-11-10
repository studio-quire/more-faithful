package org.studioquire.faithful.harness;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;

/**
 * Whole numbers in mods' static fields that could be ids. The task compares two numberings: one that stays put while the thing it named moved is a hard-coded number.
 * Classes that won't load on this side, and mixins, are skipped.
 */
final class Statics {
	private Statics() {
	}

	static void write(Path file) throws IOException {
		List<String> lines = new ArrayList<>();
		for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
			String id = mod.getMetadata().getId();
			if (id.equals("harness-probe") || !(mod.getMetadata().containsCustomValue("faithful"))) continue;
			for (Path root : mod.getRootPaths()) {
				try (Stream<Path> walk = Files.walk(root)) {
					for (Path p : (Iterable<Path>) walk::iterator) {
						String name = root.relativize(p).toString().replace('\\', '/');
						if (name.endsWith(".class") && !name.contains("/mixin/") && !name.contains("/client/")) read(id, name.substring(0, name.length() - 6).replace('/', '.'), lines);
					}
				}
			}
		}
		Collections.sort(lines);
		Files.write(file, lines);
	}

	private static void read(String mod, String className, List<String> lines) {
		try {
			Class<?> type = Class.forName(className, false, Statics.class.getClassLoader());
			for (Field f : type.getDeclaredFields()) {
				if (!Modifier.isStatic(f.getModifiers()) || (f.getType() != int.class && f.getType() != short.class)) continue;
				f.setAccessible(true);
				int value = ((Number) f.get(null)).intValue();
				if (value >= ContentRegistry.FIRST_BLOCK && value < ContentRegistry.ITEM_LIMIT) lines.add(mod + "\t" + className + "." + f.getName() + "\t" + value);
			}
		} catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
			// Not for this side.
		}
	}
}
