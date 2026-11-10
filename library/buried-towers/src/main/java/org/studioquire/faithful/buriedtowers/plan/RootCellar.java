package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

/**
 * A low store just under the land, where the trees over the tower have sent their roots down through the ceiling: logs coming through it and hanging down, a few to the floor, others running along
 * its underside, and earth showing in the walls where the stone's given way to it. Pumpkins and a chest of stores against the wall. Only near the top, and never under the desert.
 */
final class RootCellar {
	static final FloorKind SIMPLE = new Simple(), COMPLEX = new Complex();

	private RootCellar() {
	}

	static double weight(Tower t) {
		if (t.is(Climate.DESERT)) return 0;
		double w = t.is(Climate.WOODED) ? 2.5 : t.is(Climate.WET) ? 1.6 : 0.7;
		return t.purpose == Tower.Purpose.STORES ? w * 1.6 : w;
	}

	/** The wood of the trees overhead. */
	static int wood(Floor f) {
		return f.t.is(Climate.COLD) ? 1 : f.t.is(Climate.WOODED) && f.r.nextBoolean() ? 2 : 0;
	}

	/** Earth with the room's own stone through it. */
	static Mat earth(Floor f, double share) {
		return Mat.patched(f.pal.wall, Mat.of(DIRT), new Noise(f.t.seed ^ 0xD127L), share, 3);
	}

	/** Earth in patches on the wall's inner face, where its stone has come away. */
	static void dirtWalls(Floor f, double share) {
		Tower t = f.t;
		Mat earth = earth(f, share);
		for (int x = t.cx - t.radius; x <= t.cx + t.radius; x++) {
			for (int z = t.cz - t.radius; z <= t.cz + t.radius; z++) {
				if (t.depth(x, z) != t.wall) continue;
				for (int y = f.y; y <= f.top(); y++) {
					if (!f.p.held(x, y, z) && solid(f.p.id(x, y, z)) && id(earth.at(x, y, z)) == DIRT) f.p.force(x, y, z, cell(DIRT, 0));
				}
			}
		}
	}

	/** Roots through the ceiling: a log through it and on down, now and then to the floor, and along its underside a little way. */
	static void roots(Floor f, int count, int wood) {
		int half = f.half(), top = f.top(), log = cell(LOG, wood);
		for (int made = 0, tries = 0; made < count && tries < count * 10; tries++) {
			int u = f.r.nextInt(half * 2 + 1) - half, v = f.r.nextInt(half * 2 + 1) - half;
			int x = f.frame.x(u, v), z = f.frame.z(u, v);
			if (!f.inFrame(u, v) || f.idAt(u, top, v) != AIR || f.p.held(x, top, z) || f.p.held(x, top + 1, z)) continue;
			int down = f.r.nextInt(4) == 0 ? f.h : 1 + f.r.nextInt(Math.max(1, f.h - 2));
			f.p.force(x, top + 1, z, log);
			for (int k = 0; k < down; k++) {
				int y = top - k;
				if (y < f.y || f.idAt(u, y, v) != AIR || f.p.held(x, y, z)) break;
				f.put(u, y, v, log);
			}
			int d = f.r.nextInt(4);
			for (int k = 1, run = 1 + f.r.nextInt(3); k <= run; k++) {
				int au = u + DX[d] * k, av = v + DZ[d] * k;
				if (!f.inFrame(au, av) || f.idAt(au, top, av) != AIR) break;
				f.put(au, top, av, log);
			}
			made++;
		}
	}

	/** Pumpkins along the wall here and there, and a chest of stores. */
	static void stores(Floor f, int pumpkins, int chests) {
		for (int i = 0, tries = 0; i < pumpkins && tries < pumpkins * 6; tries++) {
			int[] c = f.against(f.r.nextInt(360));
			if (f.idAt(c[0], f.y, c[1]) != AIR) continue;
			f.put(c[0], f.y, c[1], cell(PUMPKIN, f.r.nextInt(4)));
			i++;
		}
		for (int i = 0; i < chests; i++) SimpleFloors.onWall(f, f.r.nextInt(360), "stores");
	}

	private static final class Simple extends FloorKind {
		Simple() {
			super("root cellar", Size.SHORT, 4, 5, 0, 0.35);
		}

		@Override
		double weight(Tower t) {
			return RootCellar.weight(t);
		}

		@Override
		Mat overhead(Floor f) {
			return earth(f, 0.55);
		}

		@Override
		Mat underfoot(Floor f) {
			return earth(f, 0.3);
		}

		@Override
		void draw(Floor f) {
			roots(f, 5 + f.r.nextInt(3), wood(f));
			dirtWalls(f, 0.35);
			stores(f, 2 + f.r.nextInt(3), 1);
			int[] s = Floor.polar(f.r.nextInt(360), 1.5);
			f.spawnerIfFree(s[0], f.y, s[1], null);
			SimpleFloors.decay(f, 1, 8);
		}
	}

	/** Timber posts round the wall holding up a boarded ceiling the roots have come through in many places, earth where the wall's stone has fallen, and the stores along it. */
	private static final class Complex extends FloorKind {
		Complex() {
			super("root cellar", Size.SHORT, 5, 6, 0, 0.35);
		}

		@Override
		double weight(Tower t) {
			return RootCellar.weight(t);
		}

		@Override
		Mat overhead(Floor f) {
			return Mat.patched(Mat.of(PLANKS), Mat.of(DIRT), new Noise(f.t.seed ^ 0xD128L), 0.35, 3);
		}

		@Override
		Mat underfoot(Floor f) {
			return earth(f, 0.25);
		}

		@Override
		void draw(Floor f) {
			int wood = wood(f), turn = f.r.nextInt(45);
			for (int a = turn; a < 360 + turn; a += 45) {
				int[] c = f.against(a);
				f.pier(c[0], c[1], c[0], c[1], f.top(), f.pal.beam);
			}
			roots(f, 10 + f.r.nextInt(5), wood);
			dirtWalls(f, 0.3);
			stores(f, 4 + f.r.nextInt(4), 2);
			for (int i = 0; i < 2; i++) {
				int[] s = Floor.polar(f.r.nextInt(360), f.rim() - 3);
				f.spawnerIfFree(s[0], f.y, s[1], null);
			}
			f.cobwebCorners(12);
		}
	}
}
