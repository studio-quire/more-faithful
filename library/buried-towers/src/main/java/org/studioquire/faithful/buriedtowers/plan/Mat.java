package org.studioquire.faithful.buriedtowers.plan;

/** A material: what a cell should be, by where it is. */
interface Mat {
	int at(int x, int y, int z);

	static Mat of(int id) {
		return (x, y, z) -> id;
	}

	static Mat of(int id, int meta) {
		int cell = Blocks.cell(id, meta);
		return (x, y, z) -> cell;
	}

	/** {@code b} in patches about {@code scale} cells across, covering about {@code share} of the surface. */
	static Mat patched(Mat a, Mat b, Noise noise, double share, double scale) {
		// Value noise bunches around a half, so the share is stretched to mean what it says.
		double edge = 0.5 + (0.5 - share) * 0.62;
		return (x, y, z) -> noise.at(x, y, z, scale) > edge ? b.at(x, y, z) : a.at(x, y, z);
	}
}
