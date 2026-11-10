package org.studioquire.faithful.buriedtowers.plan;

/** Smooth patches, so moss and repairs lie in stretches as they do on real walls, not salted a cell at a time. */
final class Noise {
	private final long seed;

	Noise(long seed) {
		this.seed = seed;
	}

	private double lattice(int x, int y, int z) {
		long h = seed ^ x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L;
		h = (h ^ h >>> 29) * 0xBF58476D1CE4E5B9L;
		h = (h ^ h >>> 32) * 0x94D049BB133111EBL;
		return (h >>> 12) * (1.0 / (1L << 52));
	}

	private static double fade(double t) {
		return t * t * (3 - 2 * t);
	}

	private static double lerp(double t, double a, double b) {
		return a + (b - a) * t;
	}

	/** Between 0 and 1, changing over about {@code scale} cells. */
	double at(double x, double y, double z, double scale) {
		x /= scale;
		y /= scale;
		z /= scale;
		int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y), z0 = (int) Math.floor(z);
		double fx = fade(x - x0), fy = fade(y - y0), fz = fade(z - z0);
		double a = lerp(fz, lattice(x0, y0, z0), lattice(x0, y0, z0 + 1)), b = lerp(fz, lattice(x0 + 1, y0, z0), lattice(x0 + 1, y0, z0 + 1));
		double c = lerp(fz, lattice(x0, y0 + 1, z0), lattice(x0, y0 + 1, z0 + 1)), d = lerp(fz, lattice(x0 + 1, y0 + 1, z0), lattice(x0 + 1, y0 + 1, z0 + 1));
		return lerp(fy, lerp(fx, a, b), lerp(fx, c, d));
	}
}
