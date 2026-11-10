package org.studioquire.faithful.buriedtowers.plan;

import java.util.Random;
import java.util.function.IntBinaryOperator;

/** The outside of a tower and the floors between its rooms, in either style: what {@link Tower} asks of it, in this order. */
abstract class Exterior {
	/** The walls from the foot up, the floors between the rooms, and whatever the top has. */
	abstract void shell();

	/** The way in from the land, after the floors are drawn. */
	abstract void entrance();

	/** Whatever stands out from the wall, after the entrance. */
	abstract void dress();

	/** Fallen stone on the land round the foot. */
	abstract void apron();

	/** Last of all: anything outside the wall's columns left holding nothing goes. */
	abstract void dropLoose();

	/**
	 * The path's top at a cell {@code out} from the wall (1 the first outside it): paving the first three to five out, chosen per tower, with the ground's own top, grass or in the desert sand,
	 * coming through more of it the further out, until it's all ground and the path has faded into it (the user, 2026-11-07). {@code paving} is what the path is laid in. Draws only from its own
	 * random, so nothing else about the tower moves.
	 */
	static int pathTop(Tower t, Random path, int length, int out, int paving) {
		if (out <= length && path.nextDouble() >= Math.pow(out / (length + 1.0), 1.5)) return paving;
		return Blocks.cell(t.is(Climate.DESERT) ? Blocks.SAND : Blocks.GRASS, 0);
	}

	/**
	 * Where the path's walked along one lane out through the gate, by distance from the middle: on the land, but never more than a block above or below the cell before it, so it's walked in and out
	 * without a block placed. Where the land beside the gate is lower than the hall's floor that's a step down a block at a time until it meets it, rather than a causeway; where it's higher, a cut
	 * up into it the same way. {@code land} is the lowest cell above the land in a column, the path resting on what's below.
	 */
	static int[] pathLevels(Tower t, int hallY, int dir, int ox, int oz, IntBinaryOperator land) {
		int[] level = new int[t.radius + Tower.APRON + 1];
		int before = hallY;
		for (int k = 0; k < level.length; k++) {
			int x = t.cx + Blocks.DX[dir] * k + ox, z = t.cz + Blocks.DZ[dir] * k + oz;
			if (t.depth(x, z) != 0) {
				before = hallY;
				continue;
			}
			level[k] = before = Math.max(before - 1, Math.min(before + 1, land.applyAsInt(x, z)));
		}
		return level;
	}

	/** What a path's raised over the land on: rubble under its paving, earth under the ground's own top. */
	static int pathFill(Tower t, boolean paved) {
		return Blocks.cell(paved ? Blocks.COBBLE : t.is(Climate.DESERT) ? Blocks.SANDSTONE : Blocks.DIRT, 0);
	}

	/** The cells under each floor, down to the ceiling of the one below, or to the foot under the last. */
	static void floors(Tower t) {
		for (int i = 0; i < t.floors.size(); i++) {
			Floor f = t.floors.get(i), below = i + 1 == t.floors.size() ? null : t.floors.get(i + 1);
			int bottom = below == null ? t.foot() : below.top() + 1;
			for (int x = t.cx - t.radius; x <= t.cx + t.radius; x++) {
				for (int z = t.cz - t.radius; z <= t.cz + t.radius; z++) {
					if (t.depth(x, z) <= t.wall) continue;
					for (int y = bottom; y < f.y; y++) {
						boolean boards = y == f.y - 1;
						boolean under = below != null && y == bottom;
						t.plan.force(x, y, z, under ? (f.kind.hotUnderfoot() ? f.pal.wall : below.kind.overhead(below)).at(x, y, z) : boards ? f.kind.underfoot(f).at(x, y, z) : f.pal.wall.at(x, y, z));
					}
				}
			}
		}
	}
}
