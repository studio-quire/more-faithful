package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Near the bottom, where the diggers broke into lava and it came up over the floor: a lake of it a cell deep, a ragged shore along the wall, a causeway across, and a crust of obsidian here and there
 * where water once ran onto it. The floor's walked a cell up, on the shore and the causeway, as the flooded hall's is. Lava lights it all, so nothing spawns: a breather. The lava
 * goes in after the ways, round them, built up a cell as a kerb wherever it would meet open air at its own level, so it's still and stays put; only stairs or a ladder come in or go out, and a ladder
 * here stands on a post of stone rather than a log, which lava would set alight. No chest: beta's lava lights fire beside anything of wood, a chest's wood
 * included, a few cells off, and everything here is that near it.
 */
final class LavaLake {
	static final FloorKind SIMPLE = new Simple(), COMPLEX = new Complex();

	private LavaLake() {
	}

	static double weight(Tower t) {
		return t.purpose == Tower.Purpose.MINE ? 1.6 : t.is(Climate.DESERT) ? 1.1 : 0.7;
	}

	/** The lava over every open cell of the floor's lowest level on solid ground with room over it; a kerb of the floor's stuff where it would meet open air the lava mustn't reach, a way's well say. */
	static void flood(Floor f, Mat kerbMat) {
		Tower t = f.t;
		Plan p = f.p;
		int y = f.y;
		Set<Long> hot = new HashSet<>();
		for (int x = t.cx - t.radius; x <= t.cx + t.radius; x++) {
			for (int z = t.cz - t.radius; z <= t.cz + t.radius; z++) {
				if (!f.in(x, z) || p.id(x, y, z) != AIR || !solid(p.id(x, y - 1, z))) continue;
				if (passable(p.id(x, y + 1, z))) hot.add(Plan.key(x, y, z));
				else p.force(x, y, z, kerbMat.at(x, y, z));
			}
		}
		Set<Long> kerb = new HashSet<>();
		for (long k : hot) {
			int x = Plan.keyX(k), z = Plan.keyZ(k);
			for (int d = 0; d < 4; d++) {
				int nx = x + DX[d], nz = z + DZ[d];
				if (!hot.contains(Plan.key(nx, y, nz)) && passable(p.id(nx, y, nz)) && p.get(nx, y, nz) != Plan.KEEP) kerb.add(k);
			}
		}
		for (long k : hot) {
			int x = Plan.keyX(k), z = Plan.keyZ(k);
			p.force(x, y, z, kerb.contains(k) ? kerbMat.at(x, y, z) : cell(LAVA, 0));
		}
	}

	/** The shore: the cells along the wall, solid at the lake's level, gone here and there; more of it gone where {@code gone} is higher. */
	static void shore(Floor f, Mat m, double gone) {
		Noise n = new Noise(f.t.seed ^ 0x1A7AL ^ f.index);
		int reach = f.half() + 1;
		for (int u = -reach; u <= reach; u++) {
			for (int v = -reach; v <= reach; v++) {
				int x = f.frame.x(u, v), z = f.frame.z(u, v);
				if (!f.in(x, z) || f.t.depth(x, z) > f.t.wall + 1) continue;
				if (n.at(u, 3, v, 3) >= gone && f.idAt(u, f.y, v) == AIR) f.put(u, f.y, v, m);
			}
		}
	}

	/** A causeway {@code wide} across the room along a quarter's line, through a place off the middle, solid at the lake's level. */
	static void causeway(Floor f, int d, int off, int wide, Mat m) {
		int side = d + 1 & 3, reach = f.half() + 1;
		for (int k = -reach; k <= reach; k++) {
			for (int w = 0; w < wide; w++) {
				int u = DX[d] * k + DX[side] * (off + w), v = DZ[d] * k + DZ[side] * (off + w);
				if (f.inFrame(u, v) && f.idAt(u, f.y, v) == AIR) f.put(u, f.y, v, m);
			}
		}
	}

	/** A few crusts of obsidian a cell or two across, out in the lake where nothing's built. */
	static void crusts(Floor f, int count) {
		for (int i = 0, tries = 0; i < count && tries < 30; tries++) {
			int[] c = Floor.polar(f.r.nextInt(360), 1.5 + f.r.nextDouble() * (f.rim() - 3));
			boolean open = true;
			for (int du = -1; du <= 1 && open; du++) {
				for (int dv = -1; dv <= 1 && open; dv++) open = f.inFrame(c[0] + du, c[1] + dv) && f.idAt(c[0] + du, f.y, c[1] + dv) == AIR && !f.p.held(f.frame.x(c[0] + du, c[1] + dv), f.y, f.frame.z(c[0] + du, c[1] + dv));
			}
			if (!open) continue;
			f.put(c[0], f.y, c[1], cell(OBSIDIAN, 0));
			if (f.r.nextBoolean()) {
				int d = f.r.nextInt(4);
				f.put(c[0] + DX[d], f.y, c[1] + DZ[d], cell(OBSIDIAN, 0));
			}
			i++;
		}
	}

	/** A plain pit of lava with a broken shore of the wall's stuff, one causeway across, a crust or two. */
	private static final class Simple extends FloorKind {
		Simple() {
			super("lava lake", Size.MEDIUM, 6, 8, 0.6, 0.98);
		}

		@Override
		double weight(Tower t) {
			return LavaLake.weight(t);
		}

		@Override
		int standOffset(int h) {
			return 1;
		}

		@Override
		boolean hotUnderfoot() {
			return true;
		}

		@Override
		Mat underfoot(Floor f) {
			return Mat.of(STONE);
		}

		@Override
		Mat overhead(Floor f) {
			return f.pal.wall;
		}

		@Override
		void draw(Floor f) {
			shore(f, f.pal.wall, 0.42);
			causeway(f, f.r.nextInt(4), f.r.nextInt(3) - 1, 1, f.pal.wall);
			crusts(f, 1 + f.r.nextInt(2));
		}

		@Override
		void afterWays(Floor f) {
			flood(f, f.pal.wall);
		}
	}

	/** A taller hall: a broken shore in the trim, causeways of flags crossing at an island in the middle, columns standing in the lava. */
	private static final class Complex extends FloorKind {
		Complex() {
			super("lava lake", Size.MEDIUM, 8, 11, 0.6, 0.98);
		}

		@Override
		double weight(Tower t) {
			return LavaLake.weight(t);
		}

		@Override
		int standOffset(int h) {
			return 1;
		}

		@Override
		boolean hotUnderfoot() {
			return true;
		}

		@Override
		Mat underfoot(Floor f) {
			return Mat.of(STONE);
		}

		@Override
		Mat overhead(Floor f) {
			return f.pal.wall;
		}

		@Override
		void draw(Floor f) {
			int turn = f.r.nextInt(45);
			double ring = f.rim() - 3.5;
			for (int i = 0; i < 6; i++) {
				int[] c = Floor.polar(turn + 60 * i, ring);
				f.pier(c[0], c[1], c[0], c[1], f.top(), f.pal.pillar);
			}
			shore(f, f.pal.trim, 0.45);
			Mat flags = f.pal.flags(f.t);
			int d = f.r.nextInt(2);
			causeway(f, d, -1, 2, flags);
			causeway(f, d + 1, -1, 2, flags);
			f.disc(-0.5, -0.5, 2.2, f.y, f.pal.trim);
			crusts(f, 2 + f.r.nextInt(3));
		}

		@Override
		void afterWays(Floor f) {
			flood(f, f.pal.trim);
		}
	}
}
