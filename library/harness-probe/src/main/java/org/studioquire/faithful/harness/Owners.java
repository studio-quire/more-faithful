package org.studioquire.faithful.harness;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

/** Which mod a thing came from. */
final class Owners {
	private Owners() {
	}

	/** The mod whose jar holds this class, or the class's name if it's from none. */
	static String of(Class<?> type) {
		try {
			URL location = type.getProtectionDomain().getCodeSource().getLocation();
			if (location != null) {
				Path jar = Paths.get(location.toURI()).toAbsolutePath();
				for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
					for (Path p : mod.getOrigin().getPaths()) {
						if (p.toAbsolutePath().equals(jar)) return mod.getMetadata().getId();
					}
				}
			}
		} catch (URISyntaxException | RuntimeException e) {
			// Below.
		}
		return type.getName();
	}

	static String namespace(String name) {
		int colon = name.indexOf(':');
		return colon < 0 ? name : name.substring(0, colon);
	}

	static String path(String name) {
		return name.substring(name.indexOf(':') + 1);
	}
}
