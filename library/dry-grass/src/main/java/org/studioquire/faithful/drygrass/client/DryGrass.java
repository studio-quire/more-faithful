package org.studioquire.faithful.drygrass.client;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.studioquire.oldfaithful.core.api.FaithfulClientMod;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.client.Client;
import org.studioquire.oldfaithful.options.settings.ConfigFile;

/**
 * The two choices, grass and oak leaves, read from {@code config/dry-grass.properties} at start and changed at once from the settings page. Colours are baked into the
 * world's meshes as they're built, so a change has every loaded chunk built again.
 */
public final class DryGrass implements FaithfulClientMod {
	static final String FILE = "dry-grass.properties", COLOUR = "colour", LEAVES = "leaves";

	@Override
	public void initClient(Mod mod) {
		Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE);
		Properties p = new Properties();
		try {
			p = ConfigFile.parse(ConfigFile.read(file));
		} catch (IOException | RuntimeException e) {
			System.out.println("[dry-grass] WARN Could not read " + file + ", so they start as Sage: " + e);
		}
		DryLand.grassTint = DryLand.tint(p.getProperty(COLOUR));
		DryLand.leafTint = DryLand.tint(p.getProperty(LEAVES));
		Client.onWorldChanged((from, to) -> {
			if (from == null && to != null) DryLand.recheck();
		});
	}

	/** The grass's choice changed: taken, and the world drawn again with it. On the game's thread, as the settings screen calls it. */
	static void setGrass(String value) {
		DryLand.grassTint = DryLand.tint(value);
		redraw();
	}

	/** The leaves' choice changed, likewise. */
	static void setLeaves(String value) {
		DryLand.leafTint = DryLand.tint(value);
		redraw();
	}

	private static void redraw() {
		Minecraft mc = Client.minecraft();
		if (mc != null && mc.world != null && mc.worldRenderer != null) mc.worldRenderer.reload();
	}
}
