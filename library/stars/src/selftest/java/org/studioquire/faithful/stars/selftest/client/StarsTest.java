package org.studioquire.faithful.stars.selftest.client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.studioquire.faithful.harness.test.client.ClientTest;
import org.studioquire.faithful.stars.client.StarList;
import org.studioquire.faithful.stars.client.Stars;

/**
 * In single player, fresh: the night sky in pictures, at every step of each setting with the rest at Beta, and what's in them measured, and two at dusk to look at.
 * Every run: a changed size and number recorded into the star list as the sky is next drawn, and Beta's again after.
 */
public final class StarsTest extends ClientTest {
	/** Feet over beta's clouds (at 108), so nothing comes between the camera and the stars, looking 40 degrees up: under the moon, overhead at midnight. */
	private static final double FEET = 121.0;
	private static final float YAW = 30.0F, PITCH = -40.0F;
	private static final long MIDNIGHT = 18000L, DUSK = 13500L;
	/** A channel at this or over is a star's: the night sky's own, this high up, are a few. Beta's stars add 64. */
	private static final int STAR = 8;

	/** A picture's top three fifths, all sky: the pixels a star lights, and the light of a typical one (the median channel among them). */
	private static final class Sky {
		boolean[] lit;
		int count, typical;
	}

	private final Map<String, Sky> skies = new HashMap<>();
	private double x, y, z, eye;
	private float yaw, pitch;
	private long time;
	private boolean raining, thundering, hud;
	private int difficulty, view;

	@Override
	protected void script() {
		then(() -> {
			// Beta draws the sky, and the stars in it, only at Far and Normal render distance.
			view = mc.options.viewDistance;
			mc.options.viewDistance = 0;
		});
		if (!remote() && !restarted()) sky();
		then(() -> {
			Stars.set("size", "smaller");
			Stars.set("number", "more");
		});
		wait(3);
		then(() -> {
			StarList list = (StarList) mc.worldRenderer;
			check("a new Size and Number are recorded into the star list as the sky is next drawn (" + list.stars$drawnSize() + ", " + list.stars$drawnNumber() + ")",
					list.stars$drawnSize() == Stars.size && list.stars$drawnNumber() == Stars.number && Stars.size != 1.0F && Stars.number != 1.0F);
			beta();
		});
		wait(3);
		then(() -> {
			StarList list = (StarList) mc.worldRenderer;
			check("and beta's again at Beta", list.stars$drawnSize() == 1.0F && list.stars$drawnNumber() == 1.0F);
			mc.options.viewDistance = view;
		});
	}

	private static void beta() {
		Stars.set("shown", "true");
		for (String key : new String[] {"size", "brightness", "number"}) Stars.set(key, "beta");
	}

	private void sky() {
		then(() -> {
			x = mc.player.x;
			y = mc.player.y;
			z = mc.player.z;
			eye = mc.player.y - mc.player.boundingBox.minY;
			yaw = mc.player.yaw;
			pitch = mc.player.pitch;
			time = mc.world.getTime();
			raining = mc.world.getProperties().getRaining();
			thundering = mc.world.getProperties().getThundering();
			hud = mc.options.hideHud;
			difficulty = mc.options.difficulty;
			mc.options.hideHud = true;
			// No monster comes for the few seconds of night on the plots round about.
			mc.options.difficulty = 0;
			mc.world.getProperties().setRaining(false);
			mc.world.getProperties().setThundering(false);
		});
		shot("beta", MIDNIGHT);
		shot("hidden", MIDNIGHT, "shown", "false");
		for (String s : new String[] {"smallest", "smaller", "larger", "largest"}) shot("size-" + s, MIDNIGHT, "size", s);
		for (String s : new String[] {"dimmest", "dimmer", "brighter", "brightest"}) shot("brightness-" + s, MIDNIGHT, "brightness", s);
		for (String s : new String[] {"fewest", "fewer", "more", "most"}) shot("number-" + s, MIDNIGHT, "number", s);
		shot("smaller-brighter-more", MIDNIGHT, "size", "smaller", "brightness", "brighter", "number", "more");
		shot("dusk-beta", DUSK);
		shot("dusk-brightest", DUSK, "brightness", "brightest");
		then(this::judge);
		then(() -> {
			beta();
			mc.world.setTime(time);
			mc.world.getProperties().setRaining(raining);
			mc.world.getProperties().setThundering(thundering);
			mc.options.hideHud = hud;
			mc.options.difficulty = difficulty;
			mc.player.setPosition(x, y, z);
			mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0.0;
			mc.player.yaw = mc.player.prevYaw = yaw;
			mc.player.pitch = mc.player.prevPitch = pitch;
		});
	}

