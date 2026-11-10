package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

import java.util.HashSet;
import java.util.Set;

/**
 * A hall the water's got into: it stands knee deep over the floor, two deep where the boards have gone, round whatever's left standing in it. Nothing solid is ever over its surface, so whatever
 * swims can always come up (the user's rule, 2026-11-07). The water goes in after the ways are drawn, filling round them: where it would meet open air at its own level, a ways' landing or the
 * mouth of a shaft, the floor is built up a block instead, as a kerb, so it's still and stays where it is. The floor's walked a block up, on what's built up, with the water below that, and only
 * stairs or a ladder come down into it or leave it, as nothing could be dropped into water that shallow.
 */
final class FloodedHall {
	static final FloorKind SIMPLE = new Simple(), COMPLEX = new Complex();

	private FloodedHall() {
	}

	static double weight(Tower t) {
		return t.is(Climate.WET) ? 2.5 : t.is(Climate.DESERT) ? 0.2 : t.is(Climate.COLD) ? 0.6 : 0.9;
	}

	/**
	 * The water over the floor's open cells, knee deep, and a cell deeper over about {@code deep} of the middle of it, ways' own floor round them included, as wading to a stair is no harm. A cell of
	 * the floor's open level with something over it is filled; one beside open air the water mustn't reach (a shaft going down) is built up as a kerb; one not on solid ground stays as it is.
	 */
	static void flood(Floor f, double deep) {
		Tower t = f.t;
		Plan p = f.p;
		int y = f.y;
		Set<Long> wet = new HashSet<>();
		for (int x = t.cx - t.radius; x <= t.cx + t.radius; x++) {
			for (int z = t.cz - t.radius; z <= t.cz + t.radius; z++) {
				if (!f.in(x, z) || p.id(x, y, z) != AIR || !solid(p.id(x, y - 1, z))) continue;
				if (passable(p.id(x, y + 1, z))) wet.add(Plan.key(x, y, z));
				else p.force(x, y, z, f.pal.floorAt(x, y, z));
			}
		}
		Set<Long> kerb = new HashSet<>();
		for (long k : wet) {
			int x = Plan.keyX(k), z = Plan.keyZ(k);
			for (int d = 0; d < 4; d++) {
				int nx = x + DX[d], nz = z + DZ[d];
				if (!wet.contains(Plan.key(nx, y, nz)) && passable(p.id(nx, y, nz)) && p.get(nx, y, nz) != Plan.KEEP) kerb.add(k);
			}
		}
		for (long k : wet) {
			int x = Plan.keyX(k), z = Plan.keyZ(k);
			p.force(x, y, z, kerb.contains(k) ? f.pal.floorAt(x, y, z) : cell(WATER, 0));
		}
		Noise n = new Noise(t.seed ^ 0xF100DL ^ f.index);
		for (long k : wet) {
			int x = Plan.keyX(k), z = Plan.keyZ(k);
			if (kerb.contains(k) || p.held(x, y - 1, z) || n.at(x, 3, z, 3) < 1 - deep - 0.12) continue;
			boolean inner = true;
			for (int d = 0; d < 4 && inner; d++) inner = p.id(x + DX[d], y, z + DZ[d]) == WATER && (solid(p.id(x + DX[d], y - 1, z + DZ[d])) || p.id(x + DX[d], y - 1, z + DZ[d]) == WATER);
			if (inner && solid(p.id(x, y - 2, z)) && !p.held(x, y - 2, z)) p.set(x, y - 1, z, cell(WATER, 0));
		}
	}

	/** Heaps of what fell from the ceiling, standing up out of the water, the chest against the wall in it, and a spawner where it's dry. */
	private static final class Simple extends FloorKind {
		Simple() {
			super("flooded hall", Size.MEDIUM, 6, 8, 0.15, 0.9);
		}

		@Override
		double weight(Tower t) {
			return FloodedHall.weight(t);
		}

		@Override
		int standOffset(int h) {
			return 1;
		}

		@Override
		Mat overhead(Floor f) {
			return f.pal.wall;
		}

		@Override
		void draw(Floor f) {
			int turn = f.r.nextInt(360);
			for (int i = 0; i < 2; i++) {
				int[] c = Floor.polar(turn + 180 * i, 2 + f.r.nextInt(2));
				f.rubble(f.frame.x(c[0], c[1]), f.frame.z(c[0], c[1]), 2, f.rubbleMat());
			}
			SimpleFloors.onWall(f, turn + 90, "sunken");
			int[] s = Floor.polar(turn, 2);
			f.spawnerIfFree(s[0], f.stackBase(f.frame.x(s[0], s[1]), f.frame.z(s[0], s[1])), s[1], null);
			f.cobwebCorners(10);
		}

		@Override
		void afterWays(Floor f) {
			flood(f, 0.3);
		}
	}

	/** Columns in rings standing in the water, a causeway of flags across on the old floor built up, a dais at its end with the chest on it, and the water two deep down the middle where the floor fell. */
	private static final class Complex extends FloorKind {
		Complex() {
			super("flooded hall", Size.MEDIUM, 8, 11, 0.15, 0.9);
		}

		@Override
		double weight(Tower t) {
			return FloodedHall.weight(t);
		}

		@Override
		int standOffset(int h) {
			return 1;
		}

		@Override
		void draw(Floor f) {
			int half = f.half(), turn = f.r.nextInt(30);
			double ring = f.rim() - 3;
			for (int i = 0; i < 8; i++) {
				int[] c = Floor.polar(turn + 45 * i, ring);
				if (f.pier(c[0], c[1], c[0], c[1], f.top(), f.pal.pillar)) f.put(c[0], f.y, c[1], f.pal.trim);
			}
			int side = f.r.nextBoolean() ? 1 : -1;
			for (int u = -half; u <= half; u++) {
				for (int v = 0; v <= 1; v++) {
					if (f.inFrame(u, v) && f.idAt(u, f.y, v) == AIR) f.put(u, f.y, v, f.pal.flags(f.t));
				}
			}
			int[] end = f.toWall(0, 0, side > 0 ? EAST : WEST);
			for (int u = end[0] - side * 2; u != end[0] + side; u += side) {
				for (int v = -1; v <= 2; v++) {
					if (f.inFrame(u, v) && f.idAt(u, f.y, v) == AIR) f.put(u, f.y, v, f.pal.trim);
				}
			}
			f.chestAt(end[0], f.y + 1, 0, side > 0 ? EAST : WEST, "sunken");
			f.spawnerIfFree(end[0] - side * 2, f.y + 1, 2, null);
			int[] s = Floor.polar(turn + 90, ring - 2);
			f.spawnerIfFree(s[0], f.y, s[1], null);
			f.cobwebCorners(14);
		}

		@Override
		void afterWays(Floor f) {
			flood(f, 0.4);
		}
	}
}
