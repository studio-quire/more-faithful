package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

/**
 * A room the sand has drifted into, over a floor that was raised three over an undercroft, and where the middle of that floor has gone the sand has caked over the hole and held. Walking on it
 * changes nothing; anything that tells a cell of it something's changed beside it, a torch put on it say, brings it down by beta's own rules, and the rest of the crust after it (no plate: the user,
 * 2026-11-08, things make sense as a place). Whoever's on it drops three with it, onto what fell, which does them no harm: a step at the edge of the hole climbs back out. In the undercroft, under
 * the solid floor at the hole's edge where nothing can fall on it, a chest that was lost when the floor went. Sand lies over the solid floor too, so nothing shows where the hole is.
 */
final class SandFloor {
	static final FloorKind SIMPLE = new Simple(), COMPLEX = new Complex();
	/** How far over the floor's lowest cell it's walked, and so how far a fall through the crust is. */
	static final int RAISED = 3;

	private SandFloor() {
	}

	static double weight(Tower t) {
		return t.is(Climate.DESERT) ? 2.5 : t.is(Climate.OPEN) ? 0.45 : t.is(Climate.COLD) ? 0.15 : 0.25;
	}

	/** Whether a column's free of the ways from the floor's lowest cell to two over where it's walked. */
	static boolean free(Floor f, int x, int z) {
		for (int yy = f.y; yy <= f.stand + 2; yy++) {
			if (f.p.held(x, yy, z)) return false;
		}
		return true;
	}

	/**
	 * The raised floor and the hole in it, a ragged patch about {@code far} across round a place near the middle: solid under the walk everywhere else, the crust of sand over the hole, sand drifted over
	 * the rest but where about {@code bareShare} of it shows {@code bare}, and drifts a cell high against the wall. A step at one edge of the hole and the chest in an alcove at another. Where the hole
	 * is, as {u, v}.
	 */
	static int[] lay(Floor f, double far, Mat solid, Mat bare, double bareShare) {
		Tower t = f.t;
		Plan p = f.p;
		int half = f.half(), crust = f.stand - 1;
		Noise n = new Noise(t.seed ^ 0x5A4DL ^ f.index);
		int[] mid = Floor.polar(f.r.nextInt(360), f.r.nextDouble() * 1.2);
		boolean[][] hole = new boolean[half * 2 + 3][half * 2 + 3];
		Mat drifted = Mat.patched(Mat.of(SAND), bare, n, bareShare, 2.5);
		for (int u = -half - 1; u <= half + 1; u++) {
			for (int v = -half - 1; v <= half + 1; v++) {
				int x = f.frame.x(u, v), z = f.frame.z(u, v);
				if (!f.in(x, z) || !free(f, x, z)) continue;
				// Never against the wall, so the crust always rests on solid floor all round.
				double at = Math.hypot(u - mid[0], v - mid[1]) + (n.at(u, 5, v, 2) - 0.5) * 1.6;
				hole[u + half + 1][v + half + 1] = at < far && Math.hypot(u, v) < f.rim() - 1.6;
			}
		}
		for (int u = -half - 1; u <= half + 1; u++) {
			for (int v = -half - 1; v <= half + 1; v++) {
				int x = f.frame.x(u, v), z = f.frame.z(u, v);
				if (!f.in(x, z) || !free(f, x, z)) continue;
				if (hole[u + half + 1][v + half + 1]) {
					for (int yy = f.y; yy < crust; yy++) p.set(x, yy, z, AIR);
					p.set(x, crust, z, cell(SAND, 0));
					continue;
				}
				for (int yy = f.y; yy < crust; yy++) p.set(x, yy, z, solid.at(x, yy, z));
				boolean edge = false;
				for (int d = 0; d < 4; d++) {
					int nu = u + DX[d], nv = v + DZ[d];
					edge |= Math.abs(nu) <= half + 1 && Math.abs(nv) <= half + 1 && hole[nu + half + 1][nv + half + 1];
				}
				p.set(x, crust, z, edge ? cell(SAND, 0) : drifted.at(x, crust, z));
			}
		}
		// The way back out, and the chest under the edge: each where the hole meets the solid floor.
		boolean stepped = false, stashed = false;
		int start = f.r.nextInt(4);
		for (int u = -half; u <= half && !(stepped && stashed); u++) {
			for (int v = -half; v <= half && !(stepped && stashed); v++) {
				int qu = start % 2 == 0 ? u : -u, qv = start / 2 == 0 ? v : -v;
				if (!hole[qu + half + 1][qv + half + 1]) continue;
				for (int d = 0; d < 4; d++) {
					int su = qu + DX[d], sv = qv + DZ[d];
					if (Math.abs(su) > half || Math.abs(sv) > half || hole[su + half + 1][sv + half + 1]) continue;
					int sx = f.frame.x(su, sv), sz = f.frame.z(su, sv), hx = f.frame.x(qu, qv), hz = f.frame.z(qu, qv);
					if (!f.in(sx, sz) || !free(f, sx, sz)) continue;
					if (!stepped) {
						// A block on the undercroft's floor at the edge: the crust over it lands on it, and it's a step up from what fell, and another out.
						p.set(hx, f.y, hz, solid.at(hx, f.y, hz));
						stepped = true;
						break;
					}
					if (!stashed && Math.hypot(su, sv) < f.rim() - 1.5) {
						// An alcove two high under the solid floor, its roof the floor's own stuff, never sand, which would fall on the chest.
						p.set(sx, f.y + 1, sz, AIR);
						p.set(sx, crust, sz, solid.at(sx, crust, sz));
						f.chest(sx, f.y, sz, f.frame.dir(d), "pit");
						stashed = true;
						break;
					}
				}
			}
		}
		// Drifts against the wall, here and there.
		for (int i = 0; i < half; i++) {
			int[] c = f.against(f.r.nextInt(360));
			int x = f.frame.x(c[0], c[1]), z = f.frame.z(c[0], c[1]);
			if (free(f, x, z) && p.id(x, f.stand, z) == AIR && p.id(x, crust, z) == SAND) p.set(x, f.stand, z, cell(SAND, 0));
		}
		return mid;
	}