	/** Steps: these settings and the rest Beta, the sky held at that hour long enough for every interpolation to settle, then its picture, measured. */
	private void shot(String name, long at, String... settings) {
		then(() -> {
			beta();
			for (int i = 0; i < settings.length; i += 2) Stars.set(settings[i], settings[i + 1]);
		});
		int[] held = {0};
		await("the sky held for " + name, 20, () -> {
			// From the tick's start, before the player moves: with no speed it doesn't, so it gathers no fall.
			mc.world.setTime(at);
			mc.world.setRainGradient(0.0F);
			mc.player.setPosition(x, FEET + eye, z);
			mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0.0;
			mc.player.yaw = mc.player.prevYaw = YAW;
			mc.player.pitch = mc.player.prevPitch = PITCH;
			return ++held[0] >= 4;
		});
		picture(name);
		then(() -> skies.put(name, measure(name)));
	}

	private Sky measure(String name) {
		BufferedImage img;
		try {
			img = ImageIO.read(new File("shots/" + checks().mod() + "/" + name + ".png"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		int w = img.getWidth(), h = img.getHeight() * 3 / 5;
		Sky sky = new Sky();
		sky.lit = new boolean[w * h];
		int[] levels = new int[256];
		for (int py = 0; py < h; py++) {
			for (int px = 0; px < w; px++) {
				int rgb = img.getRGB(px, py);
				int c = Math.max(rgb >> 16 & 0xFF, Math.max(rgb >> 8 & 0xFF, rgb & 0xFF));
				if (c < STAR) continue;
				sky.lit[py * w + px] = true;
				sky.count++;
				levels[c]++;
			}
		}
		for (int c = 0, seen = 0; c < 256; c++) {
			seen += levels[c];
			if (seen * 2 >= sky.count && sky.count > 0) {
				sky.typical = c;
				break;
			}
		}
		note(name + ": " + sky.count + " pixels lit, typically at " + sky.typical);
		return sky;
	}

	private int count(String name) {
		return skies.get(name).count;
	}

	private int typical(String name) {
		return skies.get(name).typical;
	}

	/** Of what {@code a} lights, the share {@code b} lights too. */
	private double kept(String a, String b) {
		boolean[] la = skies.get(a).lit, lb = skies.get(b).lit;
		int both = 0, of = 0;
		for (int i = 0; i < la.length; i++) {
			if (!la[i]) continue;
			of++;
			if (lb[i]) both++;
		}
		return of == 0 ? 0.0 : (double) both / of;
	}

	private static boolean rising(int... values) {
		for (int i = 1; i < values.length; i++) if (values[i] <= values[i - 1]) return false;
		return true;
	}

	private void judge() {
		check("Beta: stars in the night sky (" + count("beta") + " pixels lit), each adding a quarter of white at midnight (typically " + typical("beta") + ")",
				count("beta") >= 50 && typical("beta") >= 60 && typical("beta") <= 72);
		check("Hidden: no star (" + count("hidden") + " pixels lit)", count("hidden") == 0);
		check("Size: the stars cover more at each step up (" + count("size-smallest") + ", " + count("size-smaller") + ", " + count("beta") + ", " + count("size-larger") + ", "
				+ count("size-largest") + ")", rising(count("size-smallest"), count("size-smaller"), count("beta"), count("size-larger"), count("size-largest")));
		check("Size: each star grows where it is (of what Beta lights, Largest lights " + kept("beta", "size-largest") + ", and of what Smallest lights, Beta "
				+ kept("size-smallest", "beta") + ")", kept("beta", "size-largest") >= 0.95 && kept("size-smallest", "beta") >= 0.95);
		check("Brightness: a star is brighter at each step up (" + typical("brightness-dimmest") + ", " + typical("brightness-dimmer") + ", " + typical("beta") + ", "
				+ typical("brightness-brighter") + ", " + typical("brightness-brightest") + ")",
				rising(typical("brightness-dimmest"), typical("brightness-dimmer"), typical("beta"), typical("brightness-brighter"), typical("brightness-brightest")));
		check("Brightness: a quarter, a half, twice and four times beta's light, Brightest white", Math.abs(typical("brightness-dimmest") - 16) <= 6
				&& Math.abs(typical("brightness-dimmer") - 32) <= 6 && Math.abs(typical("brightness-brighter") - 128) <= 8 && typical("brightness-brightest") >= 250);
		check("Brightness: the same stars at each (of what Beta lights, Dimmest lights " + kept("beta", "brightness-dimmest") + ")",
				kept("beta", "brightness-dimmest") >= 0.95 && kept("brightness-dimmest", "beta") >= 0.95);
		check("Number: more stars at each step up (" + count("number-fewest") + ", " + count("number-fewer") + ", " + count("beta") + ", " + count("number-more") + ", "
				+ count("number-most") + ")", rising(count("number-fewest"), count("number-fewer"), count("beta"), count("number-more"), count("number-most")));
		check("Number: Fewer keeps some of beta's stars and More all of them (of what Fewer lights, Beta lights " + kept("number-fewer", "beta") + "; of what Beta lights, More "
				+ kept("beta", "number-more") + ")", kept("number-fewer", "beta") >= 0.95 && kept("beta", "number-more") >= 0.95);
	}
}
