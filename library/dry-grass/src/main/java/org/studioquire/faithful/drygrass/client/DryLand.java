package org.studioquire.faithful.drygrass.client;

import java.awt.Color;
import java.util.Arrays;

import org.studioquire.faithful.drygrass.mixin.FoliageColorsAccess;
import org.studioquire.faithful.drygrass.mixin.GrassColorsAccess;

/**
 * The colours. Beta tints the grass block, tall grass and ferns from one colour map and oak leaves from another (birch and spruce have a
 * fixed colour each), 256 by 256, read at x = (1 - T) * 255 and y = (1 - T * D) * 255 for a column's temperature T and downfall D. The same
 * two numbers choose the column's biome, so the grass map's hot, dry corner, its olive, is where Savanna lies, with the dry sides of
 * Shrubland and Plains on the way to it. T and D are smooth noise over the land, so the colour fades across it with no line where one
 * biome meets another, and so does everything here: it goes by how hot and dry a place is ({@link #weight}), never by its biome.
 * <p>
 * Each colour is worked out once for every cell of a map, from the map the game has (a texture pack's, when one is chosen), into a table,
 * so a lookup is two array reads. Choosing another texture pack rewrites a map in place, which a lookup or entering a world notices.
 */
public final class DryLand {
	/**
	 * The choices. Each starts from beta's map or from Green's ({@code fromGreen}), then walks part of the way ({@code strength}, reached in
	 * the hottest, driest land) towards a colour in hue, saturation and brightness, any of which may be kept as it is. Part of the way, so the
	 * grass keeps beta's own shading across dry land; all the way, each made the dry land one flat colour.
	 */
	public enum Tint {
		BETA(false, KEEP, KEEP, KEEP, 0),
		GREEN(true, KEEP, KEEP, KEEP, 0),
		/** Green, yellower and brighter, as fully coloured. */
		SPRING(true, 82, KEEP, 0.92F, 0.5),
		/** Green, deeper and richer. */
		LUSH(true, 100, 0.60F, 0.85F, 0.5),
		/** Green, a little paler. */
		PALE(true, KEEP, 0.42F, 0.90F, 0.5),
		/** Green towards a bluer, softer green, from Green so it keeps more of its colour. */
		SAGE(true, 105, 0.42F, 0.88F, 0.5),
		/** Beta's olive towards a pale fresh green. */
		MEADOW(false, 85, 0.36F, 0.92F, 0.55),
		/**
		 * Beta's olive towards a warm straw, full and bright, as beta's grass tile is dark (it averages 57% white) and a duller straw read khaki
		 * on it; a paler straw (48 degrees, half saturation) read sandy, and its fade from beta's green passed through olive.
		 */
		GOLDEN(false, 44, 0.62F, 0.98F, 0.45);

		final boolean fromGreen;
		final float hue, saturation, brightness;
		final double strength;

		Tint(boolean fromGreen, float degrees, float saturation, float brightness, double strength) {
			this.fromGreen = fromGreen;
			this.hue = degrees / 360F;
			this.saturation = saturation;
			this.brightness = brightness;
			this.strength = strength;
		}
	}

	/** A part of a colour a tint leaves as it is. */
	private static final float KEEP = Float.NaN;

	/** What grass and leaves start as, where the file says nothing or something unknown. */
	static final Tint START = Tint.SAGE;
	/** The grass's choice and the oak leaves', each its own setting. */
	static volatile Tint grassTint = START, leafTint = START;

	/**
	 * Temperatures over which a place goes from not hot at all to fully hot: wide and smooth, as beta's olive deepens steadily with heat (on the
	 * driest land its hue runs from 116 degrees at 0.2 to 58 at 1.0), so Savanna's cool side changes about half as much as its hot side. Snow
	 * can't be counted on to cover a fade in cold land: players clear it and light melts it.
	 */
	private static final double COOL = 0.20, HOT = 0.80;
	/**
	 * Wetness (T * D, the map's y) over which a place goes from not dry at all to fully dry: Savanna is drier than 0.2, Forest wetter than 0.35,
	 * and Shrubland between. A wide, smooth ramp, as beta's own olive deepens gradually towards the driest land rather than starting at a line;
	 * with each tint going only part of the way ({@link Tint}), the fade's colours stay near beta's own.
	 */
	private static final double WET = 0.45, DRY = 0.05;
	/**
	 * Green reads both maps where the land is wetter: at {@link #GREEN_DRIEST} for the driest land, rising evenly to the place's own wetness at
	 * {@link #GREEN_WET}, so dry land takes the colours of a Forest's dry side, in their order, and nothing wetter moves. In cool land it moves
	 * less, by how hot it is. Being beta's own colours all the way, its fade needs no narrowing.
	 */
	private static final double GREEN_DRIEST = 0.32, GREEN_WET = 0.45;

	private static final int SIZE = 256 * 256;

	/** One map's colours under one tint: {@code out} for each cell, worked out from {@code source}, a copy of the map as it was. */
	private static final class Table {
		final Tint tint;
		final int[] source, out;

