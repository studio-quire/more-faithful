package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

/** What a floor is made of. Materials move from cobble and moss through dressed stone to raw stone going down, and the land above has a say near the top. Nothing from the Nether, and nothing a player would strip out for profit. */
final class Palette {
	final Mat wall, floor, ceiling, trim, pillar, beam;
	final int stairs, slab;
	final String name;

	private Palette(String name, Mat wall, Mat floor, Mat ceiling, Mat trim, Mat pillar, Mat beam, int stairs, int slab) {
		this.name = name;
		this.wall = wall;
		this.floor = floor;
		this.ceiling = ceiling;
		this.trim = trim;
		this.pillar = pillar;
		this.beam = beam;
		this.stairs = stairs;
		this.slab = slab;
	}

	private static Mat mossy(Noise n, Mat base, double share) {
		return Mat.patched(base, Mat.of(MOSSY), n, share, 5);
	}

	/** {@code depth}: 0 at the land, 1 at the bottom of the tower. Walls in cobble or stone with something else at the edges of things; brick only ever as the trim. */
	static Palette at(double depth, Climate climate, Noise n) {
		Mat beam = Mat.of(LOG, climate == Climate.COLD ? 1 : 0), planks = Mat.of(PLANKS), dressed = Mat.of(DOUBLE_SLAB, STONE_SLAB);
		if (depth < 0.32) return upper(climate, n, beam);
		if (depth < 0.6) {
			Mat wall = Mat.patched(Mat.of(COBBLE), Mat.of(STONE), n, 0.25, 5);
			Mat floor = climate == Climate.DESERT ? Mat.of(DOUBLE_SLAB, SAND_SLAB) : planks;
			return new Palette("brick", wall, floor, planks, Mat.of(BRICK), Mat.of(COBBLE), beam, WOOD_STAIRS, STONE_SLAB);
		}
		if (depth < 0.84) {
			Mat wall = Mat.patched(Mat.of(STONE), Mat.of(COBBLE), n, 0.12, 4);
			return new Palette("stone", wall, dressed, planks, dressed, Mat.of(COBBLE), beam, COBBLE_STAIRS, STONE_SLAB);
		}
		Mat raw = mossy(n, Mat.patched(Mat.of(STONE), Mat.of(COBBLE), n, 0.2, 5), 0.1);
		return new Palette("deep", raw, Mat.of(COBBLE), Mat.of(COBBLE), dressed, Mat.of(COBBLE), beam, COBBLE_STAIRS, COBBLE_SLAB);
	}

	private static Palette upper(Climate climate, Noise n, Mat beam) {
		Mat planks = Mat.of(PLANKS), dressed = Mat.of(DOUBLE_SLAB, STONE_SLAB), cobble = Mat.of(COBBLE);
		switch (climate) {
			case DESERT:
				return new Palette("sandstone", Mat.of(SANDSTONE), Mat.of(SANDSTONE), planks, Mat.of(BRICK), Mat.of(SANDSTONE), beam, COBBLE_STAIRS, SAND_SLAB);
			case COLD:
				return new Palette("frost", Mat.patched(cobble, Mat.of(STONE), n, 0.3, 6), cobble, planks, dressed, cobble, beam, COBBLE_STAIRS, COBBLE_SLAB);
			case WET:
				return new Palette("moss", mossy(n, cobble, 0.62), mossy(n, cobble, 0.5), planks, dressed, Mat.of(MOSSY), beam, COBBLE_STAIRS, COBBLE_SLAB);
			case WOODED:
				return new Palette("timbered", mossy(n, cobble, 0.3), planks, planks, dressed, cobble, beam, WOOD_STAIRS, WOOD_SLAB);
			default:
				return new Palette("rubble", mossy(n, cobble, 0.2), cobble, planks, dressed, cobble, beam, COBBLE_STAIRS, COBBLE_SLAB);
		}
	}

	/**
	 * The simple style's: one stuff to a wall and nothing set against it, as beta's dungeons are, so the trim and the pillars are the wall's own. Cobble with moss where it's damp near the top
	 * (sandstone in the desert), cobble going to stone going down, and a floor of cobble and moss throughout.
	 */
	static Palette simple(double depth, Climate climate, Noise n) {
		Mat beam = Mat.of(LOG, climate == Climate.COLD ? 1 : 0), cobble = Mat.of(COBBLE), stone = Mat.of(STONE);
		if (depth < 0.32) {
			if (climate == Climate.DESERT) {
				Mat sand = Mat.patched(Mat.of(SANDSTONE), cobble, n, 0.12, 4);
				return new Palette("simple sandstone", sand, Mat.of(SANDSTONE), sand, sand, sand, beam, COBBLE_STAIRS, SAND_SLAB);
			}
			double moss = climate == Climate.WET ? 0.5 : climate == Climate.WOODED ? 0.32 : climate == Climate.COLD ? 0.1 : 0.22;
			Mat wall = mossy(n, cobble, moss), floor = mossy(n, cobble, moss + 0.15);
			return new Palette("simple", wall, floor, wall, wall, wall, beam, COBBLE_STAIRS, COBBLE_SLAB);
		}
		Mat floor = mossy(n, cobble, 0.25);
		if (depth < 0.6) {
			Mat wall = Mat.patched(cobble, stone, n, 0.25, 5);
			return new Palette("simple cobble", wall, floor, wall, wall, cobble, beam, COBBLE_STAIRS, COBBLE_SLAB);
		}
		Mat wall = Mat.patched(stone, cobble, n, 0.3, 4);
		if (depth >= 0.84) wall = mossy(n, wall, 0.1);
		return new Palette(depth < 0.84 ? "simple stone" : "simple deep", wall, floor, wall, wall, cobble, beam, COBBLE_STAIRS, COBBLE_SLAB);
	}

	/** Paving for the middle of a room. */
	Mat flags(Tower t) {
		return t.is(Climate.DESERT) && name.equals("sandstone") ? Mat.of(DOUBLE_SLAB, SAND_SLAB) : Mat.of(DOUBLE_SLAB, STONE_SLAB);
	}

	/** What edges paving: the brick where this floor has it, or dark cobble against the pale flags. */
	Mat kerb() {
		return id(trim.at(0, 0, 0)) == BRICK ? trim : Mat.of(COBBLE);
	}

	int slabCell() {
		return cell(SLAB, slab);
	}

	/** A floor's boards or flags. */
	int floorAt(int x, int y, int z) {
		return floor.at(x, y, z);
	}
}
