package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

/**
 * A room the spiders have taken: webs strung on the walls, round the columns and heaps and across the floor, thickest where a wall meets the ceiling, and only now and then one hung off another out in
 * the air (the user, 2026-11-08: the first round, thick through the air, was cluttered); spider spawners in among them, and what's left of what they caught, string and bones, in a chest by the wall.
 * Webs slow you and don't break a fall (beta's own), so it's a hazard, not a way down.
 */
final class SpiderNest {
	static final FloorKind SIMPLE = new Simple(), COMPLEX = new Complex();

	private SpiderNest() {
	}

	/** Every spawner on the nest is a spider's, as many as any other floor at the depth has (the user, 2026-11-08). */
	static final String TAKEN_BY = "Spider";

	static double weight(Tower t) {
		return t.is(Climate.WOODED, Climate.WET) ? 1.8 : t.is(Climate.COLD) ? 0.5 : 0.9;
	}

	/**
	 * Webs on whatever's there to hold them: against a wall, a column or a heap ({@code share} of such cells, all of it in a corner under the ceiling, more where two sides meet), on the floor (a third
	 * of that), and a few hung off those out into the air (a sixth of a third). Nowhere else.
	 */
	static void webs(Floor f, double share) {
		Tower t = f.t;
		Plan p = f.p;
		java.util.List<int[]> strung = new java.util.ArrayList<>();
		for (int x = t.cx - t.radius; x <= t.cx + t.radius; x++) {
			for (int z = t.cz - t.radius; z <= t.cz + t.radius; z++) {
				if (!f.in(x, z)) continue;
				for (int y = f.y; y <= f.top(); y++) {
					if (p.id(x, y, z) != AIR || p.held(x, y, z)) continue;
					int sides = 0;
					for (int d = 0; d < 4; d++) if (solid(p.id(x + DX[d], y, z + DZ[d]))) sides++;
					boolean under = solid(p.id(x, y + 1, z)), on = solid(p.id(x, y - 1, z));
					double odds = sides > 0 ? share * (under ? 1 : 0.55) * (sides > 1 ? 1.3 : 1) : on ? share * 0.33 : 0;
					if (f.r.nextDouble() < odds) {
						p.set(x, y, z, cell(WEB, 0));
						strung.add(new int[] {x, y, z});
					}
				}
			}
		}
		for (int[] w : strung) {
			for (int d = 0; d < 4; d++) {
				int x = w[0] + DX[d], z = w[2] + DZ[d];
				if (f.in(x, z) && p.id(x, w[1], z) == AIR && !p.held(x, w[1], z) && f.r.nextDouble() < share * 0.055) p.set(x, w[1], z, cell(WEB, 0));
			}
		}
	}

	private static final class Simple extends FloorKind {
		Simple() {
			super("spider nest", Size.MEDIUM, 5, 7, 0.15, 0.95);
		}

		@Override
		double weight(Tower t) {
			return SpiderNest.weight(t);
		}

		@Override
		String takenBy() {
			return TAKEN_BY;
		}

		@Override
		void draw(Floor f) {
			int turn = f.r.nextInt(360);
			SimpleFloors.onWall(f, turn, "nest");
			for (int i = 0; i < 2; i++) {
				int[] s = Floor.polar(turn + 120 + 120 * i, 2);
				f.spawnerIfFree(s[0], f.y, s[1], "Spider");
			}
			SimpleFloors.decay(f, 1, 0);
			webs(f, 0.3);
		}
	}

	/** A low ring of columns the webs are strung between, and the floor round it, the webs thick everywhere; two chests where the spiders' leavings have piled. */
	private static final class Complex extends FloorKind {
		Complex() {
			super("spider nest", Size.MEDIUM, 7, 10, 0.15, 0.95);
		}

		@Override
		double weight(Tower t) {
			return SpiderNest.weight(t);
		}

		@Override
		String takenBy() {
			return TAKEN_BY;
		}

		@Override
		void draw(Floor f) {
			int turn = f.r.nextInt(360);
			double ring = f.rim() - 4;
			for (int i = 0; i < 6; i++) {
				int[] c = Floor.polar(turn + 60 * i, ring);
				f.pier(c[0], c[1], c[0], c[1], f.top(), f.pal.pillar);
			}
			SimpleFloors.onWall(f, turn + 30, "nest");
			SimpleFloors.onWall(f, turn + 210, "nest");
			for (int i = 0; i < 3; i++) {
				int[] s = Floor.polar(turn + 120 * i + 60, i == 0 ? 1.5 : ring + 1.5);
				f.spawnerIfFree(s[0], f.y, s[1], "Spider");
			}
			for (int heaps = 1 + f.r.nextInt(2); heaps > 0; heaps--) {
				int[] c = f.against(f.r.nextInt(360));
				f.rubble(f.frame.x(c[0], c[1]), f.frame.z(c[0], c[1]), 2, f.rubbleMat());
			}
			webs(f, 0.26);
		}
	}
}