		Table(Tint tint, int[] source, int[] out) {
			this.tint = tint;
			this.source = source;
			this.out = out;
		}
	}

	private static volatile Table grass, foliage;

	private DryLand() {
	}

	static Tint tint(String value) {
		String v = value == null ? "" : value.trim();
		for (Tint t : Tint.values()) {
			if (t.name().equalsIgnoreCase(v)) return t;
		}
		return START;
	}

	/** The grass map's colour for a place, given beta's. */
	public static int grass(double temperature, double humidity, int beta) {
		Tint tint = grassTint;
		if (tint == Tint.BETA) return beta;
		int i = index(temperature, humidity);
		if (i < 0) return beta;
		Table table = grass;
		if (table == null || table.tint != tint || table.source[i] != beta) {
			int[] live = GrassColorsAccess.drygrass$map();
			// A colour that isn't the map's has come from another mod's change: theirs stands.
			if (live.length < SIZE || live[i] != beta) return beta;
			grass = table = build(live, tint);
		}
		return table.out[i];
	}

	/** The leaf map's colour for a place, given beta's. */
	public static int foliage(double temperature, double humidity, int beta) {
		Tint tint = leafTint;
		if (tint == Tint.BETA) return beta;
		int i = index(temperature, humidity);
		if (i < 0) return beta;
		Table table = foliage;
		if (table == null || table.tint != tint || table.source[i] != beta) {
			int[] live = FoliageColorsAccess.drygrass$map();
			if (live.length < SIZE || live[i] != beta) return beta;
			foliage = table = build(live, tint);
		}
		return table.out[i];
	}

	/** On entering a world: a texture pack chosen since (from the title screen) has rewritten a map, so its table is worked out again. */
	static void recheck() {
		Table g = grass, f = foliage;
		if (g != null && !Arrays.equals(g.source, GrassColorsAccess.drygrass$map())) grass = null;
		if (f != null && !Arrays.equals(f.source, FoliageColorsAccess.drygrass$map())) foliage = null;
	}

	/** The cell beta reads, worked out as beta works it out; -1 off the map, where beta's own lookup would have failed first. */
	private static int index(double temperature, double humidity) {
		humidity *= temperature;
		int x = (int) ((1.0 - temperature) * 255.0), y = (int) ((1.0 - humidity) * 255.0);
		return x < 0 || x > 255 || y < 0 || y > 255 ? -1 : y << 8 | x;
	}

	private static Table build(int[] live, Tint tint) {
		int[] source = live.clone(), out = source.clone();
		for (int y = 0; y < 256; y++) {
			double h = 1 - y / 255.0;
			// Wetness is downfall times temperature, so it's never more than the temperature: cells to the right of the diagonal are never read.
			for (int x = 0; x <= y; x++) {
				double t = 1 - x / 255.0;
				int i = y << 8 | x;
				int base = source[i];
				if (tint.fromGreen) base = source[Math.max(x, Math.min(255, (int) Math.round((1 - wetter(t, h)) * 255))) << 8 | x];
				out[i] = walk(base, tint, tint.strength * weight(t, h));
			}
		}
		return new Table(tint, source, out);
	}

	/** How hot and dry a place is, 0 to 1, from its temperature and wetness. */
	static double weight(double temperature, double wetness) {
		return ease((temperature - COOL) / (HOT - COOL)) * ease((WET - wetness) / (WET - DRY));
	}

	/** The wetness Green reads a place at. */
	static double wetter(double temperature, double wetness) {
		if (wetness >= GREEN_WET) return wetness;
		double lifted = GREEN_DRIEST + wetness * (GREEN_WET - GREEN_DRIEST) / GREEN_WET;
		return Math.min(temperature, wetness + (lifted - wetness) * ease((temperature - COOL) / (HOT - COOL)));
	}

	/** Part of the way from a colour towards a tint's, in hue (the short way round), saturation and brightness, each unless the tint keeps it. */
	private static int walk(int argb, Tint to, double part) {
		if (part <= 0) return argb;
		float[] c = Color.RGBtoHSB(argb >> 16 & 255, argb >> 8 & 255, argb & 255, null);
		float k = (float) Math.min(1, part), h = c[0], s = c[1], b = c[2];
		if (!Float.isNaN(to.hue)) {
			float dh = to.hue - h;
			if (dh > 0.5F) dh -= 1;
			if (dh < -0.5F) dh += 1;
			h += dh * k;
		}
		if (!Float.isNaN(to.saturation)) s += (to.saturation - s) * k;
		if (!Float.isNaN(to.brightness)) b += (to.brightness - b) * k;
		return argb & 0xFF000000 | Color.HSBtoRGB(h, s, b) & 0xFFFFFF;
	}

	/** 0 to 1 smoothly over 0 to 1, flat outside. */
	private static double ease(double f) {
		if (f <= 0) return 0;
		if (f >= 1) return 1;
		return f * f * (3 - 2 * f);
	}
}