	/**
	 * Once the ways are drawn: what's left empty under the edge of a way's walk, down to the hole's level and up to the walk itself (a stair's walk leaves its corners out), is made solid, so the hollow
	 * stays sealed, only the hole's own cells are under the crust, and there's no dip in the floor. Nothing's needed there: a stair's well is its middle, a ladder's shaft inside its hold.
	 */
	static void seal(Floor f) {
		Tower t = f.t;
		Plan p = f.p;
		for (int x = t.cx - t.radius; x <= t.cx + t.radius; x++) {
			for (int z = t.cz - t.radius; z <= t.cz + t.radius; z++) {
				if (!f.in(x, z) || free(f, x, z)) continue;
				boolean edge = false;
				for (int d = 0; d < 4; d++) edge |= free(f, x + DX[d], z + DZ[d]);
				if (!edge) continue;
				for (int yy = f.y; yy < f.stand; yy++) {
					if (p.id(x, yy, z) == AIR) p.force(x, yy, z, f.pal.wall.at(x, yy, z));
				}
			}
		}
	}

	/** A chest at the wall at about an angle, on the raised floor, where it's solid under it and free. */
	static void chestOnFloor(Floor f, double degrees, String loot) {
		for (int k = 0; k < 12; k++) {
			int[] c = f.against(degrees + (k % 2 == 0 ? 1 : -1) * 30 * ((k + 1) / 2));
			int x = f.frame.x(c[0], c[1]), z = f.frame.z(c[0], c[1]);
			if (f.p.id(x, f.stand, z) != AIR || f.p.id(x, f.stand + 1, z) != AIR || f.p.held(x, f.stand, z) || !solid(f.p.id(x, f.stand - 2, z))) continue;
			f.chestAt(c[0], f.stand, c[1], Floor.outward(c[0], c[1]), loot);
			return;
		}
	}

