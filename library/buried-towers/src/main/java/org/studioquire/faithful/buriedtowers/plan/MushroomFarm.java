package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

/**
 * Beds of earth across the floor with mushrooms still growing in them, brown and red, as they do in the dark (beta keeps a mushroom where the light's 12 or less, and nothing here is lit), paths
 * between, and the farm's chest by the wall.
 */
final class MushroomFarm {
	static final FloorKind SIMPLE = new Simple(), COMPLEX = new Complex();

	private MushroomFarm() {
	}

	static double weight(Tower t) {
		double w = t.purpose == Tower.Purpose.STORES ? 2 : t.purpose == Tower.Purpose.ORDER ? 1.2 : 0.7;
		return t.is(Climate.WET) ? w * 2 : t.is(Climate.DESERT) ? w * 0.4 : w;
	}

	/** A mushroom on a bed's cell, most of them. */
	static void grow(Floor f, int u, int y, int v) {
		if (f.r.nextInt(10) < 6 && f.idAt(u, y, v) == AIR) f.put(u, y, v, cell(f.r.nextInt(3) == 0 ? RED_MUSHROOM : BROWN_MUSHROOM, 0));
	}

	/** Beds two wide across the floor with a path between each: earth let into the floor. */
	private static final class Simple extends FloorKind {
		Simple() {
			super("mushroom farm", Size.MEDIUM, 5, 6, 0.1, 0.8);
		}

		@Override
		double weight(Tower t) {
			return MushroomFarm.weight(t);
		}

		@Override
		void draw(Floor f) {
			int half = f.half(), from = -half + 1 + f.r.nextInt(2);
			for (int v = from; v < half; v += 3) {
				for (int u = -half; u <= half; u++) {
					for (int dv = 0; dv <= 1; dv++) {
						if (!f.inFrame(u, v + dv) || !f.inFrame(u - 1, v + dv) && !f.inFrame(u + 1, v + dv)) continue;
						if (f.idAt(u, f.y, v + dv) != AIR) continue;
						f.put(u, f.y - 1, v + dv, cell(DIRT, 0));
						if (f.idAt(u, f.y - 1, v + dv) == DIRT) grow(f, u, f.y, v + dv);
					}
				}
			}
			SimpleFloors.onWall(f, f.r.nextInt(360), "farm");
			int[] s = f.against(f.r.nextInt(360));
			f.spawnerIfFree(s[0] - Integer.signum(s[0]), f.y, s[1] - Integer.signum(s[1]), null);
			SimpleFloors.decay(f, 0, 10);
		}
	}

	/** Raised beds edged in the trim, in rows across the room with flagged paths between, and the farm's chests along the wall. */
	private static final class Complex extends FloorKind {
		Complex() {
			super("mushroom farm", Size.MEDIUM, 6, 8, 0.1, 0.8);
		}

		@Override
		double weight(Tower t) {
			return MushroomFarm.weight(t);
		}

		@Override
		Mat underfoot(Floor f) {
			return f.pal.flags(f.t);
		}

		@Override
		void draw(Floor f) {
			int half = f.half();
			for (int v = -half + 2; v + 3 <= half - 1; v += 5) {
				// A bed four across the short way, its edge in the trim, earth in the middle two.
				for (int u = -half; u <= half; u++) {
					if (!f.inFrame(u - 1, v) || !f.inFrame(u + 1, v + 3) || !f.inFrame(u - 1, v + 3) || !f.inFrame(u + 1, v)) continue;
					boolean end = !f.inFrame(u - 2, v) || !f.inFrame(u + 2, v + 3) || !f.inFrame(u - 2, v + 3) || !f.inFrame(u + 2, v);
					for (int dv = 0; dv <= 3; dv++) {
						if (f.idAt(u, f.y, v + dv) != AIR) continue;
						boolean edge = end || dv == 0 || dv == 3;
						f.put(u, f.y, v + dv, edge ? f.pal.trim : Mat.of(DIRT));
						if (!edge && f.idAt(u, f.y, v + dv) == DIRT) grow(f, u, f.y + 1, v + dv);
					}
				}
			}
			int turn = f.r.nextInt(360);
			SimpleFloors.onWall(f, turn, "farm");
			SimpleFloors.onWall(f, turn + 150, "farm");
			for (int i = 0, made = 0; i < 12 && made < 2; i++) {
				int[] s = Floor.polar(turn + 90 + 30 * i, f.rim() - 2);
				if (f.idAt(s[0], f.y, s[1]) != AIR || f.idAt(s[0], f.y + 1, s[1]) != AIR) continue;
				f.spawnerIfFree(s[0], f.y, s[1], null);
				made++;
				i += 5;
			}
			f.cobwebCorners(14);
		}
	}
}
