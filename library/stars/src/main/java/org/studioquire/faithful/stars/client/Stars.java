package org.studioquire.faithful.stars.client;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;
import org.studioquire.oldfaithful.core.api.FaithfulClientMod;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.options.settings.ConfigFile;

/**
 * The night sky's stars as the mod's page has them, read from {@code config/stars.properties} at start and changed at once from the page. Each setting is a share of what
 * beta does, and Beta is a share of exactly one, so at Beta the mixin hands beta's own numbers back unchanged. The stars start smallest, as bright as beta's and more of
 * them, nearer how later versions draw the sky.
 * <p>
 * Beta places its stars once, from a fixed seed: 1500 tries at a point in a cube, each kept if it lies inside the sphere (about 780 do), as a square 0.25 to 0.5 of a block
 * from its middle to its side, 100 away. Number changes how many tries are made, so More goes on past beta's 1500 with the same draws and Fewer stops short of them: beta's
 * stars stay where they are either way. Size scales every square about its own middle. The stars are drawn adding their light, with colour and alpha both the sky's
 * darkness (none by day, at most a half at midnight, less in rain), so what reaches the screen goes as its square, a quarter of white at best; Brightness scales that,
 * which keeps how they come out at dusk and fade in rain.
 */
public final class Stars implements FaithfulClientMod {
	static final String FILE = "stars.properties";
	static final String SHOWN = "shown";

	/**
	 * One setting's steps in the order a click goes through them, Beta first, then up to the most and round from the least: the file's word, the button's, and the share;
	 * and the step it starts on.
	 */
	static final class Steps {
		final String key, start;
		final String[] values, words;
		final float[] shares;

		Steps(String key, String start, String[] values, String[] words, float[] shares) {
			this.key = key;
			this.start = start;
			this.values = values;
			this.words = words;
			this.shares = shares;
		}

		/** The starting step's share for nothing or anything the file holds that isn't one of the steps, as the settings screen shows it. */
		float share(String value) {
			String v = value == null ? start : value.trim();
			for (int i = 0; i < values.length; i++) {
				if (values[i].equalsIgnoreCase(v)) return shares[i];
			}
			return value == null ? 1.0F : share(null);
		}
	}

	/** Each star's half-width, as a share of beta's. */
	static final Steps SIZE = new Steps("size", "smallest", new String[] {"beta", "larger", "largest", "smallest", "smaller"},
			new String[] {"Beta", "Larger", "Largest", "Smallest", "Smaller"}, new float[] {1.0F, 1.5F, 2.0F, 0.5F, 0.7F});
	/** What a star adds to the screen, as a share of beta's. */
	static final Steps BRIGHTNESS = new Steps("brightness", "beta", new String[] {"beta", "brighter", "brightest", "dimmest", "dimmer"},
			new String[] {"Beta", "Brighter", "Brightest", "Dimmest", "Dimmer"}, new float[] {1.0F, 2.0F, 4.0F, 0.25F, 0.5F});
	/** How many tries at placing a star, as a share of beta's 1500. */
	static final Steps NUMBER = new Steps("number", "more", new String[] {"beta", "more", "most", "fewest", "fewer"},
			new String[] {"Beta", "More", "Most", "Fewest", "Fewer"}, new float[] {1.0F, 2.0F, 4.0F, 0.25F, 0.5F});

	public static volatile float size = SIZE.share(null), number = NUMBER.share(null);
	/**
	 * What the sky's darkness is multiplied by before beta draws the stars with it: the square root of Brightness, as the light goes as its square, and none while
	 * they're hidden, which beta's own test for darkness then skips drawing, as by day.
	 */
	public static volatile float light = (float) Math.sqrt(BRIGHTNESS.share(null));
	private static boolean shown = true;
	private static float brightness = BRIGHTNESS.share(null);

	@Override
	public void initClient(Mod mod) {
		Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE);
		Properties p = new Properties();
		try {
			p = ConfigFile.parse(ConfigFile.read(file));
		} catch (IOException | RuntimeException e) {
			System.out.println("[stars] WARN Could not read " + file + ", so the stars are as they start: " + e);
		}
		for (String key : new String[] {SHOWN, SIZE.key, BRIGHTNESS.key, NUMBER.key}) set(key, p.getProperty(key));
	}

	/**
	 * One setting's value as the file holds it, null for its default. Taken at once: the light from the next frame, and a new size or number by the world renderer as it
	 * next draws the sky (on the game's thread, which the settings screen's calls are on too).
	 */
	public static void set(String key, String value) {
		if (key.equals(SHOWN)) shown = value == null || !value.trim().equalsIgnoreCase("false");
		else if (key.equals(SIZE.key)) size = SIZE.share(value);
		else if (key.equals(BRIGHTNESS.key)) brightness = BRIGHTNESS.share(value);
		else if (key.equals(NUMBER.key)) number = NUMBER.share(value);
		else throw new IllegalArgumentException(key);
		light = shown ? (float) Math.sqrt(brightness) : 0.0F;
	}
}