	/** A spawner on the raised floor where it's solid all the way down, nearest to a place. */
	static void spawnerOnFloor(Floor f, int u, int v) {
		for (int r = 0; r < 4; r++) {
			for (int du = -r; du <= r; du++) {
				for (int dv = -r; dv <= r; dv++) {
					if (Math.max(Math.abs(du), Math.abs(dv)) != r) continue;
					int x = f.frame.x(u + du, v + dv), z = f.frame.z(u + du, v + dv);
					if (!f.in(x, z) || !free(f, x, z) || !solid(f.p.id(x, f.stand - 2, z)) || f.p.id(x, f.stand, z) != AIR || f.p.id(x, f.stand + 1, z) != AIR) continue;
					f.spawner(x, f.stand, z);
					return;
				}
			}
		}
	}

	/** Plain: the wall's own stuff under the walk, sand over nearly all of it, a hole about five across. */
	private static final class Simple extends FloorKind {
		Simple() {
			super("sand floor", Size.MEDIUM, 7, 8, 0.15, 0.9);
		}

		@Override
		double weight(Tower t) {
			return SandFloor.weight(t);
		}

		@Override
		int standOffset(int h) {
			return RAISED;
		}

		@Override
		boolean hollowUnder() {
			return true;
		}

		@Override
		void afterWays(Floor f) {
			seal(f);
		}

		@Override
		Mat overhead(Floor f) {
			return f.pal.wall;
		}

		@Override
		void draw(Floor f) {
			int[] mid = lay(f, 2.6, f.pal.wall, f.pal.floor, 0.15);
			double away = Math.toDegrees(Math.atan2(-mid[1], -mid[0]));
			chestOnFloor(f, f.r.nextInt(360), "stores");
			int[] s = f.against(away + 90);
			spawnerOnFloor(f, s[0] / 2, s[1] / 2);
			f.cobwebCorners(8);
		}
	}

	/** A hall of flags on a ring of columns, raised over its undercroft, with sand drifted over most of it and a hole near the middle about eight across. */
	private static final class Complex extends FloorKind {
		Complex() {
			super("sand floor", Size.MEDIUM, 8, 10, 0.15, 0.9);
		}

		@Override
		double weight(Tower t) {
			return SandFloor.weight(t);
		}

		@Override
		int standOffset(int h) {
			return RAISED;
		}

		@Override
		boolean hollowUnder() {
			return true;
		}

		@Override
		void afterWays(Floor f) {
			seal(f);
		}

		@Override
		void draw(Floor f) {
			int[] mid = lay(f, 4.2, f.pal.wall, f.pal.flags(f.t), 0.35);
			int turn = f.r.nextInt(45);
			double ring = f.rim() - 2;
			for (int i = 0; i < 8; i++) {
				int[] c = Floor.polar(turn + 45 * i, ring);
				int x = f.frame.x(c[0], c[1]), z = f.frame.z(c[0], c[1]);
				if (f.p.id(x, f.stand - 2, z) == AIR) continue;
				boolean clear = true;
				for (int yy = f.stand; yy <= f.top() && clear; yy++) clear = !f.p.held(x, yy, z) && f.p.id(x, yy, z) == AIR;
				if (clear) f.box(c[0], f.stand, c[1], c[0], f.top(), c[1], f.pal.pillar);
			}
			double away = Math.toDegrees(Math.atan2(-mid[1], -mid[0]));
			chestOnFloor(f, away + 30, "stores");
			chestOnFloor(f, away - 150, "stores");
			for (int i = 0; i < 2; i++) {
				int[] s = Floor.polar(away + 90 + 180 * i, ring - 1.5);
				spawnerOnFloor(f, s[0], s[1]);
			}
			f.cobwebCorners(12);
		}
	}
}
