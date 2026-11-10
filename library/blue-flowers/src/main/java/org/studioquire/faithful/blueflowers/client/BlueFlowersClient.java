package org.studioquire.faithful.blueflowers.client;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.resource.language.TranslationStorage;
import org.studioquire.faithful.blueflowers.mixin.TranslationStorageAccessor;
import org.studioquire.oldfaithful.core.api.FaithfulClientMod;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.options.settings.ConfigFile;

/**
 * Beta's yellow flower, "Flower" in beta, shown as Dandelion while the switch on this mod's page is on (the default). Only the name: the block, its id and everything it does stay
 * beta's. Names are the client's own in beta, so this works on any server, and a server has nothing to show them on.
 */
public final class BlueFlowersClient implements FaithfulClientMod {
	static final String FILE = "blue-flowers.properties", KEY = "dandelion";
	private static final String NAME = "tile.flower.name";

	/** What the name was before this changed it: beta's, or whatever another mod's lang file made it. */
	private static String before;

	@Override
	public void initClient(Mod mod) {
		String value = null;
		Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE);
		try {
			value = ConfigFile.parse(ConfigFile.read(file)).getProperty(KEY);
		} catch (IOException | RuntimeException e) {
			System.out.println("[blue-flowers] WARN Could not read " + file + ", so the yellow flower is called Dandelion: " + e);
		}
		dandelion(on(value));
	}

	/** On unless the file says false, as the settings screen reads anything it doesn't know as on. */
	static boolean on(String value) {
		return value == null || !value.trim().equalsIgnoreCase("false");
	}

	/** At once: the inventory's and the statistics' tooltips read the name each time they draw it. */
	public static synchronized void dandelion(boolean on) {
		Properties names = ((TranslationStorageAccessor) TranslationStorage.getInstance()).blueflowers$translations();
		if (before == null) before = names.getProperty(NAME, "Flower");
		names.setProperty(NAME, on ? "Dandelion" : before);
	}
}
